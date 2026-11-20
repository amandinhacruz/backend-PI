package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    @Query("select a from Avaliacao a where a.conexao.id = :conexaoId and a.autor.id = :autorId")
    Optional<Avaliacao> findDoAutor(@Param("conexaoId") Long conexaoId, @Param("autorId") Long autorId);

    @Query("select a from Avaliacao a where a.autor.id = :autorId and a.conexao.id in :conexaoIds")
    List<Avaliacao> findDoAutorNasConexoes(@Param("autorId") Long autorId,
                                           @Param("conexaoIds") Collection<Long> conexaoIds);

    /** Linhas [usuarioId, média, total] das avaliações recebidas por cada usuário. */
    @Query("select a.avaliado.id, avg(a.nota), count(a) from Avaliacao a "
            + "where a.avaliado.id in :usuarioIds group by a.avaliado.id")
    List<Object[]> reputacaoPorUsuario(@Param("usuarioIds") Collection<Long> usuarioIds);

    @Query("select avg(a.nota) from Avaliacao a")
    Double notaMedia();
}
