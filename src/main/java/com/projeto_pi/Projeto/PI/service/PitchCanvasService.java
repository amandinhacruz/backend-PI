package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.dto.AtualizarPitchCanvasDTO;
import com.projeto_pi.Projeto.PI.dto.PitchCanvasResponseDTO;
import com.projeto_pi.Projeto.PI.entity.Papel;
import com.projeto_pi.Projeto.PI.entity.Startup;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.repository.StartupRepository;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PitchCanvasService {

    private final StartupRepository startupRepository;
    private final UsuarioRepository usuarioRepository;

    public PitchCanvasService(
            StartupRepository startupRepository,
            UsuarioRepository usuarioRepository) {

        this.startupRepository = startupRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public PitchCanvasResponseDTO consultar(String email) {

        Startup startup = buscarStartupDoUsuario(email);

        return converterParaDTO(startup);
    }

    @Transactional
    public PitchCanvasResponseDTO atualizar(
            String email,
            AtualizarPitchCanvasDTO dto) {

        if (dto == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Os dados do perfil são obrigatórios."
            );
        }

        if (dto.getPitch() == null && dto.getCanvas() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe o pitch, o Canvas ou ambos para atualizar."
            );
        }

        if (dto.getPitch() != null && dto.getPitch().length() > 2000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O pitch deve ter no máximo 2000 caracteres."
            );
        }

        Startup startup = buscarStartupDoUsuario(email);

        // Atualiza apenas os campos enviados.
        if (dto.getPitch() != null) {
            startup.setPitch(dto.getPitch());
        }

        if (dto.getCanvas() != null) {
            startup.setCanvas(dto.getCanvas());
        }

        Startup startupSalva = startupRepository.save(startup);

        return converterParaDTO(startupSalva);
    }

    private Startup buscarStartupDoUsuario(String email) {

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."
                ));

        if (usuario.getPapel() != Papel.STARTUP) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Somente usuários com perfil de startup podem acessar esta funcionalidade."
            );
        }

        if (usuario.getStartup() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Não existe uma startup vinculada a este usuário."
            );
        }

        return usuario.getStartup();
    }

    private PitchCanvasResponseDTO converterParaDTO(Startup startup) {

        return new PitchCanvasResponseDTO(
                startup.getId(),
                startup.getNome(),
                startup.getPitch(),
                startup.getCanvas()
        );
    }
}
