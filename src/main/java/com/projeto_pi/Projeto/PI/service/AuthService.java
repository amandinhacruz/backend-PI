package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.dto.LoginDTO;
import com.projeto_pi.Projeto.PI.dto.LoginResponseDTO;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import com.projeto_pi.Projeto.PI.service.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponseDTO login(LoginDTO dto) {

        Usuario usuario = usuarioRepository
                .findByEmail(dto.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Email ou senha inválidos"));

        if (!passwordEncoder.matches(
                dto.getSenha(),
                usuario.getSenhaHash())) {

            throw new RuntimeException(
                    "Email ou senha inválidos");
        }

        if (usuario.getStatus() != com.projeto_pi.Projeto.PI.entity.StatusUsuario.APROVADO) {

            throw new RuntimeException(
                    "Usuário ainda não está aprovado"
            );
        }

        String token = jwtService.gerarToken(
                usuario.getEmail(),
                usuario.getPapel().name()
        );

        return new LoginResponseDTO(
                token,
                usuario.getEmail(),
                usuario.getPapel().name()
        );
    }
}