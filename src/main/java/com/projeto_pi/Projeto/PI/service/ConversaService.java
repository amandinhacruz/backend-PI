package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.dto.ConversaDTOs.ChatMessage;
import com.projeto_pi.Projeto.PI.dto.ConversaDTOs.Conversation;
import com.projeto_pi.Projeto.PI.dto.ConversaDTOs.InviteRequest;
import com.projeto_pi.Projeto.PI.dto.ConversaDTOs.LastMessage;
import com.projeto_pi.Projeto.PI.dto.ConversaDTOs.Meeting;
import com.projeto_pi.Projeto.PI.entity.Agendamento;
import com.projeto_pi.Projeto.PI.entity.Conexao;
import com.projeto_pi.Projeto.PI.entity.InvestidorMentor;
import com.projeto_pi.Projeto.PI.entity.Mensagem;
import com.projeto_pi.Projeto.PI.entity.Papel;
import com.projeto_pi.Projeto.PI.entity.StatusAgendamento;
import com.projeto_pi.Projeto.PI.entity.StatusConexao;
import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.entity.TipoMensagem;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.matching.Formatos;
import com.projeto_pi.Projeto.PI.repository.AgendamentoRepository;
import com.projeto_pi.Projeto.PI.repository.ConexaoRepository;
import com.projeto_pi.Projeto.PI.repository.MensagemRepository;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mensagens (RF05) e reuniões entre investidor e startup.
 *
 * Só existe conversa entre os dois lados de uma conexão firmada, e quem envia é sempre o usuário
 * do token: a versão anterior recebia o id do remetente no corpo e permitia falar em nome de outro.
 * Só o investidor convida para reunião; a startup aceita ou recusa; qualquer um dos dois cancela.
 */
@Service
public class ConversaService {

    static final int TEXTO_MAXIMO = 1000;
    static final int ASSUNTO_MAXIMO = 200;
    static final int LOCAL_MAXIMO = 300;
    static final int DURACAO_MINIMA = 15;
    static final int DURACAO_MAXIMA = 240;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final UsuarioRepository usuarios;
    private final ConexaoRepository conexoes;
    private final MensagemRepository mensagens;
    private final AgendamentoRepository agendamentos;
    private final com.projeto_pi.Projeto.PI.repository.FotoPerfilRepository fotos;

    public ConversaService(UsuarioRepository usuarios, ConexaoRepository conexoes,
                           MensagemRepository mensagens, AgendamentoRepository agendamentos,
                           com.projeto_pi.Projeto.PI.repository.FotoPerfilRepository fotos) {
        this.fotos = fotos;
        this.usuarios = usuarios;
        this.conexoes = conexoes;
        this.mensagens = mensagens;
        this.agendamentos = agendamentos;
    }

    /* ======================================================================== conversas */

