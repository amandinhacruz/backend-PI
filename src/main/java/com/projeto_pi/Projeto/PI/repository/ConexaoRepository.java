package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.Conexao;
import com.projeto_pi.Projeto.PI.entity.StatusConexao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ConexaoRepository extends JpaRepository<Conexao, Long> {

    /** Decisões já tomadas pelo investidor (alimentam o aprendizado). */
    List<Conexao> findByInvestidor_Id(Long investidorId);

    /** Startups (id de usuário) que o investidor já solicitou ou passou: saem da fila de matching. */
    @Query("select c.startup.id from Conexao c where c.investidor.id = :investidorId")
    Set<Long> findStartupsJaDecididas(@Param("investidorId") Long investidorId);

    @Query("select c from Conexao c where c.investidor.id = :investidorId and c.startup.id = :startupId")
    Optional<Conexao> findPar(@Param("investidorId") Long investidorId, @Param("startupId") Long startupId);

    List<Conexao> findByStartup_IdAndStatusOrderByCriadaEmDesc(Long startupId, StatusConexao status);

    List<Conexao> findByInvestidor_IdAndStatusOrderByRespondidaEmDesc(Long investidorId, StatusConexao status);

    List<Conexao> findByStartup_IdAndStatusOrderByRespondidaEmDesc(Long startupId, StatusConexao status);

    long countByStatus(StatusConexao status);

    long countByStatusIn(Collection<StatusConexao> status);

    long countByStatusAndOrigemAnalise(StatusConexao status, String origemAnalise);
}
