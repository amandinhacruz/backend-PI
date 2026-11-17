package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.dto.AdminDTOs.*;
import com.projeto_pi.Projeto.PI.entity.*;
import com.projeto_pi.Projeto.PI.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminService {

    private final UsuarioRepository usuarios;
    private final MensagemRepository mensagens;
    private final AgendamentoRepository agendamentos;
    private final AuditoriaAdminRepository auditoria;
    private final ConexaoRepository conexoesRepo;
    private final AvaliacaoRepository avaliacoes;

    public AdminService(UsuarioRepository usuarios, MensagemRepository mensagens,
                        AgendamentoRepository agendamentos, AuditoriaAdminRepository auditoria,
                        ConexaoRepository conexoesRepo, AvaliacaoRepository avaliacoes) {
        this.usuarios = usuarios;
        this.mensagens = mensagens;
        this.agendamentos = agendamentos;
        this.auditoria = auditoria;
        this.conexoesRepo = conexoesRepo;
        this.avaliacoes = avaliacoes;
    }

    @Transactional(readOnly = true)
    public List<CadastroAdmin> listarCadastros(String status) {
        StatusUsuario filtro = parseStatus(status);
        return usuarios.findAll().stream()
                .filter(u -> u.getPapel() != Papel.ADMIN)
                .filter(u -> filtro == null || u.getStatus() == filtro)
                .map(this::toCadastro)
                .sorted(Comparator.comparing(CadastroAdmin::enviadoEm,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public CadastroAdmin alterarStatus(Long id, StatusUsuario novo, String ator) {
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        if (usuario.getPapel() == Papel.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administradores não podem ser moderados");
        }
        usuario.setStatus(novo);
        usuarios.save(usuario);
        CadastroAdmin cadastro = toCadastro(usuario);
        String acao = switch (novo) {
            case APROVADO -> "Cadastro aprovado";
            case REJEITADO -> "Cadastro rejeitado";
            case SUSPENSO -> "Usuário suspenso";
            case PENDENTE -> "Cadastro reaberto";
        };
        auditoria.save(new AuditoriaAdmin(acao, cadastro.nome(), ator));
        return cadastro;
    }

    /**
     * Indicadores do painel. Não há registro persistido de "matches" no backend
     * (o matchmaking é calculado sob demanda), por isso o painel usa conexões
     * reais: pares de usuários com conversa ou reunião.
     */
    @Transactional(readOnly = true)
    public Metricas metricas() {
        List<Usuario> todos = usuarios.findAll();
        LocalDateTime inicioMes = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();

        long startups = todos.stream().filter(u -> u.getPapel() == Papel.STARTUP).count();
        long investidores = todos.stream().filter(u -> u.getPapel() == Papel.INVESTIDOR).count();
        long startupsNoMes = todos.stream()
                .filter(u -> u.getPapel() == Papel.STARTUP && criadoDesde(u, inicioMes)).count();
        long investidoresNoMes = todos.stream()
                .filter(u -> u.getPapel() == Papel.INVESTIDOR && criadoDesde(u, inicioMes)).count();
        long pendentes = todos.stream()
                .filter(u -> u.getPapel() != Papel.ADMIN && u.getStatus() == StatusUsuario.PENDENTE).count();

        List<Mensagem> listaMensagens = mensagens.findAll();
        List<Agendamento> listaReunioes = agendamentos.findAll();

        Set<String> paresConversa = new HashSet<>();
        Set<Long> usuariosComConversa = new HashSet<>();
        for (Mensagem m : listaMensagens) {
            paresConversa.add(par(m.getRemetente(), m.getDestinatario()));
            usuariosComConversa.add(m.getRemetente().getId());
            usuariosComConversa.add(m.getDestinatario().getId());
        }
        Set<String> paresReuniao = new HashSet<>();
        for (Agendamento a : listaReunioes) {
            paresReuniao.add(par(a.getSolicitante(), a.getDestinatario()));
        }

        Set<String> conexoes = new HashSet<>(paresConversa);
        conexoes.addAll(paresReuniao);
        long aceitas = listaReunioes.stream().filter(a -> a.getStatus() == StatusAgendamento.ACEITO).count();

        List<Usuario> startupsAprovadas = todos.stream()
                .filter(u -> u.getPapel() == Papel.STARTUP && u.getStatus() == StatusUsuario.APROVADO)
                .toList();
        long startupsComConversa = startupsAprovadas.stream()
                .filter(u -> usuariosComConversa.contains(u.getId())).count();
        long conexoesComReuniao = conexoes.stream().filter(paresReuniao::contains).count();

        // RF09: "match" é a conexão firmada pela startup depois do pedido do investidor.
        long matches = conexoesRepo.countByStatus(StatusConexao.FIRMADA);
        long pendentesDeResposta = conexoesRepo.countByStatus(StatusConexao.SOLICITADA);
        long negadas = conexoesRepo.countByStatus(StatusConexao.NEGADA);
        long passadas = conexoesRepo.countByStatus(StatusConexao.DISPENSADA);
        long pedidos = matches + pendentesDeResposta + negadas;
        long matchesComIa = conexoesRepo.countByStatusAndOrigemAnalise(StatusConexao.FIRMADA, "IA");

        List<Engajamento> engajamento = List.of(
                new Engajamento("Recomendações que viraram pedido de conexão", pct(pedidos, pedidos + passadas)),
                new Engajamento("Pedidos de conexão firmados pelas startups", pct(matches, matches + negadas)),
                new Engajamento("Startups aprovadas com conversa", pct(startupsComConversa, startupsAprovadas.size())),
                new Engajamento("Conexões com reunião agendada", pct(conexoesComReuniao, conexoes.size())),
                new Engajamento("Reuniões aceitas", pct(aceitas, listaReunioes.size())));

        return new Metricas(startups, startupsNoMes, investidores, investidoresNoMes, pendentes,
                conexoes.size(), paresConversa.size(), listaMensagens.size(),
                listaReunioes.size(), aceitas, engajamento,
                matches, pedidos, pendentesDeResposta, passadas, matchesComIa,
                avaliacoes.count(), avaliacoes.notaMedia());
    }

    @Transactional(readOnly = true)
    public List<Auditoria> auditoria() {
        return auditoria.findTop200ByOrderByDataDesc().stream()
                .map(a -> new Auditoria(a.getId(), a.getAcao(), a.getAlvo(), a.getAtor(), a.getData()))
                .toList();
    }

    private CadastroAdmin toCadastro(Usuario u) {
        String tipo;
        String nome = null;
        String responsavel = null;
        if (u.getStartup() != null) {
            tipo = "Startup";
            nome = u.getStartup().getNome();
            responsavel = u.getStartup().getProfileOwner();
        } else if (u.getInvestidorMentor() != null) {
            InvestidorMentor p = u.getInvestidorMentor();
            tipo = p.getTipo() == TipoInvestidorMentor.MENTOR ? "Mentor" : "Investidor";
            nome = p.getName();
            responsavel = p.getName();
        } else {
            tipo = u.getPapel() == Papel.STARTUP ? "Startup" : "Investidor";
        }
        if (nome == null || nome.isBlank()) nome = u.getEmail();
        if (responsavel == null || responsavel.isBlank()) responsavel = u.getEmail();
        LocalDateTime enviadoEm = u.getCriadoEm() != null ? u.getCriadoEm() : u.getDataConsentimento();
        return new CadastroAdmin(u.getId(), tipo, nome, responsavel, u.getEmail(), u.getStatus().name(), enviadoEm);
    }

    private static boolean criadoDesde(Usuario u, LocalDateTime inicio) {
        LocalDateTime data = u.getCriadoEm() != null ? u.getCriadoEm() : u.getDataConsentimento();
        return data != null && !data.isBefore(inicio);
    }

    private static String par(Usuario a, Usuario b) {
        long x = a.getId();
        long y = b.getId();
        return Math.min(x, y) + "-" + Math.max(x, y);
    }

    private static int pct(long parte, long total) {
        return total == 0 ? 0 : (int) Math.round(parte * 100.0 / total);
    }

    private static StatusUsuario parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return StatusUsuario.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido");
        }
    }
}
