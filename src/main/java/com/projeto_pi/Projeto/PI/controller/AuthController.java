package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.dto.CadastroInvestidorMentorDTO;
import com.projeto_pi.Projeto.PI.dto.CadastroStartupDTO;
import com.projeto_pi.Projeto.PI.dto.LoginDTO;
import com.projeto_pi.Projeto.PI.dto.LoginResponseDTO;
import com.projeto_pi.Projeto.PI.dto.UsuarioResponseDTO;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.service.AuthService;
import com.projeto_pi.Projeto.PI.service.UsuarioService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuthService authService;

    public AuthController(
            UsuarioService usuarioService,
            AuthService authService) {

        this.usuarioService = usuarioService;
        this.authService = authService;
    }

    @PostMapping("/cadastro/startup")
    public ResponseEntity<UsuarioResponseDTO> cadastrarStartup(
            @RequestBody CadastroStartupDTO dto) {

        Usuario usuario =
                usuarioService.cadastrarStartup(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new UsuarioResponseDTO(
                        usuario.getId(),
                        usuario.getEmail(),
                        usuario.getPapel(),
                        usuario.getStatus()
                ));
    }

    @PostMapping("/cadastro/investidor")
    public ResponseEntity<UsuarioResponseDTO> cadastrarInvestidor(
            @RequestBody CadastroInvestidorMentorDTO dto) {

        Usuario usuario =
                usuarioService.cadastrarInvestidorMentor(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new UsuarioResponseDTO(
                        usuario.getId(),
                        usuario.getEmail(),
                        usuario.getPapel(),
                        usuario.getStatus()
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @RequestBody LoginDTO dto) {

        return ResponseEntity.ok(
                authService.login(dto)
        );
    }
}
