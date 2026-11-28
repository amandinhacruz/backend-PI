package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.Publicacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Só entram no feed publicações não removidas de autores aprovados. */
public interface PublicacaoRepository extends JpaRepository<Publicacao, Long> {

    String VISIVEL = "p.removida = false and p.autor.status = com.projeto_pi.Projeto.PI.entity.StatusUsuario.APROVADO";

    /** Mais recentes primeiro; {@code antes} pagina para trás (use Long.MAX_VALUE na primeira página). */
    @Query("select p from Publicacao p join fetch p.autor where " + VISIVEL + " and p.id < :antes order by p.id desc")
    List<Publicacao> feed(@Param("antes") Long antes, Pageable limite);

    /** Publicações mais novas que {@code depois} (para avisar que chegaram novas). */
    @Query("select p from Publicacao p join fetch p.autor where " + VISIVEL + " and p.id > :depois order by p.id desc")
    List<Publicacao> novas(@Param("depois") Long depois, Pageable limite);

    /** Do autor (perfil): mesmas regras do feed. */
    @Query("select p from Publicacao p join fetch p.autor where " + VISIVEL
            + " and p.autor.id = :autorId and p.id < :antes order by p.id desc")
    List<Publicacao> doAutor(@Param("autorId") Long autorId, @Param("antes") Long antes, Pageable limite);

    /** Compartilhamentos visíveis por publicação: [publicacaoId, quantidade]. */
    @Query("select p.compartilhada.id, count(p) from Publicacao p where " + VISIVEL
            + " and p.compartilhada.id in :ids group by p.compartilhada.id")
    List<Object[]> contarCompartilhamentos(@Param("ids") Collection<Long> ids);

    /** Quais destas publicações o usuário já compartilhou (sem texto). */
    @Query("select p.compartilhada.id from Publicacao p where p.removida = false and p.autor.id = :usuarioId "
            + "and p.compartilhada.id in :ids and (p.conteudo is null)")
    List<Long> compartilhadasPor(@Param("usuarioId") Long usuarioId, @Param("ids") Collection<Long> ids);

    @Query("select p from Publicacao p where p.removida = false and p.autor.id = :usuarioId "
            + "and p.compartilhada.id = :originalId and p.conteudo is null")
    Optional<Publicacao> compartilhamentoSimples(@Param("usuarioId") Long usuarioId, @Param("originalId") Long originalId);
}
