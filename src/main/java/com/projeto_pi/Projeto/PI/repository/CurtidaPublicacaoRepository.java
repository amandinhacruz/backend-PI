package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.CurtidaPublicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CurtidaPublicacaoRepository extends JpaRepository<CurtidaPublicacao, Long> {

    Optional<CurtidaPublicacao> findByPublicacao_IdAndUsuario_Id(Long publicacaoId, Long usuarioId);

    /** Curtidas de usuários aprovados por publicação: [publicacaoId, quantidade]. */
    @Query("select c.publicacao.id, count(c) from CurtidaPublicacao c where c.publicacao.id in :ids "
            + "and c.usuario.status = com.projeto_pi.Projeto.PI.entity.StatusUsuario.APROVADO group by c.publicacao.id")
    List<Object[]> contar(@Param("ids") Collection<Long> ids);

    @Query("select c.publicacao.id from CurtidaPublicacao c where c.usuario.id = :usuarioId and c.publicacao.id in :ids")
    List<Long> curtidasPor(@Param("usuarioId") Long usuarioId, @Param("ids") Collection<Long> ids);
}
