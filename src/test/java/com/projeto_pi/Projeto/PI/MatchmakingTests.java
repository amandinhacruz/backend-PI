package com.projeto_pi.Projeto.PI;

import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import com.projeto_pi.Projeto.PI.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** RF03/RF04/RF09/RF10: fila do investidor, pedidos da startup, conexões, avaliação e aprendizado (sem IA externa). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MatchmakingTests {

    @Autowired MockMvc http;
    @Autowired UsuarioRepository users;
    @Autowired JwtService jwt;
    @Autowired JsonMapper json;

    static final String INVESTIDOR = "inv@match.test";

    /* ------------------------------------------------------------------ apoio */

    String startup(String email, String nome, String segmento, String estagio, String min, String max) {
        return """
            {"email":"%s","password":"Senha@123","name":"%s","profileOwner":"Ana",
            "segment":"%s","stage":"%s","city":"Recife","state":"PE",
            "minimumTicket":%s,"maximumTicket":%s,
            "summary":"Plataforma de telemedicina com inteligência artificial para clínicas do interior",
            "problem":"Faltam médicos especialistas fora das capitais",
            "terms":true,"privacy":true,"dataProcessing":true}
            """.formatted(email, nome, segmento, estagio, min, max);
    }

    void cadastrarStartup(String email, String nome, String segmento, String estagio, String min, String max,
                          boolean aprovar) throws Exception {
        http.perform(post("/auth/cadastro/startup").contentType(MediaType.APPLICATION_JSON)
                .content(startup(email, nome, segmento, estagio, min, max))).andExpect(status().isCreated());
        if (aprovar) aprovar(email);
    }

    void cadastrarInvestidor(String email, String segmentos, String estagios, String min, String max) throws Exception {
        http.perform(post("/auth/cadastro/investidor").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"%s","password":"Senha@123","name":"Bia Investidora","cpf":"12345678909",
            "city":"Recife","state":"PE","segments":%s,"stages":%s,
            "minimumTicket":%s,"maximumTicket":%s,
            "aboutInvestor":"Investidora anjo","lookingFor":"Startups de saúde com inteligência artificial",
            "terms":true,"privacy":true,"dataProcessing":true}
            """.formatted(email, segmentos, estagios, min, max))).andExpect(status().isCreated());
        aprovar(email);
    }

    void investidorPadrao() throws Exception {
        cadastrarInvestidor(INVESTIDOR, "[\"Healthtech\",\"Fintech\"]", "[\"Ideação\",\"Operação\"]", "50000", "200000");
    }

    void aprovar(String email) {
        Usuario u = users.findByEmail(email).orElseThrow();
        u.setStatus(StatusUsuario.APROVADO);
        users.saveAndFlush(u);
    }

    String token(String email, String papel) { return "Bearer " + jwt.gerarToken(email, papel); }
    String inv() { return token(INVESTIDOR, "INVESTIDOR"); }

    long perfilId(String email) { return users.findByEmail(email).orElseThrow().getStartup().getId(); }

    ResultActions fila(String tokenInvestidor) throws Exception {
        return http.perform(get("/matchmaking/investidor").header("Authorization", tokenInvestidor));
    }

    ResultActions decidir(String tokenInvestidor, String emailStartup, String decisao) throws Exception {
        return http.perform(post("/matchmaking/investidor/startups/" + perfilId(emailStartup) + "/decisao")
                .header("Authorization", tokenInvestidor).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"" + decisao + "\"}"));
    }

    JsonNode pedidos(String emailStartup) throws Exception {
        return json.readTree(http.perform(get("/matchmaking/startup/pedidos")
                .header("Authorization", token(emailStartup, "STARTUP")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    /** Pedido do investidor firmado pela startup; devolve o id da conexão. */
    long firmar(String emailStartup) throws Exception {
        decidir(inv(), emailStartup, "accepted").andExpect(status().isOk());
        long id = pedidos(emailStartup).get(0).get("id").asLong();
        http.perform(post("/matchmaking/startup/pedidos/" + id + "/decisao")
                .header("Authorization", token(emailStartup, "STARTUP")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"accepted\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("status").value("FIRMADA"));
        return id;
    }

    /* ------------------------------------------------------------- elegibilidade */

    @Test
    void investorSeesOnlyStartupsMatchingSegmentStageAndTicket() throws Exception {
        investidorPadrao();
        cadastrarStartup("ok@s.test", "Combina", "Healthtech", "Ideação", "80000", "150000", true);
        cadastrarStartup("seg@s.test", "Outro segmento", "Agritech / Agronegócio", "Ideação", "80000", "150000", true);
        cadastrarStartup("est@s.test", "Outro estagio", "Healthtech", "Escala", "80000", "150000", true);
        cadastrarStartup("tkt@s.test", "Ticket alto", "Healthtech", "Ideação", "500000", "900000", true);
        cadastrarStartup("pend@s.test", "Nao aprovada", "Healthtech", "Ideação", "80000", "150000", false);

        fila(inv()).andExpect(status().isOk())
            .andExpect(jsonPath("items.length()").value(1))
            .andExpect(jsonPath("items[0].name").value("Combina"))
            .andExpect(jsonPath("items[0].source").value("LOCAL"))
            .andExpect(jsonPath("items[0].affinity").value(greaterThanOrEqualTo(60)))
            .andExpect(jsonPath("items[0].tags", contains("Healthtech", "Ideação")))
            .andExpect(jsonPath("items[0].location").value("Recife/PE"))
            .andExpect(jsonPath("items[0].factors", hasItem("Segmento: Healthtech")))
            .andExpect(jsonPath("items[0].summary").isNotEmpty())
            .andExpect(jsonPath("items[0].cnpj").doesNotExist())
            .andExpect(jsonPath("items[0].email").doesNotExist())
            .andExpect(jsonPath("ai.enabled").value(false))
            .andExpect(jsonPath("criteria.segments", contains("Healthtech", "Fintech")))
            .andExpect(jsonPath("approvedStartups").value(4));
    }

    @Test
    void legacySegmentLabelsAndTicketOverlapCount() throws Exception {
        investidorPadrao();
        // "Saúde" (rótulo antigo) equivale a Healthtech; a faixa só encosta na do investidor.
        cadastrarStartup("saude@s.test", "Saude antiga", "Saúde", "Operação", "200000", "400000", true);
        fila(inv()).andExpect(jsonPath("items.length()").value(1))
            .andExpect(jsonPath("items[0].name").value("Saude antiga"));
    }

    @Test
    void investorWithoutStageOrTicketPreferenceIsNotFilteredByThem() throws Exception {
        http.perform(post("/auth/cadastro/investidor").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"%s","password":"Senha@123","name":"Sem preferencia","segment":"Healthtech",
            "terms":true,"privacy":true,"dataProcessing":true}
            """.formatted(INVESTIDOR))).andExpect(status().isCreated());
        aprovar(INVESTIDOR);
        cadastrarStartup("a@s.test", "Qualquer", "Healthtech", "Escala", "900000", "1000000", true);
        fila(inv()).andExpect(jsonPath("items.length()").value(1));
    }

    /* ------------------------------------------------------- pedidos e conexões */

    @Test
    void requestGoesToStartupAndPassStaysPrivate() throws Exception {
        investidorPadrao();
        cadastrarStartup("a@s.test", "Alfa", "Healthtech", "Ideação", "80000", "150000", true);
        cadastrarStartup("b@s.test", "Beta", "Fintech", "Operação", "60000", "100000", true);

        decidir(inv(), "a@s.test", "accepted").andExpect(status().isOk())
            .andExpect(jsonPath("status").value("SOLICITADA"));
        decidir(inv(), "b@s.test", "rejected").andExpect(status().isOk())
            .andExpect(jsonPath("status").value("DISPENSADA"));

        fila(inv()).andExpect(jsonPath("items.length()").value(0));
        decidir(inv(), "a@s.test", "accepted").andExpect(status().isConflict());

        JsonNode recebidos = pedidos("a@s.test");
        assertEquals(1, recebidos.size());
        assertEquals("Bia Investidora", recebidos.get(0).get("name").asString());
        assertEquals("Investidor(a) anjo", recebidos.get(0).get("role").asString());
        assertFalse(recebidos.get(0).has("cpf"));
        assertEquals(0, pedidos("b@s.test").size());
    }

    @Test
    void acceptedRequestBecomesMatchForBothSides() throws Exception {
        investidorPadrao();
        cadastrarStartup("a@s.test", "Alfa", "Healthtech", "Ideação", "80000", "150000", true);
        long id = firmar("a@s.test");

        http.perform(get("/conexoes").header("Authorization", inv()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Alfa")).andExpect(jsonPath("$[0].state").value("PE"))
            .andExpect(jsonPath("$[0].segments", contains("Healthtech")))
            .andExpect(jsonPath("$[0].connectedAt").isNotEmpty());
        http.perform(get("/conexoes").header("Authorization", token("a@s.test", "STARTUP")))
            .andExpect(jsonPath("$[0].name").value("Bia Investidora"))
            .andExpect(jsonPath("$[0].segments", contains("Healthtech", "Fintech")));
        assertEquals(0, pedidos("a@s.test").size());

        // não dá para responder duas vezes
        http.perform(post("/matchmaking/startup/pedidos/" + id + "/decisao")
                .header("Authorization", token("a@s.test", "STARTUP")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"rejected\"}")).andExpect(status().isConflict());

        http.perform(get("/admin/metricas").header("Authorization", token("admin@x.test", "ADMIN")))
            .andExpect(jsonPath("matches").value(1)).andExpect(jsonPath("pedidosConexao").value(1));
    }

    @Test
    void deniedRequestIsNotAConnection() throws Exception {
        investidorPadrao();
        cadastrarStartup("a@s.test", "Alfa", "Healthtech", "Ideação", "80000", "150000", true);
        decidir(inv(), "a@s.test", "accepted");
        long id = pedidos("a@s.test").get(0).get("id").asLong();
        http.perform(post("/matchmaking/startup/pedidos/" + id + "/decisao")
                .header("Authorization", token("a@s.test", "STARTUP")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"rejected\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("status").value("NEGADA"));
        http.perform(get("/conexoes").header("Authorization", inv())).andExpect(content().json("[]"));
        fila(inv()).andExpect(jsonPath("items.length()").value(0));
    }

    /* ------------------------------------------------------------- autorização */

    @Test
    void endpointsAreRestrictedByRoleAndOwnership() throws Exception {
        investidorPadrao();
        cadastrarStartup("a@s.test", "Alfa", "Healthtech", "Ideação", "80000", "150000", true);
        cadastrarStartup("b@s.test", "Beta", "Healthtech", "Ideação", "80000", "150000", true);
        decidir(inv(), "a@s.test", "accepted");
        long id = pedidos("a@s.test").get(0).get("id").asLong();

        http.perform(get("/matchmaking/investidor")).andExpect(status().isUnauthorized());
        fila(token("a@s.test", "STARTUP")).andExpect(status().isForbidden());
        http.perform(get("/matchmaking/startup/pedidos").header("Authorization", inv()))
            .andExpect(status().isForbidden());
        // outra startup não responde pedido alheio
        http.perform(post("/matchmaking/startup/pedidos/" + id + "/decisao")
                .header("Authorization", token("b@s.test", "STARTUP")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"accepted\"}")).andExpect(status().isNotFound());
        // contrato legado: só a dona do perfil ou o admin
        long perfilA = perfilId("a@s.test");
        http.perform(get("/matchmaking/startup/" + perfilA).header("Authorization", token("b@s.test", "STARTUP")))
            .andExpect(status().isForbidden());
        http.perform(get("/matchmaking/startup/" + perfilA).header("Authorization", inv()))
            .andExpect(status().isForbidden());
        http.perform(get("/matchmaking/startup/" + perfilA).header("Authorization", token("a@s.test", "STARTUP")))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].pontuacao").value(100));
        // decisão inválida e startup fora da fila
        http.perform(post("/matchmaking/investidor/startups/" + perfilId("b@s.test") + "/decisao")
                .header("Authorization", inv()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"talvez\"}")).andExpect(status().isBadRequest());
        http.perform(post("/matchmaking/investidor/startups/999999/decisao")
                .header("Authorization", inv()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"accepted\"}")).andExpect(status().isNotFound());
    }

    @Test
    void suspendedInvestorLosesAccessEvenWithValidToken() throws Exception {
        investidorPadrao();
        Usuario u = users.findByEmail(INVESTIDOR).orElseThrow();
        u.setStatus(StatusUsuario.SUSPENSO);
        users.saveAndFlush(u);
        fila(inv()).andExpect(status().isForbidden());
    }

    /* ------------------------------------------------------------------- RF10 */

    @Test
    void ratingIsOnlyForParticipantsOfConfirmedConnections() throws Exception {
        investidorPadrao();
        cadastrarStartup("a@s.test", "Alfa", "Healthtech", "Ideação", "80000", "150000", true);
        cadastrarStartup("b@s.test", "Beta", "Healthtech", "Ideação", "80000", "150000", true);
        long id = firmar("a@s.test");

        http.perform(put("/conexoes/" + id + "/avaliacao").header("Authorization", inv())
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":6}"))
            .andExpect(status().isBadRequest());
        http.perform(put("/conexoes/" + id + "/avaliacao").header("Authorization", token("b@s.test", "STARTUP"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":5}"))
            .andExpect(status().isNotFound());
        http.perform(put("/conexoes/" + id + "/avaliacao").header("Authorization", inv())
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":4,\"comment\":\"Boa conversa\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("score").value(4));
        // avaliar de novo atualiza em vez de duplicar
        http.perform(put("/conexoes/" + id + "/avaliacao").header("Authorization", inv())
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":5}"))
            .andExpect(status().isOk());
        http.perform(get("/conexoes").header("Authorization", inv()))
            .andExpect(jsonPath("$[0].myRating.score").value(5));
        http.perform(get("/conexoes").header("Authorization", token("a@s.test", "STARTUP")))
            .andExpect(jsonPath("$[0].myRating").value(nullValue()));
        http.perform(get("/admin/metricas").header("Authorization", token("admin@x.test", "ADMIN")))
            .andExpect(jsonPath("avaliacoes").value(1)).andExpect(jsonPath("notaMedia").value(5.0));

        // pedido ainda não firmado não pode ser avaliado
        decidir(inv(), "b@s.test", "accepted");
        long pendente = pedidos("b@s.test").get(0).get("id").asLong();
        http.perform(put("/conexoes/" + pendente + "/avaliacao").header("Authorization", inv())
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":3}"))
            .andExpect(status().isConflict());
    }

    /* ------------------------------------------------------------ aprendizado */

    @Test
    void rankingLearnsFromInvestorDecisions() throws Exception {
        investidorPadrao();
        // histórico: solicita duas em Ideação, passa duas em Operação
        cadastrarStartup("i1@s.test", "I1", "Healthtech", "Ideação", "80000", "150000", true);
        cadastrarStartup("i2@s.test", "I2", "Healthtech", "Ideação", "80000", "150000", true);
        cadastrarStartup("o1@s.test", "O1", "Healthtech", "Operação", "80000", "150000", true);
        cadastrarStartup("o2@s.test", "O2", "Healthtech", "Operação", "80000", "150000", true);
        decidir(inv(), "i1@s.test", "accepted");
        decidir(inv(), "i2@s.test", "accepted");
        decidir(inv(), "o1@s.test", "rejected");
        decidir(inv(), "o2@s.test", "rejected");

        // duas candidatas novas, idênticas a não ser pelo estágio
        cadastrarStartup("nova-o@s.test", "Nova Operacao", "Healthtech", "Operação", "80000", "150000", true);
        cadastrarStartup("nova-i@s.test", "Nova Ideacao", "Healthtech", "Ideação", "80000", "150000", true);

        fila(inv()).andExpect(jsonPath("items.length()").value(2))
            .andExpect(jsonPath("items[0].name").value("Nova Ideacao"))
            .andExpect(jsonPath("items[0].breakdown.learning").value(greaterThan(0)))
            .andExpect(jsonPath("items[1].breakdown.learning").value(lessThan(0)))
            .andExpect(jsonPath("items[0].factors", hasItem("Parecida com startups que você já solicitou")));
    }

    /* ---------------------------------------------------------------- dossiê */

    @Test
    void dossierFallsBackToLocalContentWithoutAi() throws Exception {
        investidorPadrao();
        cadastrarStartup("a@s.test", "Alfa", "Healthtech", "Ideação", "80000", "150000", true);
        cadastrarStartup("fora@s.test", "Fora", "Agritech / Agronegócio", "Ideação", "80000", "150000", true);
        http.perform(get("/matchmaking/investidor/startups/" + perfilId("a@s.test") + "/dossie")
                .header("Authorization", inv()))
            .andExpect(status().isOk()).andExpect(jsonPath("source").value("LOCAL"))
            .andExpect(jsonPath("executiveSummary", containsString("telemedicina")))
            .andExpect(jsonPath("questions.length()").value(3));
        // startup fora dos critérios não tem dossiê
        http.perform(get("/matchmaking/investidor/startups/" + perfilId("fora@s.test") + "/dossie")
                .header("Authorization", inv())).andExpect(status().isNotFound());
    }

    @Test
    void adminSeesAiStatus() throws Exception {
        http.perform(get("/admin/ia/status").header("Authorization", token("admin@x.test", "ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("habilitada").value(false))
            .andExpect(jsonPath("disponivel").value(false))
            .andExpect(jsonPath("modelo").value("claude-opus-5-5"));
        http.perform(get("/admin/ia/status").header("Authorization", inv())).andExpect(status().isForbidden());
    }
}
