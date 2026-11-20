package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.Startup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StartupRepository extends JpaRepository<Startup, Long> {
    @org.springframework.data.jpa.repository.Query("select s from Usuario u join u.startup s where u.status = com.projeto_pi.Projeto.PI.entity.StatusUsuario.APROVADO order by s.id")
    java.util.List<Startup> findApproved();
}