package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.entity.InvestidorMentor;
import com.projeto_pi.Projeto.PI.service.InvestidorMentorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/investidores-mentores")
public class InvestidorMentorController {

    private final InvestidorMentorService service;

    public InvestidorMentorController(
            InvestidorMentorService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InvestidorMentor> cadastrar(
            @RequestBody InvestidorMentor investidorMentor) {

        InvestidorMentor novo = service.cadastrar(investidorMentor);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(novo);
    }

    @GetMapping
    public ResponseEntity<List<InvestidorMentor>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvestidorMentor> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvestidorMentor> atualizar(
            @PathVariable Long id,
            @RequestBody InvestidorMentor investidorMentor) {

        return ResponseEntity.ok(
                service.atualizar(id, investidorMentor)
        );
    }
}
