package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.ComentarioPublicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ComentarioPublicacaoRepository extends JpaRepository<ComentarioPublicacao, Long> {

    String VISIVEL = "c.autor.status = com.projeto_pi.Projeto.PI.entity.StatusUsuario.APROVADO";

    @Query("select c from ComentarioPublicacao c join fetch c.autor where c.publicacao.id = :publicacaoId and "
            + VISIVEL + " order by c.id")
    List<ComentarioPublicacao> daPublicacao(@Param("publicacaoId") Long publicacaoId);

    /** Comentários visíveis por publicação: [publicacaoId, quantidade]. */
    @Query("select c.publicacao.id, count(c) from ComentarioPublicacao c where c.publicacao.id in :ids and "
            + VISIVEL + " group by c.publicacao.id")
    List<Object[]> contar(@Param("ids") Collection<Long> ids);
}
