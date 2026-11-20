package com.projeto_pi.Projeto.PI.service;

import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class RegistrationValidation {
    private RegistrationValidation() {}
    public static void required(String value, String field) {
        if (value == null || value.isBlank()) bad(field + " é obrigatório");
    }
    public static String credentials(String email, String password, String confirmation) {
        required(email, "E-mail"); required(password, "Senha");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) bad("E-mail inválido");
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) bad("Senha excede 72 bytes");
        if (confirmation != null && !password.equals(confirmation)) bad("As senhas não coincidem");
        return normalized;
    }
    public static void consents(Boolean terms, Boolean privacy) {
        if (Boolean.FALSE.equals(terms) || Boolean.FALSE.equals(privacy)) bad("Aceite os termos e a política de privacidade");
    }
    public static void tickets(BigDecimal min, BigDecimal max) {
        if ((min != null && min.signum() < 0) || (max != null && max.signum() < 0)
                || (min != null && max != null && min.compareTo(max) > 0)) bad("Intervalo de ticket inválido");
    }
    /** Política de senha do cadastro: mínimo 8 caracteres, com número e caractere especial. */
    public static void passwordPolicy(String password) {
        if (password == null || password.length() < 8
                || !password.matches(".*\\d.*")
                || !password.matches(".*[^\\p{L}\\d\\s].*"))
            bad("A senha deve ter no mínimo 8 caracteres, incluindo número e caractere especial");
    }
    /** Normaliza documento (remove máscara) e valida a quantidade de dígitos quando informado. */
    public static String document(String value, int digits, String label) {
        if (value == null || value.isBlank()) return null;
        String onlyDigits = value.replaceAll("\\D", "");
        if (onlyDigits.length() != digits) bad(label + " deve conter " + digits + " dígitos");
        return onlyDigits;
    }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
