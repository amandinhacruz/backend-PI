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
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Comunidades: lista, entrada e saída, conversa em grupo, não lidas e regras de acesso. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ComunidadeTests {

    @Autowired MockMvc http;
    @Autowired UsuarioRepository users;
    @Autowired JwtService jwt;
    final ObjectMapper json = new ObjectMapper();

    static final String INV = "inv@comunidade.test";
    static final String ST = "st@comunidade.test";
    static final String PEND = "pend@comunidade.test";
    static final byte[] PDF = "%PDF-1.4 teste".getBytes();

    String token(String email, String papel) { return "Bearer " + jwt.gerarToken(email, papel); }
    String inv() { return token(INV, "INVESTIDOR"); }
    String st() { return token(ST, "STARTUP"); }

    void aprovar(String email) {
        Usuario u = users.findByEmail(email).orElseThrow();
        u.setStatus(StatusUsuario.APROVADO);
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
            {"email":"%s","password":"Senha@123","name":"Ana Investidora","segments":["Fintech"],
            "terms":true,"privacy":true,"dataProcessing":true}
            """.formatted(INV))).andExpect(status().isCreated());
        aprovar(INV);
        startup(ST, "CrediFeira");
        aprovar(ST);
        startup(PEND, "Pendente");
    }

    String comunidade(String slug) throws Exception {
        JsonNode lista = json.readTree(http.perform(get("/comunidades").header("Authorization", inv()))
                .andReturn().getResponse().getContentAsString());
        for (JsonNode item : lista) {
            if (item.get("slug").asText().equals(slug)) return item.get("id").asText();
        }
        throw new AssertionError("comunidade " + slug + " não encontrada");
    }

    String enviar(String id, String auth, String texto, String categoria) throws Exception {
        String corpo = json.writeValueAsString(java.util.Map.of("text", texto, "category", categoria));
        return json.readTree(http.perform(post("/comunidades/" + id + "/mensagens").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void sixCommunitiesWithNameDescriptionAndSlug() throws Exception {
        http.perform(get("/comunidades").header("Authorization", st()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(6)))
            .andExpect(jsonPath("$[*].slug", contains("desenvolvimento", "ciberseguranca", "ia-dados",
                    "captacao", "impacto-esg", "ecossistema")))
            .andExpect(jsonPath("$[0].name").value("Desenvolvimento & Produto"))
            .andExpect(jsonPath("$[*].description", everyItem(not(emptyString()))))
            .andExpect(jsonPath("$[*].joined", everyItem(is(false))))
            .andExpect(jsonPath("$[*].members", everyItem(is(0))));
    }

    @Test
    void membersTalkInGroupLikeAChat() throws Exception {
        String id = comunidade("ciberseguranca");
        // sem entrar, não lê nem envia
        http.perform(get("/comunidades/" + id + "/mensagens").header("Authorization", inv()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("message").value(containsString("Entre na comunidade")));

        http.perform(post("/comunidades/" + id + "/entrar").header("Authorization", inv()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("joined").value(true))
            .andExpect(jsonPath("members").value(1));
        http.perform(post("/comunidades/" + id + "/entrar").header("Authorization", st()))
            .andExpect(jsonPath("members").value(2));

        enviar(id, st(), "Lançamos nosso programa de bug bounty!", "INICIATIVA");

        // a investidora tem 1 não lida (avisos de entrada não contam)
        http.perform(get("/comunidades").header("Authorization", inv()))
            .andExpect(jsonPath("$[?(@.id == '" + id + "')].unread").value(contains(1)))
            .andExpect(jsonPath("$[?(@.id == '" + id + "')].lastMessage.author").value(contains("CrediFeira")));

        http.perform(get("/comunidades/" + id + "/mensagens").header("Authorization", inv()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].sender", contains("system", "system", "member")))
            .andExpect(jsonPath("$[0].text").value("Ana Investidora entrou na comunidade"))
            .andExpect(jsonPath("$[2].authorName").value("CrediFeira"))
            .andExpect(jsonPath("$[2].authorRole").value("Startup"))
            .andExpect(jsonPath("$[2].category").value("INICIATIVA"));

        // depois de ler, zera
        http.perform(get("/comunidades").header("Authorization", inv()))
            .andExpect(jsonPath("$[?(@.id == '" + id + "')].unread").value(contains(0)));

        // para quem enviou, a própria mensagem aparece como "me"
        http.perform(get("/comunidades/" + id + "/mensagens").header("Authorization", st()))
            .andExpect(jsonPath("$[2].sender").value("me"));

        http.perform(get("/comunidades/" + id + "/membros").header("Authorization", inv()))
            .andExpect(jsonPath("$[*].name", contains("Ana Investidora", "CrediFeira")))
            .andExpect(jsonPath("$[0].me").value(true));
    }

    @Test
    void pollingWithDepoisReturnsOnlyNewMessages() throws Exception {
        String id = comunidade("captacao");
        http.perform(post("/comunidades/" + id + "/entrar").header("Authorization", inv()));
        http.perform(post("/comunidades/" + id + "/entrar").header("Authorization", st()));
        String primeira = enviar(id, inv(), "Edital aberto para pré-seed", "NOTICIA");
        enviar(id, st(), "Fechamos nossa rodada seed!", "MOVIMENTO");
        http.perform(get("/comunidades/" + id + "/mensagens").param("depois", primeira).header("Authorization", inv()))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].text").value("Fechamos nossa rodada seed!"))
            .andExpect(jsonPath("$[0].category").value("MOVIMENTO"));
    }

    @Test
    void joinTwiceDoesNotDuplicateAndLeavingBlocksMessages() throws Exception {
        String id = comunidade("ia-dados");
        http.perform(post("/comunidades/" + id + "/entrar").header("Authorization", st()));
        http.perform(post("/comunidades/" + id + "/entrar").header("Authorization", st()))
            .andExpect(jsonPath("members").value(1));
        http.perform(post("/comunidades/" + id + "/entrar").header("Authorization", inv()));
        http.perform(post("/comunidades/" + id + "/sair").header("Authorization", st()))
            .andExpect(jsonPath("joined").value(false))
            .andExpect(jsonPath("members").value(1));
        http.perform(post("/comunidades/" + id + "/mensagens").header("Authorization", st())
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"oi\"}"))
            .andExpect(status().isForbidden());
        String texto = http.perform(get("/comunidades/" + id + "/mensagens").header("Authorization", inv()))
            .andReturn().getResponse().getContentAsString();
        assertTrue(texto.contains("CrediFeira saiu da comunidade"), texto);
        assertEquals(1, texto.split("CrediFeira entrou").length - 1, "aviso de entrada duplicado");
    }

    @Test
    void invalidMessagesAreRejected() throws Exception {
        String id = comunidade("desenvolvimento");
        http.perform(post("/comunidades/" + id + "/entrar").header("Authorization", inv()));
        for (String corpo : new String[]{"{\"text\":\"   \"}", "{\"text\":\"oi\",\"category\":\"SISTEMA\"}",
                "{\"text\":\"oi\",\"category\":\"QUALQUER\"}", "{\"text\":\"" + "a".repeat(1001) + "\"}"}) {
            http.perform(post("/comunidades/" + id + "/mensagens").header("Authorization", inv())
                    .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
        }
        // sem categoria vira mensagem comum
        http.perform(post("/comunidades/" + id + "/mensagens").header("Authorization", inv())
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Olá, pessoal\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("category").value("MENSAGEM"))
            .andExpect(jsonPath("sender").value("me"));
    }

    @Test
    void onlyApprovedStartupsAndInvestorsHaveAccess() throws Exception {
        int anonimo = http.perform(get("/comunidades")).andReturn().getResponse().getStatus();
        assertTrue(anonimo == 401 || anonimo == 403, "anônimo respondeu " + anonimo);
        http.perform(get("/comunidades").header("Authorization", token(PEND, "STARTUP"))).andExpect(status().isForbidden());
        http.perform(get("/comunidades").header("Authorization", token("admin@projetopi.com", "ADMIN")))
            .andExpect(status().isForbidden());
        http.perform(post("/comunidades/999999/entrar").header("Authorization", inv())).andExpect(status().isNotFound());
    }
}
