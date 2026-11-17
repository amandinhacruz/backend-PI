package com.projeto_pi.Projeto.PI.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class LoginDTO {
    private String email;
    @JsonAlias("password")
    private String senha;

    public LoginDTO() {
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

}
