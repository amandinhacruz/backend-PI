package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.dto.MatchResponseDTO;
import com.projeto_pi.Projeto.PI.service.MatchmakingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matchmaking")
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    public MatchmakingController(
            MatchmakingService matchmakingService) {

        this.matchmakingService = matchmakingService;
    }

    @GetMapping("/startup/{startupId}")
    public ResponseEntity<List<MatchResponseDTO>> encontrarMatches(
            @PathVariable Long startupId) {

        return ResponseEntity.ok(
                matchmakingService.encontrarMatches(startupId)
        );
    }
}
