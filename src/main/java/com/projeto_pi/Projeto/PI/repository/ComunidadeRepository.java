package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.Comunidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComunidadeRepository extends JpaRepository<Comunidade, Long> {

    List<Comunidade> findAllByOrderByOrdemAscIdAsc();

    Optional<Comunidade> findBySlug(String slug);
}
