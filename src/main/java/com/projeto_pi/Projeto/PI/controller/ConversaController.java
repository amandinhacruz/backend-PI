package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.dto.ConversaDTOs.ChatMessage;
import com.projeto_pi.Projeto.PI.dto.ConversaDTOs.Conversation;
import com.projeto_pi.Projeto.PI.dto.ConversaDTOs.InviteRequest;
import com.projeto_pi.Projeto.PI.dto.ConversaDTOs.Meeting;
import com.projeto_pi.Projeto.PI.dto.ConversaDTOs.MeetingDecisionRequest;
import com.projeto_pi.Projeto.PI.dto.ConversaDTOs.SendMessageRequest;
import com.projeto_pi.Projeto.PI.service.ConversaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

/**
 * Mensagens (RF05) e reuniões. Substitui as rotas antigas /mensagens e /agendamentos,
 * que recebiam o id do remetente no corpo. Acesso: STARTUP e INVESTIDOR (SecurityConfig).
 */
@RestController
public class ConversaController {

    private final ConversaService service;

    public ConversaController(ConversaService service) {
        this.service = service;
    }

    @GetMapping("/conversas")
    public List<Conversation> conversas(Principal usuario) {
        return service.conversas(usuario.getName());
    }

    @GetMapping("/conversas/{conexaoId}/mensagens")
    public List<ChatMessage> historico(@PathVariable Long conexaoId, Principal usuario) {
        return service.historico(usuario.getName(), conexaoId);
    }

    @PostMapping("/conversas/{conexaoId}/mensagens")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessage enviar(@PathVariable Long conexaoId, @RequestBody SendMessageRequest corpo,
                              Principal usuario) {
        return service.enviar(usuario.getName(), conexaoId, corpo == null ? null : corpo.text());
    }

    /** Convite de reunião (só o investidor da conexão). */
    @PostMapping("/conversas/{conexaoId}/convites")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessage convidar(@PathVariable Long conexaoId, @RequestBody InviteRequest corpo, Principal usuario) {
        return service.convidar(usuario.getName(), conexaoId, corpo);
    }

    @GetMapping("/reunioes")
    public List<Meeting> reunioes(Principal usuario) {
        return service.reunioes(usuario.getName());
    }

    @PostMapping("/reunioes/{id}/resposta")
    public Meeting responder(@PathVariable Long id, @RequestBody MeetingDecisionRequest corpo, Principal usuario) {
        String decisao = corpo == null || corpo.decision() == null
                ? "" : corpo.decision().trim().toLowerCase(Locale.ROOT);
        boolean aceitar = switch (decisao) {
            case "accepted", "aceitar" -> true;
            case "rejected", "recusar" -> false;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe decision: accepted ou rejected");
        };
        return service.responder(usuario.getName(), id, aceitar);
    }

    @PostMapping("/reunioes/{id}/cancelar")
    public Meeting cancelar(@PathVariable Long id, Principal usuario) {
        return service.cancelar(usuario.getName(), id);
    }
}
