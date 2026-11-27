package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.MembroComunidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MembroComunidadeRepository extends JpaRepository<MembroComunidade, Long> {

    Optional<MembroComunidade> findByComunidade_IdAndUsuario_Id(Long comunidadeId, Long usuarioId);

    @Query("select m.comunidade.id from MembroComunidade m where m.usuario.id = :usuarioId")
    List<Long> comunidadesDo(@Param("usuarioId") Long usuarioId);

    /** Membros aprovados, na ordem em que entraram. */
    @Query("select m from MembroComunidade m join fetch m.usuario u where m.comunidade.id = :comunidadeId "
            + "and u.status = com.projeto_pi.Projeto.PI.entity.StatusUsuario.APROVADO order by m.entrouEm, m.id")
    List<MembroComunidade> membros(@Param("comunidadeId") Long comunidadeId);

    /** Membros aprovados por comunidade: [comunidadeId, quantidade]. */
    @Query("select m.comunidade.id, count(m) from MembroComunidade m "
            + "where m.usuario.status = com.projeto_pi.Projeto.PI.entity.StatusUsuario.APROVADO group by m.comunidade.id")
    List<Object[]> contarMembros();
}
