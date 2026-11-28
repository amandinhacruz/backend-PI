package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.dto.PerfilDTOs.InvestorProfile;
import com.projeto_pi.Projeto.PI.dto.PerfilDTOs.InvestorUpdate;
import com.projeto_pi.Projeto.PI.dto.PerfilDTOs.OwnProfile;
import com.projeto_pi.Projeto.PI.dto.PerfilDTOs.StartupProfile;
import com.projeto_pi.Projeto.PI.dto.PerfilDTOs.StartupUpdate;
import com.projeto_pi.Projeto.PI.entity.FotoPerfil;
import com.projeto_pi.Projeto.PI.entity.InvestidorMentor;
import com.projeto_pi.Projeto.PI.entity.Papel;
import com.projeto_pi.Projeto.PI.entity.Startup;
import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.matching.Criterios;
import com.projeto_pi.Projeto.PI.matching.StartupResumo;
import com.projeto_pi.Projeto.PI.repository.FotoPerfilRepository;
import com.projeto_pi.Projeto.PI.repository.InvestidorMentorRepository;
import com.projeto_pi.Projeto.PI.repository.StartupRepository;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * Perfis: o próprio usuário vê e edita o seu; investidores e startups aprovados veem o perfil
 * público das startups. A edição só altera o perfil de quem está logado (a rota antiga
 * PUT /startups/{id} permitia a qualquer startup alterar o perfil de outra).
 */
@Service
public class PerfilService {

    static final int NOME_MAXIMO = 120;
    static final int TEXTO_MAXIMO = 2000;
    static final int CURTO_MAXIMO = 300;

    private final UsuarioRepository usuarios;
    private final StartupRepository startups;
    private final InvestidorMentorRepository investidores;
    private final FotoPerfilRepository fotos;

    public PerfilService(UsuarioRepository usuarios, StartupRepository startups,
                         InvestidorMentorRepository investidores, FotoPerfilRepository fotos) {
        this.usuarios = usuarios;
        this.startups = startups;
        this.investidores = investidores;
        this.fotos = fotos;
    }

    /* ================================================================== leitura */

    @Transactional(readOnly = true)
    public OwnProfile proprio(String email) {
        Usuario usuario = participante(email);
        boolean temFoto = fotos.existsByUsuarioId(usuario.getId());
        if (usuario.getPapel() == Papel.STARTUP) {
            return new OwnProfile("STARTUP", startup(usuario, usuario.getStartup(), temFoto, true), null);
        }
        return new OwnProfile("INVESTIDOR", null, investidor(usuario, temFoto, true));
    }

