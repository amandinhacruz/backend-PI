package com.projeto_pi.Projeto.PI.entity;

import jakarta.persistence.*;
import com.projeto_pi.Projeto.PI.config.StringCriptografadoConverter;
import jakarta.persistence.Convert;

@Entity
@Table(name = "startups")
public class Startup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Convert(converter = StringCriptografadoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String segmento;

    @Convert(converter = StringCriptografadoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String estagio;

    @Convert(converter = StringCriptografadoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String necessidades;

    @Convert(converter = StringCriptografadoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String pitch;

    @Convert(converter = StringCriptografadoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String canvas;

    public Startup() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSegmento() {
        return segmento;
    }

    public void setSegmento(String segmento) {
        this.segmento = segmento;
    }

    public String getEstagio() {
        return estagio;
    }

    public void setEstagio(String estagio) {
        this.estagio = estagio;
    }

    public String getNecessidades() {
        return necessidades;
    }

    public void setNecessidades(String necessidades) {
        this.necessidades = necessidades;
    }

    public String getPitch() {
        return pitch;
    }

    public void setPitch(String pitch) {
        this.pitch = pitch;
    }

    public String getCanvas() {
        return canvas;
    }

    public void setCanvas(String canvas) {
        this.canvas = canvas;
    }
}