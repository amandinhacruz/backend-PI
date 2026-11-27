package com.projeto_pi.Projeto.PI.config;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.*;
@Configuration
public class CorsConfig {
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://127.0.0.1:5173, https://vertice-connect.netlify.app}") String origins,
            // Só no perfil dev (application-dev.properties): qualquer porta local, porque o Vite muda
            // de porta (5174, 5175...) quando a 5173 já está ocupada. Em produção fica vazio.
            @Value("${vertice.cors.padroes-locais:}") String padroesLocais) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(java.util.Arrays.stream(origins.split(",")).map(String::trim).toList());
        java.util.Arrays.stream(padroesLocais.split(",")).map(String::trim).filter(p -> !p.isEmpty())
                .forEach(config::addAllowedOriginPattern);
        // DELETE: remover foto de perfil e materiais (pitch deck/canvas) pelo navegador.
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // O front lê o nome do arquivo nos downloads de materiais (pitch deck e canvas).
        config.setExposedHeaders(List.of("Content-Disposition"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
