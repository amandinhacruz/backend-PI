package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.AuditoriaAdmin;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaAdminRepository extends JpaRepository<AuditoriaAdmin, Long> {
    List<AuditoriaAdmin> findTop200ByOrderByDataDesc();
}
