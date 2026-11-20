package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.FotoPerfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface FotoPerfilRepository extends JpaRepository<FotoPerfil, Long> {

    Optional<FotoPerfil> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);

    /** Quais destes usuários têm foto (sem carregar as imagens). */
    @Query("select f.usuarioId from FotoPerfil f where f.usuarioId in :ids")
    Set<Long> comFoto(@Param("ids") Collection<Long> ids);

    void deleteByUsuarioId(Long usuarioId);
}
