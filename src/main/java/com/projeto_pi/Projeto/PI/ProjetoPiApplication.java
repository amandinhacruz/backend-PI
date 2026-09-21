package com.projeto_pi.Projeto.PI;

import com.projeto_pi.Projeto.PI.entity.Papel;
import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class ProjetoPiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjetoPiApplication.class, args);
	}

	@Bean
	CommandLineRunner criarAdmin(
			UsuarioRepository usuarioRepository,
			PasswordEncoder passwordEncoder) {

		return args -> {

			if (usuarioRepository
					.findByEmail("admin@projetopi.com")
					.isEmpty()) {

				Usuario admin = new Usuario();

				admin.setEmail("admin@projetopi.com");

				admin.setSenhaHash(
						passwordEncoder.encode("admin123")
				);

				admin.setPapel(Papel.ADMIN);

				admin.setStatus(StatusUsuario.APROVADO);

				admin.setConsentiuEm(true);

				usuarioRepository.save(admin);
			}
		};
	}
}