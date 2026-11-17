package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.Agendamento;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    List<Agendamento> findBySolicitanteOrDestinatarioOrderByDataAscHorarioAsc(
            Usuario solicitante,
            Usuario destinatario
    );
}
