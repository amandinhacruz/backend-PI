package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.MensagemComunidade;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** Mensagens de usuários suspensos ou rejeitados deixam de aparecer (só remetentes aprovados). */
public interface MensagemComunidadeRepository extends JpaRepository<MensagemComunidade, Long> {

    String VISIVEL = "m.remetente.status = com.projeto_pi.Projeto.PI.entity.StatusUsuario.APROVADO";

    /** Mais recentes primeiro (o serviço inverte para a ordem do chat). */
    @Query("select m from MensagemComunidade m join fetch m.remetente where m.comunidade.id = :comunidadeId and "
            + VISIVEL + " order by m.id desc")
    List<MensagemComunidade> recentes(@Param("comunidadeId") Long comunidadeId, Pageable limite);

    /** Mensagens posteriores a um id, para a atualização periódica do chat. */
    @Query("select m from MensagemComunidade m join fetch m.remetente where m.comunidade.id = :comunidadeId "
            + "and m.id > :depois and " + VISIVEL + " order by m.id")
    List<MensagemComunidade> depoisDe(@Param("comunidadeId") Long comunidadeId, @Param("depois") Long depois,
                                      Pageable limite);

    @Query("select max(m.id) from MensagemComunidade m where m.comunidade.id = :comunidadeId")
    Long ultimoId(@Param("comunidadeId") Long comunidadeId);

    /** Última mensagem visível de cada comunidade: [comunidadeId, mensagem]. */
    @Query("select m.comunidade.id, m from MensagemComunidade m join fetch m.remetente where m.comunidade.id in :ids "
            + "and m.id = (select max(m2.id) from MensagemComunidade m2 where m2.comunidade.id = m.comunidade.id "
            + "and m2.remetente.status = com.projeto_pi.Projeto.PI.entity.StatusUsuario.APROVADO)")
    List<Object[]> ultimas(@Param("ids") Collection<Long> ids);

    /** Não lidas pelo usuário em cada comunidade de que participa (sem as próprias e sem os avisos). */
    @Query("select m.comunidade.id, count(m) from MensagemComunidade m, MembroComunidade mb "
            + "where mb.usuario.id = :usuarioId and mb.comunidade = m.comunidade "
            + "and m.id > coalesce(mb.ultimaLidaId, 0) and m.remetente.id <> :usuarioId "
            + "and m.categoria <> com.projeto_pi.Projeto.PI.entity.CategoriaPostagem.SISTEMA and " + VISIVEL
            + " group by m.comunidade.id")
    List<Object[]> contarNaoLidas(@Param("usuarioId") Long usuarioId);
}
