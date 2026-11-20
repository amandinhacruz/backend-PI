package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.DossieMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DossieMatchRepository extends JpaRepository<DossieMatch, Long> {

    Optional<DossieMatch> findByInvestidorPerfilIdAndStartupPerfilId(Long investidorPerfilId, Long startupPerfilId);
}
