package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.Agendamento;
import com.projeto_pi.Projeto.PI.entity.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @Query("select a from Agendamento a where a.solicitante.id = :usuarioId or a.destinatario.id = :usuarioId "
            + "order by a.data, a.horario")
    List<Agendamento> findDoUsuario(@Param("usuarioId") Long usuarioId);

    /** Reuniões de um dos usuários num dia, com os status informados (para checar conflito de horário). */
    @Query("select a from Agendamento a where a.data = :data and a.status in :status and "
            + "(a.solicitante.id in :usuarioIds or a.destinatario.id in :usuarioIds)")
    List<Agendamento> findNoDia(@Param("data") LocalDate data, @Param("status") Collection<StatusAgendamento> status,
                                @Param("usuarioIds") Collection<Long> usuarioIds);
}
