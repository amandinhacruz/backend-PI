package com.projeto_pi.Projeto.PI.controller;
import com.projeto_pi.Projeto.PI.entity.Startup;
// Escrita de perfil: PerfilController (PUT /perfil/startup).
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

    @GetMapping
    public ResponseEntity<List<com.projeto_pi.Projeto.PI.dto.StartupFeedDTO>> listar(
            @RequestParam(required = false) String search, @RequestParam(required = false) String segment,
            @RequestParam(required = false) String stage, @RequestParam(required = false) String location) {
        return ResponseEntity.ok(startupService.feed(search, segment, stage, location));
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.projeto_pi.Projeto.PI.dto.StartupFeedDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(startupService.perfilAprovado(id));
    }

    @GetMapping("/{id}/materiais/{tipo}")
    public ResponseEntity<byte[]> material(@PathVariable Long id, @PathVariable String tipo) {
        startupService.perfilAprovado(id);
        Startup startup = startupService.buscarPorId(id);
        byte[] bytes;
        String name;
        if (tipo.equals("pitch")) {
            bytes = startup.getPitchDeckData(); name = startup.getPitchDeckName();
        } else if (tipo.equals("canvas")) {
            bytes = startup.getBusinessCanvasData(); name = startup.getBusinessCanvasName();
        } else {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado");
        }
        if (bytes == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado");
        String safeName = name == null ? "material" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", org.springframework.http.ContentDisposition.attachment().filename(safeName).build().toString())
            .header("X-Content-Type-Options", "nosniff").body(bytes);
    }
}
