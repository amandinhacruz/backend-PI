package com.projeto_pi.Projeto.PI.entity;

import com.projeto_pi.Projeto.PI.config.ArquivoCriptografadoConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Foto de perfil de um usuário, numa tabela própria: assim carregar o usuário (o que acontece
 * em toda requisição autenticada) não traz a imagem junto. Guardada criptografada (RNF02).
 */
@Entity
@Table(name = "fotos_perfil")
public class FotoPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId;

    @Column(columnDefinition = "bytea", nullable = false)
    @Convert(converter = ArquivoCriptografadoConverter.class)
    private byte[] dados;

    /** image/jpeg, image/png ou image/webp, conferido pelos bytes do arquivo. */
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    protected FotoPerfil() {
    }

    public FotoPerfil(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public void registrar(byte[] dados, String tipo) {
        this.dados = dados;
        this.tipo = tipo;
        this.atualizadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public byte[] getDados() { return dados; }
    public String getTipo() { return tipo; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
}
