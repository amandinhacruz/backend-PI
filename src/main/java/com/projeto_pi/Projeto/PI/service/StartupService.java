package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.entity.Startup;
import com.projeto_pi.Projeto.PI.repository.StartupRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StartupService {

    private final StartupRepository startupRepository;

    public StartupService(StartupRepository startupRepository) {
        this.startupRepository = startupRepository;
    }

    public Startup cadastrar(Startup startup) {
        return startupRepository.save(startup);
    }

    public List<com.projeto_pi.Projeto.PI.dto.StartupFeedDTO> feed(String search, String segment, String stage, String location) {
        return startupRepository.findApproved().stream()
            .map(com.projeto_pi.Projeto.PI.dto.StartupFeedDTO::from)
            .filter(s -> search == null || s.name().toLowerCase(java.util.Locale.ROOT).contains(search.trim().toLowerCase(java.util.Locale.ROOT)))
            .filter(s -> segment == null || segment.isBlank() || s.segment().equals(segment))
            .filter(s -> stage == null || stage.isBlank() || s.stage().equals(stage))
            .filter(s -> location == null || location.isBlank() || s.location().equals(location))
            .toList();
    }

    public com.projeto_pi.Projeto.PI.dto.StartupFeedDTO perfilAprovado(Long id) {
        return startupRepository.findApproved().stream().filter(s -> s.getId().equals(id))
            .map(com.projeto_pi.Projeto.PI.dto.StartupFeedDTO::from).findFirst()
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Startup não encontrada"));
    }

    public List<Startup> listar() {
        return startupRepository.findAll();
    }

    public Startup buscarPorId(Long id) {
        return startupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Startup não encontrada"));
    }

    public Startup atualizar(Long id, Startup startup) {
        Startup existente = buscarPorId(id);

        existente.setNome(startup.getNome());
        existente.setSegmento(startup.getSegmento());
        existente.setEstagio(startup.getEstagio());
        existente.setNecessidades(startup.getNecessidades());
        existente.setPitch(startup.getPitch());
        existente.setCanvas(startup.getCanvas());

        return startupRepository.save(existente);
    }
}
