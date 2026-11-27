package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.dto.CrescimentoDTOs.Dashboard;
import com.projeto_pi.Projeto.PI.dto.CrescimentoDTOs.SettingsRequest;
import com.projeto_pi.Projeto.PI.dto.CrescimentoDTOs.UpdateRequest;
import com.projeto_pi.Projeto.PI.service.CrescimentoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Aba "Crescimento". /crescimento/**: a própria startup (STARTUP). GET /startups/{id}/crescimento:
 * investidores aprovados acompanham uma startup (regra no serviço; SecurityConfig libera o GET).
 */
@RestController
public class CrescimentoController {

    private final CrescimentoService service;

    public CrescimentoController(CrescimentoService service) {
        this.service = service;
    }

    @GetMapping("/crescimento")
    public Dashboard meu(Principal usuario) {
        return service.meu(usuario.getName());
    }

    @PutMapping("/crescimento/configuracao")
    public Dashboard configurar(@RequestBody SettingsRequest corpo, Principal usuario) {
        return service.configurar(usuario.getName(), corpo);
    }

    /** Cria ou corrige a atualização do mês informado (AAAA-MM). */
    @PutMapping("/crescimento/atualizacoes/{mes}")
    public Dashboard atualizar(@PathVariable String mes, @RequestBody UpdateRequest corpo, Principal usuario) {
        return service.atualizar(usuario.getName(), mes, corpo);
    }

    @GetMapping("/startups/{id}/crescimento")
    public Dashboard daStartup(@PathVariable Long id, Principal usuario) {
        return service.daStartup(usuario.getName(), id);
    }
}
