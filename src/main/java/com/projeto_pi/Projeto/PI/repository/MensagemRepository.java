package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.Mensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    List<Mensagem> findByConexao_IdOrderByDataEnvioAscIdAsc(Long conexaoId);

    /** Última mensagem de cada conversa: [conexaoId, mensagem]. */
    @Query("select m.conexao.id, m from Mensagem m where m.conexao.id in :conexaoIds and m.id = "
            + "(select max(m2.id) from Mensagem m2 where m2.conexao.id = m.conexao.id)")
    List<Object[]> findUltimas(@Param("conexaoIds") Collection<Long> conexaoIds);

    /** Não lidas por conversa: [conexaoId, quantidade]. */
    @Query("select m.conexao.id, count(m) from Mensagem m where m.conexao.id in :conexaoIds "
            + "and m.destinatario.id = :usuarioId and m.lidaEm is null group by m.conexao.id")
    List<Object[]> contarNaoLidas(@Param("conexaoIds") Collection<Long> conexaoIds,
                                  @Param("usuarioId") Long usuarioId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Mensagem m set m.lidaEm = :agora where m.conexao.id = :conexaoId "
            + "and m.destinatario.id = :usuarioId and m.lidaEm is null")
    int marcarComoLidas(@Param("conexaoId") Long conexaoId, @Param("usuarioId") Long usuarioId,
                        @Param("agora") LocalDateTime agora);
}
