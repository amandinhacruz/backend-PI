package com.projeto_pi.Projeto.PI;

import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import com.projeto_pi.Projeto.PI.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Regressão das regras de acesso entre portais: o que um papel não pode ver ou fazer, o que
 * usuários pendentes não expõem e cadastros que precisam falhar sem deixar conta criada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegressaoTests {

    @Autowired MockMvc http;
    @Autowired UsuarioRepository users;
    @Autowired JwtService jwt;

    static final String INV = "inv@regressao.test";
    static final String ST = "st@regressao.test";
    static final String PEND = "pend@regressao.test";
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    static final byte[] PDF = "%PDF-1.4 teste".getBytes();

    String token(String email, String papel) { return "Bearer " + jwt.gerarToken(email, papel); }
    String inv() { return token(INV, "INVESTIDOR"); }
    String st() { return token(ST, "STARTUP"); }
    String pend() { return token(PEND, "STARTUP"); }
    Usuario user(String email) { return users.findByEmail(email).orElseThrow(); }

    void aprovar(String email) {
        Usuario u = user(email);
        u.setStatus(StatusUsuario.APROVADO);
        users.saveAndFlush(u);
    }

    void startup(String email, String nome) throws Exception {
        http.perform(multipart("/auth/cadastro/startup")
                .file(new MockMultipartFile("pitchDeck", "pitch.pdf", "application/pdf", PDF))
                .file(new MockMultipartFile("photo", "logo.png", "image/png", PNG))
                .param("email", email).param("password", "Senha@123").param("name", nome)
                .param("segment", "Fintech").param("stage", "Ideação").param("summary", "Resumo")
                .param("minimumTicket", "50000").param("maximumTicket", "100000")
                .param("terms", "true").param("privacy", "true").param("dataProcessing", "true"))
            .andExpect(status().isCreated());
    }

    @BeforeEach
    void dados() throws Exception {
        http.perform(post("/auth/cadastro/investidor").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"%s","password":"Senha@123","name":"Ana","segments":["Fintech"],"stages":["Ideação"],
            "ticketMin":10000,"ticketMax":200000,"terms":true,"privacy":true,"dataProcessing":true}
            """.formatted(INV))).andExpect(status().isCreated());
        aprovar(INV);
        startup(ST, "Aprovada");
        aprovar(ST);
        startup(PEND, "Pendente"); // fica PENDENTE
    }

    /* ------------------------------------------------------- usuário pendente */

    @Test
    void pendingStartupIsInvisibleEverywhere() throws Exception {
        long perfil = user(PEND).getStartup().getId();
        long usuario = user(PEND).getId();
        http.perform(get("/startups/" + perfil + "/perfil").header("Authorization", inv())).andExpect(status().isNotFound());
        http.perform(get("/startups/" + perfil + "/materiais/pitch").header("Authorization", inv())).andExpect(status().isNotFound());
        http.perform(get("/usuarios/" + usuario + "/foto").header("Authorization", inv())).andExpect(status().isNotFound());
        http.perform(get("/startups").header("Authorization", inv()))
            .andExpect(jsonPath("$[*].name", not(hasItem("Pendente"))))
            .andExpect(jsonPath("$[*].name", hasItem("Aprovada")));
        http.perform(get("/matchmaking/investidor").header("Authorization", inv()))
            .andExpect(jsonPath("items[*].name", not(hasItem("Pendente"))));
        http.perform(post("/matchmaking/investidor/startups/" + perfil + "/decisao").header("Authorization", inv())
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"accepted\"}"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void pendingUserCannotUsePortalButSeesOwnPhoto() throws Exception {
        http.perform(get("/perfil").header("Authorization", pend())).andExpect(status().isForbidden());
        http.perform(put("/perfil/startup").header("Authorization", pend()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"segment\":\"Fintech\",\"stage\":\"Ideação\"}"))
            .andExpect(status().isForbidden());
        http.perform(get("/usuarios/" + user(PEND).getId() + "/foto").header("Authorization", pend()))
            .andExpect(status().isOk()).andExpect(content().bytes(PNG));
    }

    /* ------------------------------------------------- separação dos papéis */

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        long aprovado = user(ST).getId();
        for (String rota : new String[]{"/perfil", "/usuarios/" + aprovado + "/foto", "/startups", "/conexoes",
                "/conversas", "/reunioes", "/matchmaking/investidor", "/admin/metricas"}) {
            int codigo = http.perform(get(rota)).andReturn().getResponse().getStatus();
            assertTrue(codigo == 401 || codigo == 403, rota + " respondeu " + codigo);
        }
    }

    @Test
    void eachPortalOnlyReachesItsOwnRoutes() throws Exception {
        http.perform(get("/matchmaking/investidor").header("Authorization", st())).andExpect(status().isForbidden());
        http.perform(get("/matchmaking/startup/pedidos").header("Authorization", inv())).andExpect(status().isForbidden());
        http.perform(get("/admin/metricas").header("Authorization", inv())).andExpect(status().isForbidden());
        http.perform(get("/admin/usuarios").header("Authorization", st())).andExpect(status().isForbidden());
        http.perform(multipart(HttpMethod.PUT, "/perfil/materiais/pitch")
                .file(new MockMultipartFile("file", "p.pdf", "application/pdf", PDF)).header("Authorization", inv()))
            .andExpect(status().is4xxClientError());
        http.perform(put("/perfil/investidor").header("Authorization", st()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"segments\":[\"Fintech\"]}"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void approvedUsersSeeEachOthersPhotos() throws Exception {
        http.perform(get("/usuarios/" + user(ST).getId() + "/foto").header("Authorization", inv()))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    /* ---------------------------------------------- cadastros que devem falhar */

    @Test
    void invalidPhotoAbortsRegistrationWithoutCreatingAccount() throws Exception {
        http.perform(multipart("/auth/cadastro/investidor")
                .file(new MockMultipartFile("photo", "x.png", "image/png", "<svg onload=alert(1)>".getBytes()))
                .param("email", "foto-ruim@regressao.test").param("password", "Senha@123").param("name", "Teste")
                .param("segments", "Fintech")
                .param("terms", "true").param("privacy", "true").param("dataProcessing", "true"))
            .andExpect(status().isBadRequest());
        assertTrue(users.findByEmail("foto-ruim@regressao.test").isEmpty(), "conta não deveria existir");

        http.perform(multipart("/auth/cadastro/startup")
                .file(new MockMultipartFile("photo", "grande.png", "image/png", grande()))
                .param("email", "foto-grande@regressao.test").param("password", "Senha@123").param("name", "X")
                .param("segment", "Fintech").param("stage", "Ideação")
                .param("terms", "true").param("privacy", "true").param("dataProcessing", "true"))
            .andExpect(status().isPayloadTooLarge());
        assertTrue(users.findByEmail("foto-grande@regressao.test").isEmpty(), "conta não deveria existir");
    }

    @Test
    void investorMultipartRegistrationKeepsListsAndRequiresConsent() throws Exception {
        http.perform(multipart("/auth/cadastro/investidor")
                .param("email", "sem-termos@regressao.test").param("password", "Senha@123").param("name", "Teste")
                .param("segments", "Fintech"))
            .andExpect(status().isBadRequest());
        http.perform(multipart("/auth/cadastro/investidor")
                .file(new MockMultipartFile("photo", "ok.png", "image/png", PNG))
                .param("email", "listas@regressao.test").param("password", "Senha@123").param("name", "Listas")
                .param("segments", "Fintech").param("segments", "Agrotech").param("stages", "Escala")
                .param("terms", "true").param("privacy", "true").param("dataProcessing", "true"))
            .andExpect(status().isCreated());
        aprovar("listas@regressao.test");
        http.perform(get("/perfil").header("Authorization", token("listas@regressao.test", "INVESTIDOR")))
            .andExpect(jsonPath("investor.segments", contains("Fintech", "Agrotech")))
            .andExpect(jsonPath("investor.stages", contains("Escala")))
            .andExpect(jsonPath("investor.hasPhoto").value(true));
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitive() throws Exception {
        http.perform(post("/auth/cadastro/investidor").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"INV@Regressao.test","password":"Senha@123","name":"Outra","segments":["Fintech"],
            "terms":true,"privacy":true,"dataProcessing":true}
            """)).andExpect(status().isConflict());
    }

    static byte[] grande() {
        byte[] b = new byte[2 * 1024 * 1024 + 10];
        System.arraycopy(PNG, 0, b, 0, PNG.length);
        return b;
    }
}
