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
