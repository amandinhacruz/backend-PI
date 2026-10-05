package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.dto.AgendamentoDTO;
import com.projeto_pi.Projeto.PI.dto.AgendamentoResponseDTO;
import com.projeto_pi.Projeto.PI.service.AgendamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agendamentos")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(
            AgendamentoService agendamentoService) {

        this.agendamentoService = agendamentoService;
    }

    @PostMapping
    public ResponseEntity<AgendamentoResponseDTO> criar(
            @RequestBody AgendamentoDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(agendamentoService.criar(dto));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<AgendamentoResponseDTO>> listarPorUsuario(
            @PathVariable Long usuarioId) {

        return ResponseEntity.ok(
                agendamentoService.listarPorUsuario(usuarioId)
        );
    }

    @PutMapping("/{id}/aceitar")
    public ResponseEntity<AgendamentoResponseDTO> aceitar(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                agendamentoService.aceitar(id)
        );
    }

    @PutMapping("/{id}/rejeitar")
    public ResponseEntity<AgendamentoResponseDTO> rejeitar(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                agendamentoService.rejeitar(id)
        );
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<AgendamentoResponseDTO> cancelar(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                agendamentoService.cancelar(id)
        );
    }
}
