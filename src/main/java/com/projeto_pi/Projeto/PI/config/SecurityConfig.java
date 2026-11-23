package com.projeto_pi.Projeto.PI.config;

import com.projeto_pi.Projeto.PI.security.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> response.sendError(401)))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/auth/cadastro/**",
                                "/auth/login",
                                "/politica-privacidade",
                                // Página interna de erro do Spring: sem esta liberação, qualquer erro
                                // inesperado chegava ao navegador como 403 "Acesso não permitido".
                                "/error"
                        ).permitAll()

                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/startups", "/startups/**")
                        .hasAnyRole("INVESTIDOR", "STARTUP", "ADMIN")

                        .requestMatchers("/startups/**")
                        .hasRole("STARTUP")

                        .requestMatchers("/investidores-mentores/**")
                        .hasRole("INVESTIDOR")

                        .requestMatchers("/matchmaking/investidor/**")
                        .hasRole("INVESTIDOR")

                        .requestMatchers("/matchmaking/startup/pedidos/**")
                        .hasRole("STARTUP")

                        .requestMatchers("/matchmaking/startup/**")
                        .hasAnyRole("STARTUP", "ADMIN")

                        .requestMatchers("/conexoes/**")
                        .hasAnyRole("STARTUP", "INVESTIDOR")

                        .requestMatchers("/perfil", "/perfil/**")
                        .hasAnyRole("STARTUP", "INVESTIDOR")

                        .requestMatchers("/conversas/**", "/reunioes/**")
                        .hasAnyRole("STARTUP", "INVESTIDOR")

                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}