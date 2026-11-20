package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.dto.StartupFeedDTO;
import com.projeto_pi.Projeto.PI.entity.Startup;
import com.projeto_pi.Projeto.PI.matching.StartupResumo;
import com.projeto_pi.Projeto.PI.repository.FotoPerfilRepository;
import com.projeto_pi.Projeto.PI.repository.StartupRepository;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Feed e materiais das startups aprovadas. As rotas antigas de escrita (POST /startups e
 * PUT /startups/{id}) foram removidas: não verificavam o dono do perfil. A edição agora é
 * feita em PUT /perfil/startup (PerfilService).
 */
@Service
public class StartupService {

    private final StartupRepository startupRepository;
    private final UsuarioRepository usuarios;
    private final FotoPerfilRepository fotos;

    public StartupService(StartupRepository startupRepository, UsuarioRepository usuarios, FotoPerfilRepository fotos) {
        this.startupRepository = startupRepository;
        this.usuarios = usuarios;
        this.fotos = fotos;
    }

    @Transactional(readOnly = true)
    public List<StartupFeedDTO> feed(String search, String segment, String stage, String location) {
        List<StartupResumo> aprovadas = usuarios.findStartupsAprovadas();
        Set<Long> comFoto = aprovadas.isEmpty() ? Set.of()
                : fotos.comFoto(aprovadas.stream().map(StartupResumo::usuarioId).toList());
        return aprovadas.stream()
            .map(s -> StartupFeedDTO.from(s, comFoto.contains(s.usuarioId())))
            .filter(s -> search == null || s.name().toLowerCase(Locale.ROOT).contains(search.trim().toLowerCase(Locale.ROOT)))
            .filter(s -> segment == null || segment.isBlank() || s.segment().equals(segment))
            .filter(s -> stage == null || stage.isBlank() || s.stage().equals(stage))
            .filter(s -> location == null || location.isBlank() || s.location().equals(location))
            .toList();
    }

    @Transactional(readOnly = true)
    public StartupFeedDTO perfilAprovado(Long id) {
        StartupResumo s = usuarios.findStartupAprovada(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrada"));
        return StartupFeedDTO.from(s, fotos.existsByUsuarioId(s.usuarioId()));
    }

    public Startup buscarPorId(Long id) {
        return startupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrada"));
    }
}
