package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.dto.AgendamentoDTO;
import com.projeto_pi.Projeto.PI.dto.AgendamentoResponseDTO;
import com.projeto_pi.Projeto.PI.entity.Agendamento;
import com.projeto_pi.Projeto.PI.entity.StatusAgendamento;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.repository.AgendamentoRepository;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;

    public AgendamentoService(
            AgendamentoRepository agendamentoRepository,
            UsuarioRepository usuarioRepository) {

        this.agendamentoRepository = agendamentoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public AgendamentoResponseDTO criar(AgendamentoDTO dto) {

        Usuario solicitante = usuarioRepository.findById(dto.getSolicitanteId())
                .orElseThrow(() ->
                        new RuntimeException("Solicitante não encontrado"));

        Usuario destinatario = usuarioRepository.findById(dto.getDestinatarioId())
                .orElseThrow(() ->
                        new RuntimeException("Destinatário não encontrado"));

        if (dto.getData() == null) {
            throw new RuntimeException("A data é obrigatória");
        }

        if (dto.getHorario() == null) {
            throw new RuntimeException("O horário é obrigatório");
        }

        if (dto.getData().isBefore(LocalDate.now())) {
            throw new RuntimeException(
                    "Não é possível criar um agendamento para uma data passada"
            );
        }

        Agendamento agendamento = new Agendamento();

        agendamento.setSolicitante(solicitante);
        agendamento.setDestinatario(destinatario);
        agendamento.setData(dto.getData());
        agendamento.setHorario(dto.getHorario());
        agendamento.setAssunto(dto.getAssunto());
        agendamento.setStatus(StatusAgendamento.PENDENTE);

        Agendamento salvo = agendamentoRepository.save(agendamento);

        return converterParaDTO(salvo);
    }

    public AgendamentoResponseDTO aceitar(Long id) {

        Agendamento agendamento = buscar(id);

        agendamento.setStatus(StatusAgendamento.ACEITO);

        return converterParaDTO(
                agendamentoRepository.save(agendamento)
        );
    }

    public AgendamentoResponseDTO rejeitar(Long id) {

        Agendamento agendamento = buscar(id);

        agendamento.setStatus(StatusAgendamento.REJEITADO);

        return converterParaDTO(
                agendamentoRepository.save(agendamento)
        );
    }

    public AgendamentoResponseDTO cancelar(Long id) {

        Agendamento agendamento = buscar(id);

        agendamento.setStatus(StatusAgendamento.CANCELADO);

        return converterParaDTO(
                agendamentoRepository.save(agendamento)
        );
    }

    public List<AgendamentoResponseDTO> listarPorUsuario(Long usuarioId) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado"));

        return agendamentoRepository
                .findBySolicitanteOrDestinatarioOrderByDataAscHorarioAsc(
                        usuario,
                        usuario
                )
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    private Agendamento buscar(Long id) {

        return agendamentoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Agendamento não encontrado"));
    }

    private AgendamentoResponseDTO converterParaDTO(
            Agendamento agendamento) {

        return new AgendamentoResponseDTO(
                agendamento.getId(),
                agendamento.getSolicitante().getId(),
                agendamento.getDestinatario().getId(),
                agendamento.getData(),
                agendamento.getHorario(),
                agendamento.getAssunto(),
                agendamento.getStatus()
        );
    }
}
