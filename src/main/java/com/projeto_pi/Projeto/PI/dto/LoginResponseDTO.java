package com.projeto_pi.Projeto.PI.dto;

public class LoginResponseDTO {
    private String token;
    private String email;
    private String papel;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(String token, String email, String papel) {
        this.token = token;
        this.email = email;
        this.papel = papel;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPapel() {
        return papel;
    }

    public void setPapel(String papel) {
        this.papel = papel;
    }
}
