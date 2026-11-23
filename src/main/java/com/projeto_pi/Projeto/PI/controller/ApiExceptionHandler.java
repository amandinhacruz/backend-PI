package com.projeto_pi.Projeto.PI.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of(
            "status", error.getStatusCode().value(), "message", error.getReason() == null ? "Requisição inválida" : error.getReason()));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<?> invalid(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("status", 400, "message", "Formato de dados inválido"));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict() {
        return ResponseEntity.status(409).body(Map.of("status", 409, "message", "Cadastro conflitante com dados existentes"));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> tooLarge() {
        return ResponseEntity.status(413).body(Map.of("status", 413, "message", "Arquivo excede o limite permitido"));
    }

    /**
     * Qualquer outro erro: registra no log e responde 500 com mensagem genérica. Antes ele ia para a
     * página interna /error e o navegador recebia 403, confundido com falta de permissão.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception error) {
        // Erros do próprio Spring MVC (rota inexistente, método não suportado, parte ausente...)
        // já trazem o status certo: mantém 404/405/400 em vez de virar 500.
        if (error instanceof org.springframework.web.ErrorResponse resposta) {
            int codigo = resposta.getStatusCode().value();
            return ResponseEntity.status(codigo).body(Map.of("status", codigo,
                "message", codigo == 404 ? "Recurso não encontrado" : "Requisição inválida"));
        }
        LOG.error("Erro inesperado ao processar a requisição", error);
        return ResponseEntity.status(500).body(Map.of("status", 500,
            "message", "Erro inesperado no servidor. Tente novamente em instantes."));
    }
}
