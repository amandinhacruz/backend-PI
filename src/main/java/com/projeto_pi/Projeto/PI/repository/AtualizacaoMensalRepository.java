package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.AtualizacaoMensal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AtualizacaoMensalRepository extends JpaRepository<AtualizacaoMensal, Long> {

    List<AtualizacaoMensal> findByStartup_IdOrderByMesAsc(Long startupUsuarioId);

    Optional<AtualizacaoMensal> findByStartup_IdAndMes(Long startupUsuarioId, LocalDate mes);
}
