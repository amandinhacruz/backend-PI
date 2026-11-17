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

        dto.setEmail(RegistrationValidation.credentials(dto.getEmail(), dto.getSenha(), dto.getConfirmPassword()));
        RegistrationValidation.passwordPolicy(dto.getSenha());
        RegistrationValidation.consents(dto.getTerms(), dto.getPrivacy());
        validarConsentimento(dto.getConsentimentoLGPD());
        dto.setCnpj(RegistrationValidation.document(dto.getCnpj(), 14, "CNPJ"));
        verificarEmail(dto.getEmail());

        Startup startup = new Startup();

        startup.setNome(dto.getNome());
        startup.setSegmento(dto.getSegmento());
        startup.setEstagio(dto.getEstagio());
        startup.setNecessidades(dto.getNecessidades());
        startup.setPitch(dto.getPitch());
        startup.setCanvas(dto.getCanvas());
        RegistrationValidation.required(dto.getNome(), "Nome da startup");
        RegistrationValidation.required(dto.getSegmento(), "Segmento");
        RegistrationValidation.required(dto.getEstagio(), "Estágio");
        RegistrationValidation.tickets(dto.getMinimumTicket(), dto.getMaximumTicket());
        startup.setCnpj(dto.getCnpj());
        startup.setWebsite(dto.getWebsite());
        startup.setCity(dto.getCity());
        startup.setState(dto.getState());
        startup.setFoundedYear(dto.getFoundedYear());
        startup.setProfileOwner(dto.getProfileOwner());
        startup.setSummary(dto.getSummary());
        startup.setProblem(dto.getProblem());
        startup.setCompetitiveAdvantages(dto.getCompetitiveAdvantages());
        startup.setMinimumTicket(dto.getMinimumTicket());
        startup.setMaximumTicket(dto.getMaximumTicket());
        startup.setPresentationVideo(dto.getPresentationVideo());


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

        dto.setEmail(RegistrationValidation.credentials(dto.getEmail(), dto.getSenha(), dto.getConfirmPassword()));
        RegistrationValidation.passwordPolicy(dto.getSenha());
        RegistrationValidation.consents(dto.getTerms(), dto.getPrivacy());
        validarConsentimento(dto.getConsentimentoLGPD());
        dto.setCpf(RegistrationValidation.document(dto.getCpf(), 11, "CPF"));
        dto.setCnpj(RegistrationValidation.document(dto.getCnpj(), 14, "CNPJ"));
        if (dto.getCpf() != null && dto.getCnpj() != null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Informe CPF ou CNPJ, não ambos");
        verificarEmail(dto.getEmail());

        InvestidorMentor perfil = new InvestidorMentor();

        RegistrationValidation.required(dto.getAreasInteresse(), "Área de interesse");
        RegistrationValidation.tickets(dto.getTicketMin(), dto.getTicketMax());
        perfil.setTipo(dto.getTipo() == null ? TipoInvestidorMentor.INVESTIDOR : dto.getTipo());
        perfil.setAreasInteresse(dto.getAreasInteresse());
        perfil.setTicketMin(dto.getTicketMin());
        perfil.setTicketMax(dto.getTicketMax());
        perfil.setEstagiosPref(dto.getEstagiosPref());
        perfil.setDisponibilidade(dto.getDisponibilidade());
        perfil.setName(dto.getName());
        perfil.setCpf(dto.getCpf());
        perfil.setCnpj(dto.getCnpj());
        perfil.setLinkedin(dto.getLinkedin());
        perfil.setCity(dto.getCity());
        perfil.setState(dto.getState());
        perfil.setAboutInvestor(dto.getAboutInvestor());
        perfil.setCareer(dto.getCareer());
        perfil.setLookingFor(dto.getLookingFor());


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

        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "E-mail já cadastrado."
            );
        }
    }
}