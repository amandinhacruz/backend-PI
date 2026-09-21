package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.dto.CadastroInvestidorMentorDTO;
import com.projeto_pi.Projeto.PI.dto.CadastroStartupDTO;
import com.projeto_pi.Projeto.PI.entity.*;
import com.projeto_pi.Projeto.PI.repository.InvestidorMentorRepository;
import com.projeto_pi.Projeto.PI.repository.StartupRepository;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public Usuario cadastrarStartup(CadastroStartupDTO dto) {

        verificarEmail(dto.getEmail());

        Startup startup = new Startup();

        startup.setNome(dto.getNome());
        startup.setSegmento(dto.getSegmento());
        startup.setEstagio(dto.getEstagio());
        startup.setNecessidades(dto.getNecessidades());
        startup.setPitch(dto.getPitch());
        startup.setCanvas(dto.getCanvas());

        Startup startupSalva =
                startupRepository.save(startup);

        Usuario usuario = new Usuario();

        usuario.setEmail(dto.getEmail());

        usuario.setSenhaHash(
                passwordEncoder.encode(dto.getSenha())
        );

        usuario.setPapel(Papel.STARTUP);
        usuario.setStatus(StatusUsuario.PENDENTE);
        usuario.setConsentiuEm(true);
        usuario.setStartup(startupSalva);

        return usuarioRepository.save(usuario);
    }

    public Usuario cadastrarInvestidorMentor(
            CadastroInvestidorMentorDTO dto) {

        verificarEmail(dto.getEmail());

        InvestidorMentor investidorMentor =
                new InvestidorMentor();

        investidorMentor.setTipo(dto.getTipo());
        investidorMentor.setAreasInteresse(
                dto.getAreasInteresse()
        );
        investidorMentor.setTicketMin(
                dto.getTicketMin()
        );
        investidorMentor.setTicketMax(
                dto.getTicketMax()
        );
        investidorMentor.setEstagiosPref(
                dto.getEstagiosPref()
        );
        investidorMentor.setDisponibilidade(
                dto.getDisponibilidade()
        );

        InvestidorMentor perfilSalvo =
                investidorMentorRepository.save(
                        investidorMentor
                );

        Usuario usuario = new Usuario();

        usuario.setEmail(dto.getEmail());

        usuario.setSenhaHash(
                passwordEncoder.encode(dto.getSenha())
        );

        usuario.setPapel(Papel.INVESTIDOR);
        usuario.setStatus(StatusUsuario.PENDENTE);
        usuario.setConsentiuEm(true);
        usuario.setInvestidorMentor(perfilSalvo);

        return usuarioRepository.save(usuario);
    }

    private void verificarEmail(String email) {

        if (usuarioRepository.findByEmail(email).isPresent()) {

            throw new RuntimeException(
                    "Email já cadastrado"
            );
        }
    }
}