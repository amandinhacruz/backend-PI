package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.dto.AtualizarPitchCanvasDTO;
import com.projeto_pi.Projeto.PI.dto.PitchCanvasResponseDTO;
import com.projeto_pi.Projeto.PI.service.PitchCanvasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/startups/meu-perfil/pitch-canvas")
public class PitchCanvasController {

    private final PitchCanvasService pitchCanvasService;

    public PitchCanvasController(
            PitchCanvasService pitchCanvasService) {

        this.pitchCanvasService = pitchCanvasService;
    }

    @GetMapping
    public ResponseEntity<PitchCanvasResponseDTO> consultar(
            Principal principal) {

        return ResponseEntity.ok(
                pitchCanvasService.consultar(principal.getName())
        );
    }

    @PutMapping
    public ResponseEntity<PitchCanvasResponseDTO> atualizar(
            Principal principal,
            @RequestBody AtualizarPitchCanvasDTO dto) {

        return ResponseEntity.ok(
                pitchCanvasService.atualizar(
                        principal.getName(),
                        dto
                )
        );
    }
}
