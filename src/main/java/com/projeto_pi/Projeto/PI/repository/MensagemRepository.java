package com.projeto_pi.Projeto.PI.repository;

import com.projeto_pi.Projeto.PI.entity.Mensagem;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    List<Mensagem> findByRemetenteAndDestinatarioOrderByDataEnvioAsc(
            Usuario remetente,
            Usuario destinatario
    );

    List<Mensagem> findByDestinatarioAndRemetenteOrderByDataEnvioAsc(
            Usuario destinatario,
            Usuario remetente
    );
}