    @Transactional(readOnly = true)
    public List<Conversation> conversas(String email) {
        Usuario usuario = participante(email);
        List<Conexao> firmadas = usuario.getPapel() == Papel.INVESTIDOR
                ? conexoes.findByInvestidor_IdAndStatusOrderByRespondidaEmDesc(usuario.getId(), StatusConexao.FIRMADA)
                : conexoes.findByStartup_IdAndStatusOrderByRespondidaEmDesc(usuario.getId(), StatusConexao.FIRMADA);
        firmadas = firmadas.stream().filter(c -> outro(c, usuario).getStatus() == StatusUsuario.APROVADO).toList();
        if (firmadas.isEmpty()) {
            return List.of();
        }
        List<Long> ids = firmadas.stream().map(Conexao::getId).toList();
        Map<Long, Mensagem> ultimas = new HashMap<>();
        for (Object[] linha : mensagens.findUltimas(ids)) {
            ultimas.put((Long) linha[0], (Mensagem) linha[1]);
        }
        Map<Long, Long> naoLidas = new HashMap<>();
        for (Object[] linha : mensagens.contarNaoLidas(ids, usuario.getId())) {
            naoLidas.put((Long) linha[0], ((Number) linha[1]).longValue());
        }

        Set<Long> comFoto = fotos.comFoto(firmadas.stream().map(c -> outro(c, usuario).getId()).toList());
        List<Conversation> lista = new ArrayList<>();
        for (Conexao conexao : firmadas) {
            Usuario contato = outro(conexao, usuario);
            Mensagem ultima = ultimas.get(conexao.getId());
            LastMessage resumo = ultima == null ? null : new LastMessage(
                    Formatos.cortar(textoDaMensagem(ultima), 120), ultima.getTipo().name(), ultima.getDataEnvio(),
                    ultima.getRemetente().getId().equals(usuario.getId()));
            lista.add(new Conversation(conexao.getId().toString(), contato.getId(), nome(contato), papel(contato),
                    resumo, naoLidas.getOrDefault(conexao.getId(), 0L), comFoto.contains(contato.getId())));
        }
        // Conversas com mensagem mais recente primeiro; sem mensagens, pela data da conexão.
        Map<String, LocalDateTime> ordem = new HashMap<>();
        for (Conexao conexao : firmadas) {
            Mensagem ultima = ultimas.get(conexao.getId());
            ordem.put(conexao.getId().toString(), ultima != null ? ultima.getDataEnvio()
                    : conexao.getRespondidaEm() != null ? conexao.getRespondidaEm() : conexao.getCriadaEm());
        }
        lista.sort(Comparator.comparing((Conversation c) -> ordem.get(c.id())).reversed());
        return lista;
    }

    /** Histórico completo da conversa; as mensagens recebidas passam a contar como lidas. */
    @Transactional
    public List<ChatMessage> historico(String email, Long conexaoId) {
        Usuario usuario = participante(email);
        Conexao conexao = conexaoDoUsuario(usuario, conexaoId);
        mensagens.marcarComoLidas(conexao.getId(), usuario.getId(), LocalDateTime.now());
        return mensagens.findByConexao_IdOrderByDataEnvioAscIdAsc(conexao.getId()).stream()
                .map(mensagem -> mensagem(mensagem, usuario))
                .toList();
    }

    @Transactional
    public ChatMessage enviar(String email, Long conexaoId, String texto) {
        Usuario usuario = participante(email);
        Conexao conexao = conexaoDoUsuario(usuario, conexaoId);
        String limpo = texto == null ? "" : texto.strip();
        if (limpo.isEmpty()) {
            throw erro(HttpStatus.BAD_REQUEST, "A mensagem não pode estar vazia");
        }
        if (limpo.length() > TEXTO_MAXIMO) {
            throw erro(HttpStatus.BAD_REQUEST, "A mensagem deve ter no máximo " + TEXTO_MAXIMO + " caracteres");
        }
        Mensagem salva = registrar(conexao, usuario, TipoMensagem.TEXTO, limpo, null);
        return mensagem(salva, usuario);
    }

    /* ========================================================================= reuniões */

