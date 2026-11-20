package com.projeto_pi.Projeto.PI.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {
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
}
