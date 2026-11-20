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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Perfis: visualização pública da startup, edição do próprio perfil, foto e materiais. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerfilTests {

    @Autowired MockMvc http;
    @Autowired UsuarioRepository users;
    @Autowired JwtService jwt;

    static final String INV = "inv@perfil.test";
    static final String ST = "st@perfil.test";
    static final String OUTRA = "outra@perfil.test";
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    static final byte[] PDF = "%PDF-1.4 teste".getBytes();

    String inv() { return "Bearer " + jwt.gerarToken(INV, "INVESTIDOR"); }
    String st() { return "Bearer " + jwt.gerarToken(ST, "STARTUP"); }
    String outra() { return "Bearer " + jwt.gerarToken(OUTRA, "STARTUP"); }

    void aprovar(String email) {
        Usuario u = users.findByEmail(email).orElseThrow();
        u.setStatus(StatusUsuario.APROVADO);
        users.saveAndFlush(u);
    }

    long perfilDe(String email) { return users.findByEmail(email).orElseThrow().getStartup().getId(); }
    long usuario(String email) { return users.findByEmail(email).orElseThrow().getId(); }

    @BeforeEach
    void cadastros() throws Exception {
        http.perform(multipart("/auth/cadastro/investidor")
                .file(new MockMultipartFile("photo", "eu.png", "image/png", PNG))
                .param("email", INV).param("password", "Senha@123").param("name", "Ana Anjo")
                .param("segments", "Healthtech").param("segments", "Fintech")
                .param("stages", "Ideação").param("ticketMin", "50000").param("ticketMax", "200000")
                .param("terms", "true").param("privacy", "true").param("dataProcessing", "true"))
            .andExpect(status().isCreated());
        aprovar(INV);
        http.perform(multipart("/auth/cadastro/startup")
                .file(new MockMultipartFile("pitchDeck", "pitch.pdf", "application/pdf", PDF))
                .file(new MockMultipartFile("photo", "logo.png", "image/png", PNG))
                .param("email", ST).param("password", "Senha@123").param("name", "TeleSertão")
                .param("segment", "Healthtech").param("stage", "Ideação").param("summary", "Telemedicina")
                .param("presentationVideo", "https://video.example/pitch")
                .param("minimumTicket", "60000").param("maximumTicket", "150000")
                .param("terms", "true").param("privacy", "true").param("dataProcessing", "true"))
            .andExpect(status().isCreated());
        aprovar(ST);
        http.perform(post("/auth/cadastro/startup").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"%s","password":"Senha@123","name":"Outra","segment":"Healthtech","stage":"Ideação",
            "summary":"Outra","minimumTicket":60000,"maximumTicket":150000,"terms":true,"privacy":true,"dataProcessing":true}
            """.formatted(OUTRA))).andExpect(status().isCreated());
        aprovar(OUTRA);
    }

    @Test
    void investorSeesPublicStartupProfileWithoutPrivateFields() throws Exception {
        http.perform(get("/startups/" + perfilDe(ST) + "/perfil").header("Authorization", inv()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("name").value("TeleSertão"))
            .andExpect(jsonPath("hasPitchDeck").value(true))
            .andExpect(jsonPath("hasCanvas").value(false))
            .andExpect(jsonPath("hasPhoto").value(true))
            .andExpect(jsonPath("presentationVideo").value("https://video.example/pitch"))
            .andExpect(jsonPath("email").value(nullValue()))
            .andExpect(jsonPath("cnpj").value(nullValue()));
    }

    @Test
    void ownProfileIsReturnedForEachRole() throws Exception {
        http.perform(get("/perfil").header("Authorization", st()))
            .andExpect(jsonPath("role").value("STARTUP"))
            .andExpect(jsonPath("startup.email").value(ST))
            .andExpect(jsonPath("investor").value(nullValue()));
        http.perform(get("/perfil").header("Authorization", inv()))
            .andExpect(jsonPath("role").value("INVESTIDOR"))
            .andExpect(jsonPath("investor.segments", contains("Healthtech", "Fintech")))
            .andExpect(jsonPath("investor.hasPhoto").value(true));
    }

    @Test
    void startupEditsOnlyItsOwnProfile() throws Exception {
        http.perform(put("/perfil/startup").header("Authorization", st()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"TeleSertão Saúde","segment":"Healthtech","stage":"Operação","summary":"Novo resumo",
                    "minimumTicket":100000,"maximumTicket":300000,"website":"https://tele.example"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("name").value("TeleSertão Saúde"))
            .andExpect(jsonPath("stage").value("Operação"));
        // a outra startup editando não altera a primeira
        http.perform(put("/perfil/startup").header("Authorization", outra()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Outra 2\",\"segment\":\"Fintech\",\"stage\":\"Ideação\"}"))
            .andExpect(status().isOk());
        http.perform(get("/startups/" + perfilDe(ST) + "/perfil").header("Authorization", inv()))
            .andExpect(jsonPath("name").value("TeleSertão Saúde"));
        // investidor não usa a rota da startup
        http.perform(put("/perfil/startup").header("Authorization", inv()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"segment\":\"Fintech\",\"stage\":\"Ideação\"}"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void profileEditValidatesRequiredFieldsTicketsAndLinks() throws Exception {
        http.perform(put("/perfil/startup").header("Authorization", st()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"segment\":\"Healthtech\",\"stage\":\"Ideação\"}"))
            .andExpect(status().isBadRequest());
        http.perform(put("/perfil/startup").header("Authorization", st()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"segment\":\"Healthtech\",\"stage\":\"Ideação\",\"minimumTicket\":500,\"maximumTicket\":100}"))
            .andExpect(status().isBadRequest());
        http.perform(put("/perfil/investidor").header("Authorization", inv()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ana\",\"segments\":[\"Fintech\"],\"stages\":[\"Ideação\"],\"linkedin\":\"javascript:alert(1)\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void investorEditsSegmentsAndStages() throws Exception {
        http.perform(put("/perfil/investidor").header("Authorization", inv()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Ana Anjo","segments":["Agrotech"],"stages":["Tração","Escala"],
                    "ticketMin":10000,"ticketMax":90000,"aboutInvestor":"Investe no Nordeste"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("segments", contains("Agrotech")))
            .andExpect(jsonPath("stages", contains("Tração", "Escala")))
            .andExpect(jsonPath("aboutInvestor").value("Investe no Nordeste"));
    }

    @Test
    void photoIsValidatedServedAndRemovable() throws Exception {
        long id = usuario(OUTRA);
        http.perform(get("/usuarios/" + id + "/foto").header("Authorization", inv())).andExpect(status().isNotFound());
        http.perform(multipart(HttpMethod.PUT, "/perfil/foto").file(
                new MockMultipartFile("photo", "x.png", "image/png", "<svg onload=alert(1)>".getBytes()))
                .header("Authorization", outra()))
            .andExpect(status().isBadRequest());
        http.perform(multipart(HttpMethod.PUT, "/perfil/foto").file(new MockMultipartFile("photo", "x.png", "image/png", PNG))
                .header("Authorization", outra()))
            .andExpect(status().isNoContent());
        http.perform(get("/usuarios/" + id + "/foto").header("Authorization", inv()))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(content().bytes(PNG));
        http.perform(delete("/perfil/foto").header("Authorization", outra())).andExpect(status().isNoContent());
        http.perform(get("/usuarios/" + id + "/foto").header("Authorization", inv())).andExpect(status().isNotFound());
    }

    @Test
    void startupUploadsAndRemovesMaterials() throws Exception {
        http.perform(multipart(HttpMethod.PUT, "/perfil/materiais/canvas")
                .file(new MockMultipartFile("file", "canvas.pdf", "application/pdf", PDF))
                .header("Authorization", st()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("hasCanvas").value(true))
            .andExpect(jsonPath("canvasName").value("canvas.pdf"));
        http.perform(multipart(HttpMethod.PUT, "/perfil/materiais/canvas")
                .file(new MockMultipartFile("file", "virus.exe", "application/octet-stream", "MZ".getBytes()))
                .header("Authorization", st()))
            .andExpect(status().isBadRequest());
        http.perform(delete("/perfil/materiais/pitch").header("Authorization", st()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("hasPitchDeck").value(false));
        http.perform(multipart(HttpMethod.PUT, "/perfil/materiais/canvas")
                .file(new MockMultipartFile("file", "canvas.pdf", "application/pdf", PDF))
                .header("Authorization", inv()))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void matchCardsAndFeedExposeAttachmentsOnlyWhenPresent() throws Exception {
        http.perform(get("/matchmaking/investidor").header("Authorization", inv()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("items[?(@.name == 'TeleSertão')].hasPitchDeck", contains(true)))
            .andExpect(jsonPath("items[?(@.name == 'TeleSertão')].hasCanvas", contains(false)))
            .andExpect(jsonPath("items[?(@.name == 'TeleSertão')].hasPhoto", contains(true)))
            .andExpect(jsonPath("items[?(@.name == 'TeleSertão')].presentationVideo", contains("https://video.example/pitch")))
            .andExpect(jsonPath("items[?(@.name == 'Outra')].hasPitchDeck", contains(false)))
            .andExpect(jsonPath("items[?(@.name == 'Outra')].presentationVideo", contains("")));
        http.perform(get("/startups").header("Authorization", inv()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("[?(@.name == 'TeleSertão')].hasPhoto", contains(true)))
            .andExpect(jsonPath("[?(@.name == 'TeleSertão')].userId", contains((int) usuario(ST))));
    }

    @Test
    void legacyUnauthenticatedStartupWritesAreGone() throws Exception {
        http.perform(put("/startups/" + perfilDe(ST)).header("Authorization", outra())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Invadida\"}"))
            .andExpect(status().is4xxClientError());
        http.perform(get("/startups/" + perfilDe(ST) + "/perfil").header("Authorization", inv()))
            .andExpect(jsonPath("name").value("TeleSertão"));
    }
}
