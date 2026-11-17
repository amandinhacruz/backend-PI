package com.projeto_pi.Projeto.PI.dto;

public class PitchCanvasResponseDTO {

    private Long startupId;
    private String nome;
    private String pitch;
    private String canvas;

    public PitchCanvasResponseDTO() {
    }

    public PitchCanvasResponseDTO(
            Long startupId,
            String nome,
            String pitch,
            String canvas) {

        this.startupId = startupId;
        this.nome = nome;
        this.pitch = pitch;
        this.canvas = canvas;
    }

    public Long getStartupId() {
        return startupId;
    }

    public String getNome() {
        return nome;
    }

    public String getPitch() {
        return pitch;
    }

    public String getCanvas() {
        return canvas;
    }
}
