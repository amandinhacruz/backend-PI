package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.dto.MatchResponseDTO;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.ConnectionItem;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.DecisionRequest;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.DecisionResult;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.Dossier;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.Queue;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.Rating;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.RatingRequest;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.RequestCard;
import com.projeto_pi.Projeto.PI.service.ConexaoService;
import com.projeto_pi.Projeto.PI.service.MatchmakingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

/** Matchmaking e conexões. As regras de acesso por papel estão em SecurityConfig. */
@RestController
public class MatchmakingController {

    private final MatchmakingService matchmaking;
    private final ConexaoService conexoes;

    public MatchmakingController(MatchmakingService matchmaking, ConexaoService conexoes) {
        this.matchmaking = matchmaking;
        this.conexoes = conexoes;
    }

    /* ------------------------------------------------------------ portal do investidor */

    @GetMapping("/matchmaking/investidor")
    public Queue fila(Principal usuario) {
        return matchmaking.filaDoInvestidor(usuario.getName());
    }

    @PostMapping("/matchmaking/investidor/startups/{startupId}/decisao")
    public DecisionResult decidir(@PathVariable Long startupId, @RequestBody DecisionRequest corpo,
                                  Principal usuario) {
        return matchmaking.decidir(usuario.getName(), startupId, aceita(corpo));
    }

    @GetMapping("/matchmaking/investidor/startups/{startupId}/dossie")
    public Dossier dossie(@PathVariable Long startupId, Principal usuario) {
        return matchmaking.dossie(usuario.getName(), startupId);
    }

    /* --------------------------------------------------------------- portal da startup */

    @GetMapping("/matchmaking/startup/pedidos")
    public List<RequestCard> pedidos(Principal usuario) {
        return matchmaking.pedidosDaStartup(usuario.getName());
    }

    @PostMapping("/matchmaking/startup/pedidos/{conexaoId}/decisao")
    public DecisionResult responder(@PathVariable Long conexaoId, @RequestBody DecisionRequest corpo,
                                    Principal usuario) {
        return matchmaking.responder(usuario.getName(), conexaoId, aceita(corpo));
    }

    /** Contrato legado: investidores ordenados por afinidade com a startup. */
    @GetMapping("/matchmaking/startup/{startupId}")
    public List<MatchResponseDTO> encontrarMatches(@PathVariable Long startupId, Authentication autenticacao) {
        boolean administrador = autenticacao.getAuthorities().stream()
                .anyMatch(papel -> "ROLE_ADMIN".equals(papel.getAuthority()));
        return matchmaking.encontrarMatches(startupId, autenticacao.getName(), administrador);
    }

    /* ----------------------------------------------------------------------- conexões */

    @GetMapping("/conexoes")
    public List<ConnectionItem> conexoes(Principal usuario) {
        return conexoes.listar(usuario.getName());
    }

    /** RF10: avaliação pós-interação. */
    @PutMapping("/conexoes/{conexaoId}/avaliacao")
    public Rating avaliar(@PathVariable Long conexaoId, @RequestBody RatingRequest corpo, Principal usuario) {
        return conexoes.avaliar(usuario.getName(), conexaoId, corpo.score(), corpo.comment());
    }

    private static boolean aceita(DecisionRequest corpo) {
        String decisao = corpo == null || corpo.decision() == null
                ? ""
                : corpo.decision().trim().toLowerCase(Locale.ROOT);
        return switch (decisao) {
            case "accepted", "solicitar", "firmar" -> true;
            case "rejected", "passar", "negar" -> false;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe decision: accepted ou rejected");
        };
    }
}
