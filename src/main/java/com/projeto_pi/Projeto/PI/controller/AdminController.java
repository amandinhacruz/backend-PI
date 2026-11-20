package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.dto.AdminDTOs.*;
import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.matching.AnaliseAfinidadeService;
import com.projeto_pi.Projeto.PI.service.AdminService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * Painel administrativo. Acesso restrito a ADMIN (SecurityConfig: /admin/**).
 * As respostas usam DTOs: a versão anterior devolvia a entidade Usuario, expondo senhaHash.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService service;
    private final AnaliseAfinidadeService analisesIa;

    public AdminController(AdminService service, AnaliseAfinidadeService analisesIa) {
        this.service = service;
        this.analisesIa = analisesIa;
    }

    /** Situação do analista generativo (governança): ligado, chave, modelo, uso e último erro. */
    @GetMapping("/ia/status")
    public AnaliseAfinidadeService.StatusIa statusIa() {
        return analisesIa.status();
    }

    @GetMapping("/usuarios")
    public List<CadastroAdmin> listar(@RequestParam(required = false) String status) {
        return service.listarCadastros(status);
    }

    @PutMapping("/usuarios/{id}/aprovar")
    public CadastroAdmin aprovar(@PathVariable Long id, Principal admin) {
        return service.alterarStatus(id, StatusUsuario.APROVADO, ator(admin));
    }

    @PutMapping("/usuarios/{id}/rejeitar")
    public CadastroAdmin rejeitar(@PathVariable Long id, Principal admin) {
        return service.alterarStatus(id, StatusUsuario.REJEITADO, ator(admin));
    }

    @PutMapping("/usuarios/{id}/suspender")
    public CadastroAdmin suspender(@PathVariable Long id, Principal admin) {
        return service.alterarStatus(id, StatusUsuario.SUSPENSO, ator(admin));
    }

    @GetMapping("/metricas")
    public Metricas metricas() {
        return service.metricas();
    }

    @GetMapping("/auditoria")
    public List<Auditoria> auditoria() {
        return service.auditoria();
    }

    private static String ator(Principal admin) {
        return admin == null ? "desconhecido" : admin.getName();
    }
}
