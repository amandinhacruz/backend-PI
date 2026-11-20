package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.AnaliseAfinidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnaliseAfinidadeRepository extends JpaRepository<AnaliseAfinidade, Long> {

    List<AnaliseAfinidade> findByInvestidorPerfilId(Long investidorPerfilId);

    Optional<AnaliseAfinidade> findByInvestidorPerfilIdAndStartupPerfilId(Long investidorPerfilId, Long startupPerfilId);
}