    /** Só o investidor da conexão convida; o convite aparece no chat e na aba Reuniões dos dois. */
    @Transactional
    public ChatMessage convidar(String email, Long conexaoId, InviteRequest pedido) {
        Usuario usuario = participante(email);
        Conexao conexao = conexaoDoUsuario(usuario, conexaoId);
        if (!conexao.getInvestidor().getId().equals(usuario.getId())) {
            throw erro(HttpStatus.FORBIDDEN, "Só o investidor pode enviar convite de reunião");
        }
        if (pedido == null || pedido.date() == null || pedido.time() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe a data e o horário da reunião");
        }
        LocalDateTime inicio = LocalDateTime.of(pedido.date(), pedido.time());
        if (!inicio.isAfter(LocalDateTime.now())) {
            throw erro(HttpStatus.BAD_REQUEST, "Escolha uma data e um horário futuros");
        }
        if (pedido.date().isAfter(LocalDate.now().plusMonths(6))) {
            throw erro(HttpStatus.BAD_REQUEST, "Escolha uma data nos próximos 6 meses");
        }
        int duracao = pedido.durationMinutes() == null ? 60 : pedido.durationMinutes();
        if (duracao < DURACAO_MINIMA || duracao > DURACAO_MAXIMA) {
            throw erro(HttpStatus.BAD_REQUEST,
                    "A duração deve ficar entre " + DURACAO_MINIMA + " e " + DURACAO_MAXIMA + " minutos");
        }
        String assunto = pedido.subject() == null ? "" : pedido.subject().strip();
        if (assunto.isEmpty() || assunto.length() > ASSUNTO_MAXIMO) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe o assunto da reunião (até " + ASSUNTO_MAXIMO + " caracteres)");
        }
        String local = pedido.location() == null || pedido.location().isBlank() ? null : pedido.location().strip();
        if (local != null && local.length() > LOCAL_MAXIMO) {
            throw erro(HttpStatus.BAD_REQUEST, "O local ou link deve ter no máximo " + LOCAL_MAXIMO + " caracteres");
        }
        boolean conviteAberto = agendamentos.findDoUsuario(usuario.getId()).stream()
                .anyMatch(a -> a.getConexao() != null && a.getConexao().getId().equals(conexao.getId())
                        && a.getStatus() == StatusAgendamento.PENDENTE && !expirado(a));
        if (conviteAberto) {
            throw erro(HttpStatus.CONFLICT, "Já existe um convite aguardando resposta nesta conversa");
        }
        verificarConflito(conexao, pedido.date(), pedido.time(), duracao, null);

        Agendamento agendamento = new Agendamento();
        agendamento.setSolicitante(conexao.getInvestidor());
        agendamento.setDestinatario(conexao.getStartup());
        agendamento.setConexao(conexao);
        agendamento.setData(pedido.date());
        agendamento.setHorario(pedido.time());
        agendamento.setDuracaoMinutos(duracao);
        agendamento.setAssunto(assunto);
        agendamento.setLocal(local);
        agendamento.setStatus(StatusAgendamento.PENDENTE);
        agendamento.setCriadoEm(LocalDateTime.now());
        agendamentos.save(agendamento);

