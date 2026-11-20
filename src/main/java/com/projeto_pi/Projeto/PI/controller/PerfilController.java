package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.dto.PerfilDTOs.InvestorProfile;
import com.projeto_pi.Projeto.PI.dto.PerfilDTOs.InvestorUpdate;
import com.projeto_pi.Projeto.PI.dto.PerfilDTOs.OwnProfile;
import com.projeto_pi.Projeto.PI.dto.PerfilDTOs.StartupProfile;
import com.projeto_pi.Projeto.PI.dto.PerfilDTOs.StartupUpdate;
import com.projeto_pi.Projeto.PI.entity.FotoPerfil;
import com.projeto_pi.Projeto.PI.service.PerfilService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

/** Perfis, foto e materiais. Acesso: /perfil/** para STARTUP e INVESTIDOR (SecurityConfig). */
@RestController
public class PerfilController {

    private final PerfilService service;

    public PerfilController(PerfilService service) {
        this.service = service;
    }

    @GetMapping("/perfil")
    public OwnProfile proprio(Principal usuario) {
        return service.proprio(usuario.getName());
    }

    @PutMapping("/perfil/startup")
    public StartupProfile editarStartup(@RequestBody StartupUpdate corpo, Principal usuario) {
        return service.editarStartup(usuario.getName(), corpo);
    }

    @PutMapping("/perfil/investidor")
    public InvestorProfile editarInvestidor(@RequestBody InvestorUpdate corpo, Principal usuario) {
        return service.editarInvestidor(usuario.getName(), corpo);
    }

    @PutMapping(value = "/perfil/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> foto(@RequestPart("photo") MultipartFile foto, Principal usuario) {
        service.salvarFoto(usuario.getName(), foto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/perfil/foto")
    public ResponseEntity<Void> removerFoto(Principal usuario) {
        service.removerFoto(usuario.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/perfil/materiais/{tipo}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StartupProfile material(@PathVariable String tipo, @RequestPart("file") MultipartFile arquivo,
                                   Principal usuario) {
        return service.salvarMaterial(usuario.getName(), tipo, arquivo);
    }

    @DeleteMapping("/perfil/materiais/{tipo}")
    public StartupProfile removerMaterial(@PathVariable String tipo, Principal usuario) {
        return service.removerMaterial(usuario.getName(), tipo);
    }

    /** Perfil público de uma startup aprovada (investidores, startups e admin). */
    @GetMapping("/startups/{id}/perfil")
    public StartupProfile startup(@PathVariable Long id) {
        return service.startupPublica(id);
    }

    /** Foto de perfil. Só de usuários aprovados, para usuários aprovados (ou a própria). */
    @GetMapping("/usuarios/{id}/foto")
    public ResponseEntity<byte[]> fotoDoUsuario(@PathVariable Long id, Principal usuario) {
        FotoPerfil foto = service.foto(usuario.getName(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getTipo()))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(foto.getDados());
    }
}
