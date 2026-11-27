package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.dto.ComunidadeDTOs.Community;
import com.projeto_pi.Projeto.PI.dto.ComunidadeDTOs.Member;
import com.projeto_pi.Projeto.PI.dto.ComunidadeDTOs.Post;
import com.projeto_pi.Projeto.PI.dto.ComunidadeDTOs.PostRequest;
import com.projeto_pi.Projeto.PI.service.ComunidadeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/** Comunidades (conversa em grupo). Acesso: STARTUP e INVESTIDOR (SecurityConfig). */
@RestController
public class ComunidadeController {

    private final ComunidadeService service;

    public ComunidadeController(ComunidadeService service) {
        this.service = service;
    }

    @GetMapping("/comunidades")
    public List<Community> listar(Principal usuario) {
        return service.listar(usuario.getName());
    }

    @PostMapping("/comunidades/{id}/entrar")
    public Community entrar(@PathVariable Long id, Principal usuario) {
        return service.entrar(usuario.getName(), id);
    }

    @PostMapping("/comunidades/{id}/sair")
    public Community sair(@PathVariable Long id, Principal usuario) {
        return service.sair(usuario.getName(), id);
    }

    /** Sem {@code depois}: histórico recente. Com {@code depois}: só as mensagens novas. */
    @GetMapping("/comunidades/{id}/mensagens")
    public List<Post> mensagens(@PathVariable Long id, @RequestParam(required = false) Long depois,
                                Principal usuario) {
        return service.mensagens(usuario.getName(), id, depois);
    }

    @PostMapping("/comunidades/{id}/mensagens")
    @ResponseStatus(HttpStatus.CREATED)
    public Post enviar(@PathVariable Long id, @RequestBody PostRequest corpo, Principal usuario) {
        return service.enviar(usuario.getName(), id, corpo);
    }

    @GetMapping("/comunidades/{id}/membros")
    public List<Member> membros(@PathVariable Long id, Principal usuario) {
        return service.membros(usuario.getName(), id);
    }
}
