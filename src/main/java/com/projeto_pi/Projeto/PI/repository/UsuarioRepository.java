package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.matching.StartupResumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Colunas do perfil da startup usadas no matchmaking (sem os arquivos binários). */
    String RESUMO = "select new com.projeto_pi.Projeto.PI.matching.StartupResumo("
            + "u.id, u.versaoPoliticaConsentida, s.id, s.nome, s.segmento, s.estagio, s.city, s.state, "
            + "s.summary, s.problem, s.competitiveAdvantages, s.necessidades, s.pitch, "
            + "s.minimumTicket, s.maximumTicket, s.website, s.presentationVideo, "
            + "s.pitchDeckName, s.businessCanvasName) "
            + "from Usuario u join u.startup s "
            + "where u.status = com.projeto_pi.Projeto.PI.entity.StatusUsuario.APROVADO";

    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findByPapelAndStatus(com.projeto_pi.Projeto.PI.entity.Papel papel,
                                       com.projeto_pi.Projeto.PI.entity.StatusUsuario status);

    @Query(RESUMO + " order by s.id")
    List<StartupResumo> findStartupsAprovadas();

    @Query(RESUMO + " and s.id = :perfilId")
    Optional<StartupResumo> findStartupAprovada(@Param("perfilId") Long perfilId);

    @Query(RESUMO + " and u.id in :usuarioIds")
    List<StartupResumo> findStartupsAprovadasPorUsuario(@Param("usuarioIds") Collection<Long> usuarioIds);
}
