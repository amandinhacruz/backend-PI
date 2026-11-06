package com.projeto_pi.Projeto.PI.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class CriptografiaUtil {

    private static final String PREFIXO = "ENCv1:";

    private static final int TAMANHO_IV = 12;
    private static final int TAMANHO_TAG = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private CriptografiaUtil() {
    }

    public static boolean estaCriptografado(String valor) {
        return valor != null && valor.startsWith(PREFIXO);
    }

    public static String criptografar(String valor) {

        if (valor == null || valor.isEmpty()) {
            return valor;
        }

        if (estaCriptografado(valor)) {
            return valor;
        }

        try {
            byte[] iv = new byte[TAMANHO_IV];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    obterChave(),
                    new GCMParameterSpec(TAMANHO_TAG, iv)
            );

            byte[] criptografado = cipher.doFinal(
                    valor.getBytes(StandardCharsets.UTF_8)
            );

            byte[] resultado = new byte[iv.length + criptografado.length];

            System.arraycopy(iv, 0, resultado, 0, iv.length);

            System.arraycopy(
                    criptografado,
                    0,
                    resultado,
                    iv.length,
                    criptografado.length
            );

            return PREFIXO + Base64.getEncoder()
                    .encodeToString(resultado);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Erro ao criptografar os dados.",
                    e
            );
        }
    }

    public static String descriptografar(String valor) {

        if (valor == null || valor.isEmpty()) {
            return valor;
        }

        // Compatibilidade com registros antigos ainda em texto puro.
        if (!estaCriptografado(valor)) {
            return valor;
        }

        try {
            byte[] dados = Base64.getDecoder()
                    .decode(valor.substring(PREFIXO.length()));

            if (dados.length < TAMANHO_IV + 16) {
                throw new IllegalArgumentException(
                        "Conteúdo criptografado inválido."
                );
            }

            byte[] iv = Arrays.copyOfRange(
                    dados,
                    0,
                    TAMANHO_IV
            );

            byte[] criptografado = Arrays.copyOfRange(
                    dados,
                    TAMANHO_IV,
                    dados.length
            );

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    obterChave(),
                    new GCMParameterSpec(TAMANHO_TAG, iv)
            );

            byte[] original = cipher.doFinal(criptografado);

            return new String(original, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Erro ao descriptografar os dados. "
                            + "Verifique a chave de criptografia.",
                    e
            );
        }
    }

    private static SecretKey obterChave() {

        String chaveConfigurada =
                System.getenv("VERTICE_ENCRYPTION_KEY");

        if (chaveConfigurada == null || chaveConfigurada.isBlank()) {
            throw new IllegalStateException(
                    "A variável VERTICE_ENCRYPTION_KEY não foi configurada."
            );
        }

        byte[] chave = Base64.getDecoder().decode(chaveConfigurada);

        if (chave.length != 32) {
            throw new IllegalStateException(
                    "A chave de criptografia deve possuir 32 bytes."
            );
        }

        return new SecretKeySpec(chave, "AES");
    }
}
