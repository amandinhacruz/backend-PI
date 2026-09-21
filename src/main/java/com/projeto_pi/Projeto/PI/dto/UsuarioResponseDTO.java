package com.projeto_pi.Projeto.PI.dto;

import com.projeto_pi.Projeto.PI.entity.Papel;
import com.projeto_pi.Projeto.PI.entity.StatusUsuario;

public class UsuarioResponseDTO {

    private Long id;
    private String email;
    private Papel papel;
    private StatusUsuario status;

    public UsuarioResponseDTO() {
    }

    public UsuarioResponseDTO(
            Long id,
            String email,
            Papel papel,
            StatusUsuario status) {

        this.id = id;
        this.email = email;
        this.papel = papel;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Papel getPapel() {
        return papel;
    }

    public StatusUsuario getStatus() {
        return status;
    }
}
