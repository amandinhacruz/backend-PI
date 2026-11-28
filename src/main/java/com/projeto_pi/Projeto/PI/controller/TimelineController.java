package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.dto.TimelineDTOs.Comment;
import com.projeto_pi.Projeto.PI.dto.TimelineDTOs.CommentRequest;
import com.projeto_pi.Projeto.PI.dto.TimelineDTOs.Post;
import com.projeto_pi.Projeto.PI.dto.TimelineDTOs.PostRequest;
import com.projeto_pi.Projeto.PI.service.TimelineService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/** Timeline. Acesso: STARTUP e INVESTIDOR (SecurityConfig). */
@RestController
public class TimelineController {

    private final TimelineService service;

    public TimelineController(TimelineService service) {
        this.service = service;
    }

    /** Sem parâmetros: primeira página. {@code antes}: página seguinte. {@code depois}: só as novas. */
    @GetMapping("/timeline")
    public List<Post> feed(@RequestParam(required = false) Long antes, @RequestParam(required = false) Long depois,
                           Principal usuario) {
        return service.feed(usuario.getName(), antes, depois);
    }

    @GetMapping("/timeline/usuarios/{usuarioId}")
    public List<Post> doUsuario(@PathVariable Long usuarioId, @RequestParam(required = false) Long antes,
                                Principal usuario) {
        return service.doUsuario(usuario.getName(), usuarioId, antes);
    }

    @PostMapping("/timeline")
    @ResponseStatus(HttpStatus.CREATED)
    public Post publicar(@RequestBody PostRequest corpo, Principal usuario) {
        return service.publicar(usuario.getName(), corpo);
    }

    @GetMapping("/timeline/{id}")
    public Post uma(@PathVariable Long id, Principal usuario) {
        return service.uma(usuario.getName(), id);
    }

    @DeleteMapping("/timeline/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, Principal usuario) {
        service.excluir(usuario.getName(), id);
    }

    @PostMapping("/timeline/{id}/curtida")
    public Post curtir(@PathVariable Long id, Principal usuario) {
        return service.curtir(usuario.getName(), id, true);
    }

    @DeleteMapping("/timeline/{id}/curtida")
    public Post descurtir(@PathVariable Long id, Principal usuario) {
        return service.curtir(usuario.getName(), id, false);
    }

    /** Desfaz o compartilhamento simples da publicação {@code id}. */
    @DeleteMapping("/timeline/{id}/compartilhamento")
    public Post descompartilhar(@PathVariable Long id, Principal usuario) {
        return service.descompartilhar(usuario.getName(), id);
    }

    @GetMapping("/timeline/{id}/comentarios")
    public List<Comment> comentarios(@PathVariable Long id, Principal usuario) {
        return service.comentarios(usuario.getName(), id);
    }

    @PostMapping("/timeline/{id}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    public Comment comentar(@PathVariable Long id, @RequestBody CommentRequest corpo, Principal usuario) {
        return service.comentar(usuario.getName(), id, corpo == null ? null : corpo.text());
    }

    @DeleteMapping("/timeline/comentarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirComentario(@PathVariable Long id, Principal usuario) {
        service.excluirComentario(usuario.getName(), id);
    }
}
