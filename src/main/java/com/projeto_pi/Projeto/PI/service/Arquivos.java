package com.projeto_pi.Projeto.PI.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

/** Validação dos arquivos enviados: foto de perfil e materiais da startup. */
public final class Arquivos {

    private Arquivos() {
    }

    public static final long FOTO_MAXIMA = 2L * 1024 * 1024;
    public static final long MATERIAL_MAXIMO = 10L * 1024 * 1024;

    public record Foto(byte[] dados, String tipo) {
    }

    /**
     * Foto de perfil: JPEG, PNG ou WebP de até 2 MB. O tipo é conferido pelos primeiros bytes
     * do arquivo, não pelo nome nem pelo cabeçalho enviado pelo navegador.
     */
    public static Foto foto(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw erro(HttpStatus.BAD_REQUEST, "Envie uma imagem");
        }
        if (arquivo.getSize() > FOTO_MAXIMA) {
            throw erro(HttpStatus.PAYLOAD_TOO_LARGE, "A foto deve ter no máximo 2 MB");
        }
        byte[] dados = bytes(arquivo);
        String tipo = tipoDaImagem(dados);
        if (tipo == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Envie uma foto em JPG, PNG ou WebP");
        }
        return new Foto(dados, tipo);
    }

    static String tipoDaImagem(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    /** Pitch deck e canvas: PDF, PPT ou PPTX de até 10 MB (mesma regra do cadastro). */
    public static void material(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            return;
        }
        String nome = Optional.ofNullable(arquivo.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!nome.matches(".*\\.(pdf|ppt|pptx)$")) {
            throw erro(HttpStatus.BAD_REQUEST, "Envie PDF, PPT ou PPTX");
        }
        if (arquivo.getSize() > MATERIAL_MAXIMO) {
            throw erro(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo excede 10 MB");
        }
    }

    public static byte[] bytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw erro(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo");
        }
    }

    private static ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
