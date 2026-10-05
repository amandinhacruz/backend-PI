package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.dto.EnviarMensagemDTO;
import com.projeto_pi.Projeto.PI.dto.MensagemResponseDTO;
import com.projeto_pi.Projeto.PI.service.MensagemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mensagens")
public class MensagemController {

    private final MensagemService mensagemService;

    public MensagemController(MensagemService mensagemService) {
        this.mensagemService = mensagemService;
    }

    @PostMapping
    public ResponseEntity<MensagemResponseDTO> enviar(
            @RequestBody EnviarMensagemDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mensagemService.enviar(dto));
    }

    @GetMapping("/conversa/{usuarioId}/{outroUsuarioId}")
    public ResponseEntity<List<MensagemResponseDTO>> buscarConversa(
            @PathVariable Long usuarioId,
            @PathVariable Long outroUsuarioId) {

        return ResponseEntity.ok(
                mensagemService.buscarConversa(
                        usuarioId,
                        outroUsuarioId
                )
        );
    }
}
