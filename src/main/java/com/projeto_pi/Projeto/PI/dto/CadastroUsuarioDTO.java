package com.projeto_pi.Projeto.PI.dto;

import com.projeto_pi.Projeto.PI.entity.Papel;

public class CadastroUsuarioDTO {

    private String email;
    private String senha;
    private Papel papel;

    public CadastroUsuarioDTO() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Papel getPapel() {
        return papel;
    }

    public void setPapel(Papel papel) {
        this.papel = papel;
    }
}
