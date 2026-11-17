package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.ConnectionItem;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.Rating;
import com.projeto_pi.Projeto.PI.entity.Avaliacao;
import com.projeto_pi.Projeto.PI.entity.Conexao;
import com.projeto_pi.Projeto.PI.entity.InvestidorMentor;
import com.projeto_pi.Projeto.PI.entity.Papel;
import com.projeto_pi.Projeto.PI.entity.StatusConexao;
import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.matching.Criterios;
import com.projeto_pi.Projeto.PI.matching.Formatos;
import com.projeto_pi.Projeto.PI.matching.StartupResumo;
import com.projeto_pi.Projeto.PI.repository.AvaliacaoRepository;
import com.projeto_pi.Projeto.PI.repository.ConexaoRepository;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Conexões firmadas (matches) e avaliação pós-interação (RF10). */
@Service
public class ConexaoService {

    private static final int COMENTARIO_MAXIMO = 1000;

    private final UsuarioRepository usuarios;
    private final ConexaoRepository conexoes;
    private final AvaliacaoRepository avaliacoes;

    public ConexaoService(UsuarioRepository usuarios, ConexaoRepository conexoes, AvaliacaoRepository avaliacoes) {
        this.usuarios = usuarios;
        this.conexoes = conexoes;
        this.avaliacoes = avaliacoes;
    }

    @Transactional(readOnly = true)
    public List<ConnectionItem> listar(String email) {
        Usuario usuario = participante(email);
        boolean investidor = usuario.getPapel() == Papel.INVESTIDOR;
        List<Conexao> firmadas = investidor
                ? conexoes.findByInvestidor_IdAndStatusOrderByRespondidaEmDesc(usuario.getId(), StatusConexao.FIRMADA)
                : conexoes.findByStartup_IdAndStatusOrderByRespondidaEmDesc(usuario.getId(), StatusConexao.FIRMADA);
        if (firmadas.isEmpty()) {
            return List.of();
        }

        Map<Long, Rating> minhas = new HashMap<>();
        for (Avaliacao avaliacao : avaliacoes.findDoAutorNasConexoes(
                usuario.getId(), firmadas.stream().map(Conexao::getId).toList())) {
            minhas.put(avaliacao.getConexao().getId(), new Rating(avaliacao.getNota(), avaliacao.getComentario()));
        }

        List<ConnectionItem> itens = new ArrayList<>();
        if (investidor) {
            Map<Long, StartupResumo> startups = new HashMap<>();
            usuarios.findStartupsAprovadasPorUsuario(firmadas.stream().map(c -> c.getStartup().getId()).toList())
                    .forEach(resumo -> startups.put(resumo.usuarioId(), resumo));
            for (Conexao conexao : firmadas) {
                StartupResumo s = startups.get(conexao.getStartup().getId());
                if (s == null) {
                    continue; // startup suspensa ou reprovada depois da conexão
                }
                itens.add(new ConnectionItem(
                        conexao.getId().toString(), s.id().toString(), s.usuarioId(), Formatos.texto(s.nome()),
                        "Startup cadastrada", Formatos.texto(s.cidade()), Formatos.texto(s.uf()),
                        Criterios.lista(s.segmento()), s.ticketMin(), s.ticketMax(),
                        data(conexao), minhas.get(conexao.getId())));
            }
        } else {
            for (Conexao conexao : firmadas) {
                Usuario outro = conexao.getInvestidor();
                InvestidorMentor perfil = outro.getInvestidorMentor();
                if (outro.getStatus() != StatusUsuario.APROVADO || perfil == null) {
                    continue;
                }
                itens.add(new ConnectionItem(
                        conexao.getId().toString(), perfil.getId().toString(), outro.getId(),
                        MatchmakingService.nomeDe(perfil, outro), MatchmakingService.papelDe(perfil),
                        Formatos.texto(perfil.getCity()), Formatos.texto(perfil.getState()),
                        Criterios.lista(perfil.getAreasInteresse()), perfil.getTicketMin(), perfil.getTicketMax(),
                        data(conexao), minhas.get(conexao.getId())));
            }
        }
        return itens;
    }

    /** RF10: cria ou atualiza a avaliação do usuário sobre a outra parte de uma conexão firmada. */
    @Transactional
    public Rating avaliar(String email, Long conexaoId, Integer nota, String comentario) {
        Usuario usuario = participante(email);
        if (nota == null || nota < 1 || nota > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nota deve ser de 1 a 5");
        }
        String texto = comentario == null || comentario.isBlank() ? null : comentario.trim();
        if (texto != null && texto.length() > COMENTARIO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O comentário deve ter no máximo " + COMENTARIO_MAXIMO + " caracteres");
        }
        Conexao conexao = conexoes.findById(conexaoId)
                .filter(c -> c.getInvestidor().getId().equals(usuario.getId())
                        || c.getStartup().getId().equals(usuario.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conexão não encontrada"));
        if (conexao.getStatus() != StatusConexao.FIRMADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Só é possível avaliar conexões firmadas");
        }
        Usuario avaliado = conexao.getInvestidor().getId().equals(usuario.getId())
                ? conexao.getStartup()
                : conexao.getInvestidor();
        Avaliacao avaliacao = avaliacoes.findDoAutor(conexaoId, usuario.getId())
                .orElseGet(() -> new Avaliacao(conexao, usuario, avaliado));
        avaliacao.registrar(nota, texto);
        avaliacoes.save(avaliacao);
        return new Rating(nota, texto);
    }

    private static String data(Conexao conexao) {
        var quando = conexao.getRespondidaEm() != null ? conexao.getRespondidaEm() : conexao.getCriadaEm();
        return quando.toLocalDate().toString();
    }

    private Usuario participante(String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email == null ? "" : email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não permitido"));
        if (usuario.getStatus() != StatusUsuario.APROVADO
                || (usuario.getPapel() != Papel.INVESTIDOR && usuario.getPapel() != Papel.STARTUP)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não permitido");
        }
        return usuario;
    }
}