    /** Perfil público de uma startup aprovada (sem CNPJ, responsável e e-mail). */
    @Transactional(readOnly = true)
    public StartupProfile startupPublica(Long startupPerfilId) {
        StartupResumo resumo = usuarios.findStartupAprovada(startupPerfilId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Startup não encontrada"));
        Usuario usuario = usuarios.findById(resumo.usuarioId()).orElseThrow();
        return startup(usuario, usuario.getStartup(), fotos.existsByUsuarioId(usuario.getId()), false);
    }

    /** Perfil público de um investidor aprovado, visto por outro usuário aprovado (sem CPF, CNPJ e e-mail). */
    @Transactional(readOnly = true)
    public InvestorProfile investidorPublico(String email, Long usuarioId) {
        Usuario leitor = usuarios.findByEmailIgnoreCase(email == null ? "" : email)
                .orElseThrow(() -> erro(HttpStatus.FORBIDDEN, "Acesso não permitido"));
        if (leitor.getStatus() != StatusUsuario.APROVADO) {
            throw erro(HttpStatus.FORBIDDEN, "Acesso não permitido");
        }
        Usuario investidor = usuarios.findById(usuarioId)
                .filter(u -> u.getPapel() == Papel.INVESTIDOR && u.getStatus() == StatusUsuario.APROVADO
                        && u.getInvestidorMentor() != null)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Investidor não encontrado"));
        return investidor(investidor, fotos.existsByUsuarioId(investidor.getId()), false);
    }

    /** Foto de um usuário aprovado (ou do próprio usuário, mesmo pendente). */
    @Transactional(readOnly = true)
    public FotoPerfil foto(String email, Long usuarioId) {
        Usuario solicitante = usuarios.findByEmailIgnoreCase(email == null ? "" : email)
                .orElseThrow(() -> erro(HttpStatus.FORBIDDEN, "Acesso não permitido"));
        Usuario dono = usuarios.findById(usuarioId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Foto não encontrada"));
        boolean proprio = dono.getId().equals(solicitante.getId());
        if (!proprio && (dono.getStatus() != StatusUsuario.APROVADO || solicitante.getStatus() != StatusUsuario.APROVADO)) {
            throw erro(HttpStatus.NOT_FOUND, "Foto não encontrada");
        }
        return fotos.findByUsuarioId(usuarioId).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Foto não encontrada"));
    }

    /* ================================================================== edição */

    @Transactional
    public StartupProfile editarStartup(String email, StartupUpdate dados) {
        Usuario usuario = participante(email);
        if (usuario.getPapel() != Papel.STARTUP || usuario.getStartup() == null) {
            throw erro(HttpStatus.FORBIDDEN, "Acesso não permitido");
        }
        if (dados == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe os dados do perfil");
        }
        Startup s = usuario.getStartup();
        s.setNome(obrigatorio(dados.name(), "Nome da startup", NOME_MAXIMO));
        s.setSegmento(obrigatorio(dados.segment(), "Segmento", NOME_MAXIMO));
        s.setEstagio(obrigatorio(dados.stage(), "Estágio", NOME_MAXIMO));
        RegistrationValidation.tickets(dados.minimumTicket(), dados.maximumTicket());
        s.setMinimumTicket(dados.minimumTicket());
        s.setMaximumTicket(dados.maximumTicket());
        s.setWebsite(link(dados.website(), "Site"));
        s.setPresentationVideo(link(dados.presentationVideo(), "Link de apresentação"));
        s.setCity(opcional(dados.city(), CURTO_MAXIMO));
        s.setState(opcional(dados.state(), CURTO_MAXIMO));
        s.setFoundedYear(ano(dados.foundedYear()));
        s.setProfileOwner(opcional(dados.profileOwner(), NOME_MAXIMO));
        s.setSummary(opcional(dados.summary(), TEXTO_MAXIMO));
        s.setProblem(opcional(dados.problem(), TEXTO_MAXIMO));
        s.setCompetitiveAdvantages(opcional(dados.competitiveAdvantages(), TEXTO_MAXIMO));
        startups.save(s);
        return startup(usuario, s, fotos.existsByUsuarioId(usuario.getId()), true);
    }

    @Transactional
    public InvestorProfile editarInvestidor(String email, InvestorUpdate dados) {
        Usuario usuario = participante(email);
        InvestidorMentor perfil = usuario.getInvestidorMentor();
        if (usuario.getPapel() != Papel.INVESTIDOR || perfil == null) {
            throw erro(HttpStatus.FORBIDDEN, "Acesso não permitido");
        }
        if (dados == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe os dados do perfil");
        }
        List<String> segmentos = limpar(dados.segments());
        if (segmentos.isEmpty()) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe pelo menos um segmento de interesse");
        }
        RegistrationValidation.tickets(dados.ticketMin(), dados.ticketMax());
        perfil.setName(obrigatorio(dados.name(), "Nome", NOME_MAXIMO));
        perfil.setAreasInteresse(String.join("; ", segmentos));
        List<String> estagios = limpar(dados.stages());
        perfil.setEstagiosPref(estagios.isEmpty() ? null : String.join("; ", estagios));
        perfil.setTicketMin(dados.ticketMin());
        perfil.setTicketMax(dados.ticketMax());
        perfil.setLinkedin(link(dados.linkedin(), "LinkedIn"));
        perfil.setCity(opcional(dados.city(), CURTO_MAXIMO));
        perfil.setState(opcional(dados.state(), CURTO_MAXIMO));
        perfil.setAboutInvestor(opcional(dados.aboutInvestor(), TEXTO_MAXIMO));
        perfil.setCareer(opcional(dados.career(), TEXTO_MAXIMO));
        perfil.setLookingFor(opcional(dados.lookingFor(), TEXTO_MAXIMO));
        investidores.save(perfil);
        return investidor(usuario, fotos.existsByUsuarioId(usuario.getId()), true);
    }

    @Transactional
    public void salvarFoto(String email, MultipartFile arquivo) {
        Usuario usuario = participante(email);
        salvarFoto(usuario.getId(), arquivo);
    }

    /** Usado também no cadastro, antes da aprovação. */
    @Transactional
    public void salvarFoto(Long usuarioId, MultipartFile arquivo) {
        Arquivos.Foto foto = Arquivos.foto(arquivo);
        FotoPerfil registro = fotos.findByUsuarioId(usuarioId).orElseGet(() -> new FotoPerfil(usuarioId));
        registro.registrar(foto.dados(), foto.tipo());
        fotos.save(registro);
    }

    @Transactional
    public void removerFoto(String email) {
        fotos.deleteByUsuarioId(participante(email).getId());
    }

    /** Envia ou substitui o pitch deck ("pitch") ou o canvas ("canvas") da própria startup. */
    @Transactional
    public StartupProfile salvarMaterial(String email, String tipo, MultipartFile arquivo) {
        Usuario usuario = startupLogada(email);
        if (arquivo == null || arquivo.isEmpty()) {
            throw erro(HttpStatus.BAD_REQUEST, "Envie um arquivo");
        }
        Arquivos.material(arquivo);
        Startup s = usuario.getStartup();
        byte[] dados = Arquivos.bytes(arquivo);
        switch (tipoMaterial(tipo)) {
            case "pitch" -> { s.setPitchDeckData(dados); s.setPitchDeckName(arquivo.getOriginalFilename()); }
            default -> { s.setBusinessCanvasData(dados); s.setBusinessCanvasName(arquivo.getOriginalFilename()); }
        }
        startups.save(s);
        return startup(usuario, s, fotos.existsByUsuarioId(usuario.getId()), true);
    }

    @Transactional
    public StartupProfile removerMaterial(String email, String tipo) {
        Usuario usuario = startupLogada(email);
        Startup s = usuario.getStartup();
        switch (tipoMaterial(tipo)) {
            case "pitch" -> { s.setPitchDeckData(null); s.setPitchDeckName(null); }
            default -> { s.setBusinessCanvasData(null); s.setBusinessCanvasName(null); }
        }
        startups.save(s);
        return startup(usuario, s, fotos.existsByUsuarioId(usuario.getId()), true);
    }

    /* ==================================================================== apoio */

    private static StartupProfile startup(Usuario u, Startup s, boolean temFoto, boolean proprio) {
        String cidade = texto(s.getCity());
        String uf = texto(s.getState());
        return new StartupProfile(
                s.getId().toString(), u.getId(), texto(s.getNome()), texto(s.getSegmento()), texto(s.getEstagio()),
                cidade, uf, cidade + (!cidade.isEmpty() && !uf.isEmpty() ? "/" : "") + uf,
                texto(s.getFoundedYear()), texto(s.getWebsite()), texto(s.getSummary()), texto(s.getProblem()),
                texto(s.getCompetitiveAdvantages()), s.getMinimumTicket(), s.getMaximumTicket(),
                texto(s.getPresentationVideo()),
                s.getPitchDeckData() != null, s.getPitchDeckData() != null ? s.getPitchDeckName() : null,
                s.getBusinessCanvasData() != null, s.getBusinessCanvasData() != null ? s.getBusinessCanvasName() : null,
                temFoto,
                proprio ? s.getCnpj() : null, proprio ? s.getProfileOwner() : null, proprio ? u.getEmail() : null);
    }

    private static InvestorProfile investidor(Usuario u, boolean temFoto, boolean proprio) {
        InvestidorMentor p = u.getInvestidorMentor();
        return new InvestorProfile(
                p.getId().toString(), u.getId(), p.getTipo() == null ? "INVESTIDOR" : p.getTipo().name(),
                texto(p.getName()), texto(p.getCity()), texto(p.getState()), texto(p.getLinkedin()),
                Criterios.lista(p.getAreasInteresse()), Criterios.lista(p.getEstagiosPref()),
                p.getTicketMin(), p.getTicketMax(), texto(p.getAboutInvestor()), texto(p.getCareer()),
                texto(p.getLookingFor()), temFoto,
                proprio ? p.getCpf() : null, proprio ? p.getCnpj() : null, proprio ? u.getEmail() : null);
    }

    private static String tipoMaterial(String tipo) {
        if ("pitch".equals(tipo) || "canvas".equals(tipo)) {
            return tipo;
        }
        throw erro(HttpStatus.NOT_FOUND, "Material não encontrado");
    }

    private static List<String> limpar(List<String> itens) {
        if (itens == null) {
            return List.of();
        }
        return itens.stream().filter(i -> i != null && !i.isBlank()).map(String::strip).distinct().toList();
    }

    private static String obrigatorio(String valor, String campo, int maximo) {
        String limpo = opcional(valor, maximo);
        if (limpo == null) {
            throw erro(HttpStatus.BAD_REQUEST, campo + " é obrigatório");
        }
        return limpo;
    }

    private static String opcional(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpo = valor.strip();
        if (limpo.length() > maximo) {
            throw erro(HttpStatus.BAD_REQUEST, "Texto acima do limite de " + maximo + " caracteres");
        }
        return limpo;
    }

    private static String link(String valor, String campo) {
        String limpo = opcional(valor, CURTO_MAXIMO);
        if (limpo != null && !limpo.matches("(?i)^https?://\\S+$")) {
            throw erro(HttpStatus.BAD_REQUEST, campo + " deve começar com http:// ou https://");
        }
        return limpo;
    }

    private static String ano(String valor) {
        String limpo = opcional(valor, 4);
        if (limpo != null && !limpo.matches("^(19|20)\\d{2}$")) {
            throw erro(HttpStatus.BAD_REQUEST, "Ano de fundação inválido");
        }
        return limpo;
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private Usuario startupLogada(String email) {
        Usuario usuario = participante(email);
        if (usuario.getPapel() != Papel.STARTUP || usuario.getStartup() == null) {
            throw erro(HttpStatus.FORBIDDEN, "Acesso não permitido");
        }
        return usuario;
    }

    private Usuario participante(String email) {
        Optional<Usuario> encontrado = usuarios.findByEmailIgnoreCase(email == null ? "" : email);
        Usuario usuario = encontrado.orElseThrow(() -> erro(HttpStatus.FORBIDDEN, "Acesso não permitido"));
        if (usuario.getStatus() != StatusUsuario.APROVADO
                || (usuario.getPapel() != Papel.INVESTIDOR && usuario.getPapel() != Papel.STARTUP)) {
            throw erro(HttpStatus.FORBIDDEN, "Acesso não permitido");
        }
        if (usuario.getPapel() == Papel.INVESTIDOR && usuario.getInvestidorMentor() == null) {
            throw erro(HttpStatus.CONFLICT, "Complete o seu perfil de investidor");
        }
        return usuario;
    }

    private static ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