        Mensagem convite = registrar(conexao, usuario, TipoMensagem.CONVITE, assunto, agendamento);
        return mensagem(convite, usuario);
    }

    /** A startup aceita ou recusa um convite pendente. */
    @Transactional
    public Meeting responder(String email, Long agendamentoId, boolean aceitar) {
        Usuario usuario = participante(email);
        Agendamento agendamento = agendamentoDoUsuario(usuario, agendamentoId);
        if (!agendamento.getDestinatario().getId().equals(usuario.getId())) {
            throw erro(HttpStatus.FORBIDDEN, "Só quem recebeu o convite pode responder");
        }
        if (agendamento.getStatus() != StatusAgendamento.PENDENTE) {
            throw erro(HttpStatus.CONFLICT, "Este convite já foi respondido ou cancelado");
        }
        if (expirado(agendamento)) {
            throw erro(HttpStatus.CONFLICT, "O horário deste convite já passou");
        }
        if (aceitar) {
            verificarConflito(agendamento.getConexao(), agendamento.getData(), agendamento.getHorario(),
                    agendamento.getDuracaoMinutos(), agendamento.getId());
        }
        agendamento.setStatus(aceitar ? StatusAgendamento.ACEITO : StatusAgendamento.REJEITADO);
        agendamento.setRespondidoEm(LocalDateTime.now());
        agendamentos.save(agendamento);
        if (agendamento.getConexao() != null) {
            registrar(agendamento.getConexao(), usuario, TipoMensagem.SISTEMA,
                    (aceitar ? "Reunião confirmada para " : "Convite recusado: reunião de ") + quando(agendamento) + ".",
                    agendamento);
        }
        return reuniao(agendamento, usuario);
    }

    /** Qualquer participante cancela uma reunião pendente ou confirmada que ainda não aconteceu. */
    @Transactional
    public Meeting cancelar(String email, Long agendamentoId) {
        Usuario usuario = participante(email);
        Agendamento agendamento = agendamentoDoUsuario(usuario, agendamentoId);
        if (agendamento.getStatus() != StatusAgendamento.PENDENTE && agendamento.getStatus() != StatusAgendamento.ACEITO) {
            throw erro(HttpStatus.CONFLICT, "Esta reunião não pode mais ser cancelada");
        }
        if (!LocalDateTime.of(agendamento.getData(), agendamento.getHorario()).isAfter(LocalDateTime.now())) {
            throw erro(HttpStatus.CONFLICT, "Esta reunião já aconteceu");
        }
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento.setRespondidoEm(LocalDateTime.now());
        agendamentos.save(agendamento);
        if (agendamento.getConexao() != null) {
            registrar(agendamento.getConexao(), usuario, TipoMensagem.SISTEMA,
                    "Reunião de " + quando(agendamento) + " cancelada por " + nome(usuario) + ".", agendamento);
        }
        return reuniao(agendamento, usuario);
    }

    /** Agenda do usuário: todas as reuniões em que ele participa, por data. */
    @Transactional(readOnly = true)
    public List<Meeting> reunioes(String email) {
        Usuario usuario = participante(email);
        return agendamentos.findDoUsuario(usuario.getId()).stream()
                .filter(a -> outro(a, usuario).getStatus() == StatusUsuario.APROVADO)
                .map(a -> reuniao(a, usuario))
                .toList();
    }

    /* ============================================================================ apoio */

    private void verificarConflito(Conexao conexao, LocalDate data, LocalTime horario, int duracao, Long ignorar) {
        LocalTime fim = horario.plusMinutes(duracao);
        Long investidorId = conexao.getInvestidor().getId();
        for (Agendamento outro : agendamentos.findNoDia(data, Set.of(StatusAgendamento.ACEITO),
                List.of(investidorId, conexao.getStartup().getId()))) {
            if (outro.getId().equals(ignorar)) {
                continue;
            }
            LocalTime outroFim = outro.getHorario().plusMinutes(outro.getDuracaoMinutos());
            boolean sobrepoe = horario.isBefore(outroFim) && outro.getHorario().isBefore(fim);
            if (sobrepoe) {
                boolean doInvestidor = outro.getSolicitante().getId().equals(investidorId)
                        || outro.getDestinatario().getId().equals(investidorId);
                throw erro(HttpStatus.CONFLICT, (doInvestidor ? "O investidor" : "A startup")
                        + " já tem uma reunião confirmada das " + outro.getHorario().format(HORA)
                        + " às " + outroFim.format(HORA) + " nesse dia");
            }
        }
    }

    private Mensagem registrar(Conexao conexao, Usuario remetente, TipoMensagem tipo, String texto,
                               Agendamento agendamento) {
        Mensagem mensagem = new Mensagem();
        mensagem.setConexao(conexao);
        mensagem.setRemetente(remetente);
        mensagem.setDestinatario(outro(conexao, remetente));
        mensagem.setTipo(tipo);
        mensagem.setConteudo(texto);
        mensagem.setAgendamento(agendamento);
        mensagem.setDataEnvio(LocalDateTime.now());
        return mensagens.save(mensagem);
    }

    private ChatMessage mensagem(Mensagem mensagem, Usuario usuario) {
        boolean minha = mensagem.getRemetente().getId().equals(usuario.getId());
        String remetente = mensagem.getTipo() == TipoMensagem.SISTEMA ? "system" : minha ? "me" : "contact";
        Meeting reuniao = mensagem.getAgendamento() == null ? null : reuniao(mensagem.getAgendamento(), usuario);
        return new ChatMessage(mensagem.getId().toString(), remetente, mensagem.getTipo().name(),
                mensagem.getConteudo(), mensagem.getDataEnvio(), mensagem.getLidaEm() != null, reuniao);
    }

    private Meeting reuniao(Agendamento a, Usuario usuario) {
        Usuario contato = outro(a, usuario);
        boolean organizador = a.getSolicitante().getId().equals(usuario.getId());
        boolean expirado = expirado(a);
        String status = a.getStatus() == StatusAgendamento.PENDENTE && expirado ? "EXPIRADO" : a.getStatus().name();
        boolean futuro = LocalDateTime.of(a.getData(), a.getHorario()).isAfter(LocalDateTime.now());
        boolean aberto = a.getStatus() == StatusAgendamento.PENDENTE || a.getStatus() == StatusAgendamento.ACEITO;
        return new Meeting(a.getId().toString(), a.getConexao() == null ? null : a.getConexao().getId().toString(),
                nome(contato), papel(contato), a.getData(), a.getHorario(), a.getDuracaoMinutos(), a.getAssunto(),
                a.getLocal(), status, organizador,
                !organizador && a.getStatus() == StatusAgendamento.PENDENTE && !expirado,
                aberto && futuro);
    }

    private static boolean expirado(Agendamento a) {
        return a.getStatus() == StatusAgendamento.PENDENTE
                && !LocalDateTime.of(a.getData(), a.getHorario()).isAfter(LocalDateTime.now());
    }

    private static String quando(Agendamento a) {
        return a.getData().format(DATA) + " às " + a.getHorario().format(HORA);
    }

    private static String textoDaMensagem(Mensagem mensagem) {
        if (mensagem.getTipo() == TipoMensagem.CONVITE) {
            return "Convite de reunião: " + mensagem.getConteudo();
        }
        return mensagem.getConteudo();
    }

    static Usuario outro(Conexao conexao, Usuario usuario) {
        return conexao.getInvestidor().getId().equals(usuario.getId()) ? conexao.getStartup() : conexao.getInvestidor();
    }

    private static Usuario outro(Agendamento agendamento, Usuario usuario) {
        return agendamento.getSolicitante().getId().equals(usuario.getId())
                ? agendamento.getDestinatario() : agendamento.getSolicitante();
    }

    static String nome(Usuario usuario) {
        if (usuario.getStartup() != null) {
            String nome = Formatos.texto(usuario.getStartup().getNome());
            return nome.isEmpty() ? usuario.getEmail() : nome;
        }
        InvestidorMentor perfil = usuario.getInvestidorMentor();
        return perfil == null ? usuario.getEmail() : MatchmakingService.nomeDe(perfil, usuario);
    }

    static String papel(Usuario usuario) {
        if (usuario.getStartup() != null) {
            return "Startup cadastrada";
        }
        InvestidorMentor perfil = usuario.getInvestidorMentor();
        return perfil == null ? "Investidor(a)" : MatchmakingService.papelDe(perfil);
    }

    private Conexao conexaoDoUsuario(Usuario usuario, Long conexaoId) {
        return conexoes.findById(conexaoId)
                .filter(c -> c.getInvestidor().getId().equals(usuario.getId())
                        || c.getStartup().getId().equals(usuario.getId()))
                .filter(c -> c.getStatus() == StatusConexao.FIRMADA)
                .filter(c -> outro(c, usuario).getStatus() == StatusUsuario.APROVADO)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Conversa não encontrada"));
    }

    private Agendamento agendamentoDoUsuario(Usuario usuario, Long agendamentoId) {
        return agendamentos.findById(agendamentoId)
                .filter(a -> a.getSolicitante().getId().equals(usuario.getId())
                        || a.getDestinatario().getId().equals(usuario.getId()))
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Reunião não encontrada"));
    }

    private Usuario participante(String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email == null ? "" : email)
                .orElseThrow(() -> erro(HttpStatus.FORBIDDEN, "Acesso não permitido"));
        if (usuario.getStatus() != StatusUsuario.APROVADO
                || (usuario.getPapel() != Papel.INVESTIDOR && usuario.getPapel() != Papel.STARTUP)) {
            throw erro(HttpStatus.FORBIDDEN, "Acesso não permitido");
        }
        return usuario;
    }

    private static ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
