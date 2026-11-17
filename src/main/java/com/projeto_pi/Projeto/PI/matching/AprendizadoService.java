package com.projeto_pi.Projeto.PI.matching;

import com.projeto_pi.Projeto.PI.entity.Avaliacao;
import com.projeto_pi.Projeto.PI.entity.Conexao;
import com.projeto_pi.Projeto.PI.entity.StatusConexao;
import com.projeto_pi.Projeto.PI.repository.AvaliacaoRepository;
import com.projeto_pi.Projeto.PI.repository.ConexaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aprendizado com o uso (camada 4): ajusta a ordem das recomendações para cada investidor.
 *
 * Dois sinais, ambos explicáveis:
 * - preferência do investidor: o que ele solicita conta a favor das características da startup
 *   (segmento, estágio, UF, faixa de ticket); o que ele passa conta contra. As avaliações que ele
 *   deu depois de uma conexão (RF10) reforçam o sinal. Vale até {@value #AJUSTE_MAX_PREFERENCIA} pontos;
 * - reputação da startup: média das avaliações que ela recebeu (RF10), com amortecimento para que
 *   poucas notas não distorçam. Vale até {@value #AJUSTE_MAX_REPUTACAO} pontos.
 *
 * Sem histórico, os dois ajustes são zero.
 */
@Service
public class AprendizadoService {

    public static final int AJUSTE_MAX_PREFERENCIA = 12;
    public static final int AJUSTE_MAX_REPUTACAO = 8;

    /** Média e peso da "avaliação neutra" usada para amortecer a reputação. */
    private static final double REPUTACAO_NEUTRA = 3.0;
    private static final double PESO_NEUTRO = 2.0;

    private final ConexaoRepository conexoes;
    private final AvaliacaoRepository avaliacoes;

    public AprendizadoService(ConexaoRepository conexoes, AvaliacaoRepository avaliacoes) {
        this.conexoes = conexoes;
        this.avaliacoes = avaliacoes;
    }

    public PerfilAprendido doInvestidor(Long investidorUsuarioId) {
        List<Conexao> historico = conexoes.findByInvestidor_Id(investidorUsuarioId);
        PerfilAprendido perfil = new PerfilAprendido();
        if (historico.isEmpty()) {
            return perfil;
        }
        Map<Long, Conexao> porId = new HashMap<>();
        for (Conexao conexao : historico) {
            porId.put(conexao.getId(), conexao);
            perfil.registrar(conexao, conexao.getStatus() != StatusConexao.DISPENSADA, 1);
        }
        for (Avaliacao avaliacao : avaliacoes.findDoAutorNasConexoes(investidorUsuarioId, porId.keySet())) {
            Conexao conexao = porId.get(avaliacao.getConexao().getId());
            if (conexao == null) {
                continue;
            }
            if (avaliacao.getNota() >= 4) {
                perfil.registrar(conexao, true, 1);
            } else if (avaliacao.getNota() <= 2) {
                perfil.registrar(conexao, false, 2);
            }
        }
        return perfil;
    }

    /** Ajuste de reputação (em pontos) por id de usuário da startup; ausente = sem avaliações. */
    public Map<Long, Integer> ajusteReputacao(Collection<Long> startupUsuarioIds) {
        Map<Long, Integer> ajustes = new HashMap<>();
        if (startupUsuarioIds.isEmpty()) {
            return ajustes;
        }
        for (Object[] linha : avaliacoes.reputacaoPorUsuario(startupUsuarioIds)) {
            Long usuarioId = ((Number) linha[0]).longValue();
            double media = ((Number) linha[1]).doubleValue();
            long total = ((Number) linha[2]).longValue();
            double amortecida = (PESO_NEUTRO * REPUTACAO_NEUTRA + media * total) / (PESO_NEUTRO + total);
            double centrada = (amortecida - 1) / 4 - 0.5;
            ajustes.put(usuarioId, (int) Math.round(2 * AJUSTE_MAX_REPUTACAO * centrada));
        }
        return ajustes;
    }

    /** Preferências aprendidas de um investidor. */
    public static final class PerfilAprendido {

        /** característica -> [sinais positivos, sinais negativos] */
        private final Map<String, int[]> contagens = new HashMap<>();
        private int decisoes;

        void registrar(Conexao conexao, boolean positivo, int peso) {
            registrar(conexao.getSegmentoStartup(), conexao.getEstagioStartup(), conexao.getUfStartup(),
                    conexao.getTicketMedioStartup(), positivo, peso);
        }

        void registrar(String segmento, String estagio, String uf, BigDecimal ticketMedio,
                       boolean positivo, int peso) {
            decisoes++;
            for (String caracteristica : caracteristicas(segmento, estagio, uf, ticketMedio)) {
                int[] contagem = contagens.computeIfAbsent(caracteristica, chave -> new int[2]);
                contagem[positivo ? 0 : 1] += peso;
            }
        }

        public int decisoes() {
            return decisoes;
        }

        /**
         * Chance estimada (0 a 1) de o investidor se interessar pela startup: média, entre as
         * características dela, de (positivos + 1) / (positivos + negativos + 2). Sem dados, 0,5.
         */
        public double preferencia(StartupResumo startup) {
            List<String> chaves = caracteristicas(
                    startup.segmento(), startup.estagio(), startup.uf(), startup.ticketMedio());
            if (chaves.isEmpty()) {
                return 0.5;
            }
            double soma = 0;
            for (String chave : chaves) {
                int[] contagem = contagens.getOrDefault(chave, new int[2]);
                soma += (contagem[0] + 1.0) / (contagem[0] + contagem[1] + 2.0);
            }
            return soma / chaves.size();
        }

        /** Ajuste em pontos, entre -{@value #AJUSTE_MAX_PREFERENCIA} e +{@value #AJUSTE_MAX_PREFERENCIA}. */
        public int ajuste(StartupResumo startup) {
            return (int) Math.round(2 * AJUSTE_MAX_PREFERENCIA * (preferencia(startup) - 0.5));
        }

        static List<String> caracteristicas(String segmento, String estagio, String uf, BigDecimal ticketMedio) {
            List<String> chaves = new ArrayList<>();
            adicionar(chaves, "seg:", Criterios.norm(segmento));
            adicionar(chaves, "est:", Criterios.norm(estagio));
            adicionar(chaves, "uf:", Criterios.norm(uf));
            adicionar(chaves, "tkt:", faixaDeTicket(ticketMedio));
            return chaves;
        }

        private static void adicionar(List<String> chaves, String prefixo, String valor) {
            if (valor != null && !valor.isEmpty()) {
                chaves.add(prefixo + valor);
            }
        }

        static String faixaDeTicket(BigDecimal ticketMedio) {
            if (ticketMedio == null) {
                return "";
            }
            double valor = ticketMedio.doubleValue();
            if (valor < 50_000) {
                return "ate-50k";
            }
            if (valor < 200_000) {
                return "50k-200k";
            }
            if (valor < 1_000_000) {
                return "200k-1m";
            }
            return "acima-1m";
        }
    }
}
