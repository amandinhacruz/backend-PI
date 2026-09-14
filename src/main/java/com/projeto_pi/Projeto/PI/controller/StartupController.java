package com.projeto_pi.Projeto.PI.controller;
import com.projeto_pi.Projeto.PI.entity.Startup;
import com.projeto_pi.Projeto.PI.service.StartupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/startups")
public class StartupController {

    private final StartupService startupService;

    public StartupController(StartupService startupService) {
        this.startupService = startupService;
    }

    @PostMapping
    public ResponseEntity<Startup> cadastrar(@RequestBody Startup startup) {
        Startup novaStartup = startupService.cadastrar(startup);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(novaStartup);
    }

    @GetMapping
    public ResponseEntity<List<Startup>> listar() {
        return ResponseEntity.ok(startupService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Startup> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(startupService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Startup> atualizar(
            @PathVariable Long id,
            @RequestBody Startup startup) {

        return ResponseEntity.ok(
                startupService.atualizar(id, startup)
        );
    }
}
