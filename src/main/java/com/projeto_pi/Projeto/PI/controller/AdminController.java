package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UsuarioRepository usuarioRepository;

    public AdminController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @PutMapping("/usuarios/{id}/aprovar")
    public ResponseEntity<Usuario> aprovar(
            @PathVariable Long id) {

        Usuario usuario = usuarioRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"));

        usuario.setStatus(StatusUsuario.APROVADO);

        return ResponseEntity.ok(
                usuarioRepository.save(usuario)
        );
    }

    @PutMapping("/usuarios/{id}/rejeitar")
    public ResponseEntity<Usuario> rejeitar(
            @PathVariable Long id) {

        Usuario usuario = usuarioRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"));

        usuario.setStatus(StatusUsuario.REJEITADO);

        return ResponseEntity.ok(
                usuarioRepository.save(usuario)
        );
    }

    @PutMapping("/usuarios/{id}/suspender")
    public ResponseEntity<Usuario> suspender(
            @PathVariable Long id) {

        Usuario usuario = usuarioRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"));

        usuario.setStatus(StatusUsuario.SUSPENSO);

        return ResponseEntity.ok(
                usuarioRepository.save(usuario)
        );
    }
}
