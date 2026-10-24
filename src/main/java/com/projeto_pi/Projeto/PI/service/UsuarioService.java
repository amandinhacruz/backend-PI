package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.config.PoliticaPrivacidadeConfig;
import com.projeto_pi.Projeto.PI.dto.CadastroStartupDTO;
import com.projeto_pi.Projeto.PI.dto.CadastroInvestidorMentorDTO;
import com.projeto_pi.Projeto.PI.entity.*;
import com.projeto_pi.Projeto.PI.repository.StartupRepository;
import com.projeto_pi.Projeto.PI.repository.InvestidorMentorRepository;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final StartupRepository startupRepository;
    private final InvestidorMentorRepository investidorMentorRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            StartupRepository startupRepository,
            InvestidorMentorRepository investidorMentorRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.startupRepository = startupRepository;
        this.investidorMentorRepository = investidorMentorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario cadastrarStartup(CadastroStartupDTO dto) {

        validarConsentimento(dto.getConsentimentoLGPD());
        verificarEmail(dto.getEmail());

        Startup startup = new Startup();

        startup.setNome(dto.getNome());
        startup.setSegmento(dto.getSegmento());
        startup.setEstagio(dto.getEstagio());
        startup.setNecessidades(dto.getNecessidades());
        startup.setPitch(dto.getPitch());
        startup.setCanvas(dto.getCanvas());

        Startup startupSalva = startupRepository.save(startup);

        Usuario usuario = new Usuario();

        usuario.setEmail(dto.getEmail());
        usuario.setSenhaHash(passwordEncoder.encode(dto.getSenha()));
        usuario.setPapel(Papel.STARTUP);
        usuario.setStatus(StatusUsuario.PENDENTE);
        usuario.setStartup(startupSalva);

        registrarConsentimento(usuario);

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario cadastrarInvestidorMentor(
            CadastroInvestidorMentorDTO dto) {

        validarConsentimento(dto.getConsentimentoLGPD());
        verificarEmail(dto.getEmail());

        InvestidorMentor perfil = new InvestidorMentor();

        perfil.setTipo(dto.getTipo());
        perfil.setAreasInteresse(dto.getAreasInteresse());
        perfil.setTicketMin(dto.getTicketMin());
        perfil.setTicketMax(dto.getTicketMax());
        perfil.setEstagiosPref(dto.getEstagiosPref());
        perfil.setDisponibilidade(dto.getDisponibilidade());

        InvestidorMentor perfilSalvo =
                investidorMentorRepository.save(perfil);

        Usuario usuario = new Usuario();

        usuario.setEmail(dto.getEmail());
        usuario.setSenhaHash(passwordEncoder.encode(dto.getSenha()));
        usuario.setPapel(Papel.INVESTIDOR);
        usuario.setStatus(StatusUsuario.PENDENTE);
        usuario.setInvestidorMentor(perfilSalvo);

        registrarConsentimento(usuario);

        return usuarioRepository.save(usuario);
    }

    private void validarConsentimento(Boolean consentimento) {

        if (!Boolean.TRUE.equals(consentimento)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Para concluir o cadastro, é necessário aceitar "
                            + "a Política de Privacidade e Uso."
            );
        }
    }

    private void registrarConsentimento(Usuario usuario) {

        usuario.setConsentiuEm(true);
        usuario.setDataConsentimento(LocalDateTime.now());
        usuario.setVersaoPoliticaConsentida(
                PoliticaPrivacidadeConfig.VERSAO_ATUAL
        );
    }

    private void verificarEmail(String email) {

        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "E-mail já cadastrado."
            );
        }
    }
}