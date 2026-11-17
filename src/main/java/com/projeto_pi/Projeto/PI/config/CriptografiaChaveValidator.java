package com.projeto_pi.Projeto.PI.config;

import com.projeto_pi.Projeto.PI.security.CriptografiaUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Interrompe a inicialização se VERTICE_ENCRYPTION_KEY estiver ausente ou inválida,
 * em vez de deixar a aplicação subir e falhar com 500 no primeiro cadastro.
 */
@Component
public class CriptografiaChaveValidator {

    @PostConstruct
    void validar() {
        try {
            CriptografiaUtil.validarChave();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "VERTICE_ENCRYPTION_KEY não é um Base64 válido de 32 bytes.", e);
        }
    }
}
