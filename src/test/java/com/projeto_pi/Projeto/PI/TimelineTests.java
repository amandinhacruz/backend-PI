package com.projeto_pi.Projeto.PI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import com.projeto_pi.Projeto.PI.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Timeline: publicar, curtir, comentar, compartilhar, excluir, paginação e acesso. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TimelineTests {

    @Autowired MockMvc http;
    @Autowired UsuarioRepository users;
    @Autowired JwtService jwt;
    final ObjectMapper json = new ObjectMapper();

    static final String INV = "inv@timeline.test";
    static final String ST = "st@timeline.test";
    static final String ST2 = "st2@timeline.test";
    static final String PEND = "pend@timeline.test";
    static final byte[] PDF = "%PDF-1.4 teste".getBytes();

    String token(String email, String papel) { return "Bearer " + jwt.gerarToken(email, papel); }
    String inv() { return token(INV, "INVESTIDOR"); }
    String st() { return token(ST, "STARTUP"); }
    String st2() { return token(ST2, "STARTUP"); }
    Usuario user(String email) { return users.findByEmail(email).orElseThrow(); }

    void definirStatus(String email, StatusUsuario s) {
        Usuario u = user(email);
        u.setStatus(s);
        users.saveAndFlush(u);
    }

    void startup(String email, String nome) throws Exception {
        http.perform(multipart("/auth/cadastro/startup")
                .file(new MockMultipartFile("pitchDeck", "pitch.pdf", "application/pdf", PDF))
                .param("email", email).param("password", "Senha@123").param("name", nome)
                .param("segment", "Fintech").param("stage", "Ideação")
                .param("terms", "true").param("privacy", "true").param("dataProcessing", "true"))
            .andExpect(status().isCreated());
    }

    @BeforeEach
    void dados() throws Exception {
        http.perform(post("/auth/cadastro/investidor").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"%s","password":"Senha@123","name":"Ana Investidora","segments":["Fintech"],"cpf":"52998224725",
            "terms":true,"privacy":true,"dataProcessing":true}
            """.formatted(INV))).andExpect(status().isCreated());
        definirStatus(INV, StatusUsuario.APROVADO);
        startup(ST, "CrediFeira");
        definirStatus(ST, StatusUsuario.APROVADO);
        startup(ST2, "AgroData");
        definirStatus(ST2, StatusUsuario.APROVADO);
        startup(PEND, "Pendente");
    }

    ResultActions publicar(String auth, String texto, String compartilhada) throws Exception {
        Map<String, String> corpo = new java.util.HashMap<>();
        if (texto != null) corpo.put("text", texto);
        if (compartilhada != null) corpo.put("sharedPostId", compartilhada);
        return http.perform(post("/timeline").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)));
    }

    String id(ResultActions r) throws Exception {
        JsonNode no = json.readTree(r.andReturn().getResponse().getContentAsString());
        return no.get("id").asText();
    }

    @Test
    void postsAppearNewestFirstWithAuthorData() throws Exception {
        publicar(inv(), "Procurando fintechs em fase seed", null).andExpect(status().isCreated())
            .andExpect(jsonPath("mine").value(true));
        publicar(st(), "Lançamos nosso app hoje!", null).andExpect(status().isCreated());

        http.perform(get("/timeline").header("Authorization", st2()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].text", contains("Lançamos nosso app hoje!", "Procurando fintechs em fase seed")))
            .andExpect(jsonPath("$[0].author.name").value("CrediFeira"))
            .andExpect(jsonPath("$[0].author.type").value("STARTUP"))
            .andExpect(jsonPath("$[0].author.role").value("Startup"))
            .andExpect(jsonPath("$[0].author.profileId").value(user(ST).getStartup().getId().toString()))
            .andExpect(jsonPath("$[1].author.type").value("INVESTIDOR"))
            .andExpect(jsonPath("$[1].author.userId").value(user(INV).getId()))
            .andExpect(jsonPath("$[0].mine").value(false));
    }

    @Test
    void invalidPostsAreRejected() throws Exception {
        publicar(st(), "   ", null).andExpect(status().isBadRequest());
        publicar(st(), "a".repeat(501), null).andExpect(status().isBadRequest());
        publicar(st(), "ok", "999999").andExpect(status().isNotFound());
    }

    @Test
    void likeAndUnlikeAreIdempotent() throws Exception {
        String post = id(publicar(st(), "Novo cliente!", null));
        http.perform(post("/timeline/" + post + "/curtida").header("Authorization", inv()))
            .andExpect(jsonPath("likes").value(1)).andExpect(jsonPath("liked").value(true));
        http.perform(post("/timeline/" + post + "/curtida").header("Authorization", inv()))
            .andExpect(jsonPath("likes").value(1));
        http.perform(post("/timeline/" + post + "/curtida").header("Authorization", st2()))
            .andExpect(jsonPath("likes").value(2));
        http.perform(delete("/timeline/" + post + "/curtida").header("Authorization", inv()))
            .andExpect(jsonPath("likes").value(1)).andExpect(jsonPath("liked").value(false));
    }

    @Test
    void commentsAreListedCountedAndModerated() throws Exception {
        String post = id(publicar(st(), "Qual a melhor forma de captar?", null));
        String corpo = "{\"text\":\"Comece pelos anjos da sua região.\"}";
        String comentario = json.readTree(http.perform(post("/timeline/" + post + "/comentarios")
                        .header("Authorization", inv()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("author.name").value("Ana Investidora"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        http.perform(post("/timeline/" + post + "/comentarios").header("Authorization", st2())
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"  \"}"))
            .andExpect(status().isBadRequest());

        http.perform(get("/timeline/" + post + "/comentarios").header("Authorization", st2()))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].canDelete").value(false));
        http.perform(get("/timeline").header("Authorization", st2()))
            .andExpect(jsonPath("$[0].comments").value(1));

        // terceiro não apaga; o autor da publicação pode
        http.perform(delete("/timeline/comentarios/" + comentario).header("Authorization", st2()))
            .andExpect(status().isForbidden());
        http.perform(delete("/timeline/comentarios/" + comentario).header("Authorization", st()))
            .andExpect(status().isNoContent());
        http.perform(get("/timeline/" + post + "/comentarios").header("Authorization", st2()))
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void sharingWorksLikeRepostAndQuote() throws Exception {
        String original = id(publicar(st(), "Abrimos rodada seed", null));
        // compartilhamento simples
        String repost = id(publicar(inv(), null, original).andExpect(status().isCreated())
                .andExpect(jsonPath("repost").value(true))
                .andExpect(jsonPath("sharedPost.id").value(original))
                .andExpect(jsonPath("sharedPost.shares").value(1))
                .andExpect(jsonPath("sharedPost.shared").value(true)));
        publicar(inv(), null, original).andExpect(status().isConflict()); // de novo não
        // compartilhar o compartilhamento = compartilhar a original
        publicar(st2(), null, repost).andExpect(jsonPath("sharedPost.id").value(original));
        // com comentário
        publicar(st2(), "Vale olhar!", original)
            .andExpect(jsonPath("repost").value(false))
            .andExpect(jsonPath("text").value("Vale olhar!"))
            .andExpect(jsonPath("sharedPost.text").value("Abrimos rodada seed"));
        // curtir o compartilhamento simples curte a original
        http.perform(post("/timeline/" + repost + "/curtida").header("Authorization", st2()))
            .andExpect(jsonPath("id").value(original)).andExpect(jsonPath("likes").value(1));

        http.perform(get("/timeline/" + original).header("Authorization", st()))
            .andExpect(jsonPath("shares").value(3));
        // desfazer
        http.perform(delete("/timeline/" + original + "/compartilhamento").header("Authorization", inv()))
            .andExpect(jsonPath("shares").value(2)).andExpect(jsonPath("shared").value(false));
    }

    @Test
    void deletingAPostHidesItAndItsReposts() throws Exception {
        String original = id(publicar(st(), "Publicação que será apagada", null));
        publicar(inv(), null, original);
        publicar(st2(), "Comentando a original", original);
        http.perform(delete("/timeline/" + original).header("Authorization", inv())).andExpect(status().isForbidden());
        http.perform(delete("/timeline/" + original).header("Authorization", st())).andExpect(status().isNoContent());

        http.perform(get("/timeline").header("Authorization", inv()))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].text").value("Comentando a original"))
            .andExpect(jsonPath("$[0].sharedPost").value(nullValue()))
            .andExpect(jsonPath("$[0].sharedUnavailable").value(true));
        http.perform(post("/timeline/" + original + "/curtida").header("Authorization", inv()))
            .andExpect(status().isNotFound());
    }

    @Test
    void paginationAndNewPosts() throws Exception {
        String primeiro = null;
        for (int i = 1; i <= 23; i++) {
            String id = id(publicar(st(), "Post " + i, null));
            if (i == 1) primeiro = id;
        }
        String resposta = http.perform(get("/timeline").header("Authorization", inv()))
            .andExpect(jsonPath("$", hasSize(20)))
            .andExpect(jsonPath("$[0].text").value("Post 23"))
            .andReturn().getResponse().getContentAsString();
        String ultimo = json.readTree(resposta).get(19).get("id").asText();
        http.perform(get("/timeline").param("antes", ultimo).header("Authorization", inv()))
            .andExpect(jsonPath("$[*].text", contains("Post 3", "Post 2", "Post 1")));
        http.perform(get("/timeline").param("depois", primeiro).header("Authorization", inv()))
            .andExpect(jsonPath("$", hasSize(22)));
        http.perform(get("/timeline/usuarios/" + user(ST).getId()).header("Authorization", inv()))
            .andExpect(jsonPath("$", hasSize(20)));
    }

    @Test
    void suspendedAuthorsDisappearAndAccessIsRestricted() throws Exception {
        publicar(st2(), "Some quando suspensa", null);
        publicar(st(), "Continua", null);
        definirStatus(ST2, StatusUsuario.SUSPENSO);
        http.perform(get("/timeline").header("Authorization", inv()))
            .andExpect(jsonPath("$[*].text", contains("Continua")));

        http.perform(get("/timeline").header("Authorization", token(PEND, "STARTUP"))).andExpect(status().isForbidden());
        http.perform(get("/timeline").header("Authorization", token("admin@projetopi.com", "ADMIN")))
            .andExpect(status().isForbidden());
        int anonimo = http.perform(get("/timeline")).andReturn().getResponse().getStatus();
        assertTrue(anonimo == 401 || anonimo == 403, "anônimo respondeu " + anonimo);
    }

    @Test
    void publicInvestorProfileHidesPrivateData() throws Exception {
        long investidor = user(INV).getId();
        http.perform(get("/investidores/" + investidor + "/perfil").header("Authorization", st()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("name").value("Ana Investidora"))
            .andExpect(jsonPath("cpf").value(nullValue()))
            .andExpect(jsonPath("email").value(nullValue()));
        http.perform(get("/investidores/" + user(ST).getId() + "/perfil").header("Authorization", inv()))
            .andExpect(status().isNotFound());
        http.perform(get("/investidores/" + investidor + "/perfil").header("Authorization", token(PEND, "STARTUP")))
            .andExpect(status().isForbidden());
    }
}
