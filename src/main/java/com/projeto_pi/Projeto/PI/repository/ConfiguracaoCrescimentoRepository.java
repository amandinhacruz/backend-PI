package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.ConfiguracaoCrescimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracaoCrescimentoRepository extends JpaRepository<ConfiguracaoCrescimento, Long> {

    Optional<ConfiguracaoCrescimento> findByStartup_Id(Long startupUsuarioId);
}
