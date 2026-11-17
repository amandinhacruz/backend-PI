package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.dto.EnviarMensagemDTO;
import com.projeto_pi.Projeto.PI.dto.MensagemResponseDTO;
import com.projeto_pi.Projeto.PI.entity.Mensagem;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.repository.MensagemRepository;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MensagemService {

    private final MensagemRepository mensagemRepository;
    private final UsuarioRepository usuarioRepository;

    public MensagemService(
            MensagemRepository mensagemRepository,
            UsuarioRepository usuarioRepository) {

        this.mensagemRepository = mensagemRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public MensagemResponseDTO enviar(EnviarMensagemDTO dto) {

        Usuario remetente = usuarioRepository.findById(dto.getRemetenteId())
                .orElseThrow(() ->
                        new RuntimeException("Remetente não encontrado"));

        Usuario destinatario = usuarioRepository.findById(dto.getDestinatarioId())
                .orElseThrow(() ->
                        new RuntimeException("Destinatário não encontrado"));

        if (dto.getConteudo() == null || dto.getConteudo().isBlank()) {
            throw new RuntimeException("A mensagem não pode estar vazia");
        }

        Mensagem mensagem = new Mensagem();

        mensagem.setRemetente(remetente);
        mensagem.setDestinatario(destinatario);
        mensagem.setConteudo(dto.getConteudo());
        mensagem.setDataEnvio(LocalDateTime.now());

        Mensagem salva = mensagemRepository.save(mensagem);

        return converterParaDTO(salva);
    }

    public List<MensagemResponseDTO> buscarConversa(
            Long usuarioId,
            Long outroUsuarioId) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado"));

        Usuario outroUsuario = usuarioRepository.findById(outroUsuarioId)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado"));

        List<Mensagem> enviadas =
                mensagemRepository
                        .findByRemetenteAndDestinatarioOrderByDataEnvioAsc(
                                usuario,
                                outroUsuario
                        );

        List<Mensagem> recebidas =
                mensagemRepository
                        .findByDestinatarioAndRemetenteOrderByDataEnvioAsc(
                                usuario,
                                outroUsuario
                        );

        List<Mensagem> conversa = new ArrayList<>();

        conversa.addAll(enviadas);
        conversa.addAll(recebidas);

        conversa.sort((a, b) ->
                a.getDataEnvio().compareTo(b.getDataEnvio())
        );

        return conversa.stream()
                .map(this::converterParaDTO)
                .toList();
    }

    private MensagemResponseDTO converterParaDTO(Mensagem mensagem) {

        return new MensagemResponseDTO(
                mensagem.getId(),
                mensagem.getRemetente().getId(),
                mensagem.getDestinatario().getId(),
                mensagem.getConteudo(),
                mensagem.getDataEnvio()
        );
    }
}
