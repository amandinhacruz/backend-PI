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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** RF05: mensagens entre conexões firmadas, convite de reunião do investidor e agenda dos dois lados. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConversaTests {

    @Autowired MockMvc http;
    @Autowired UsuarioRepository users;
    @Autowired JwtService jwt;
    @Autowired JsonMapper json;

    static final String INV = "inv@chat.test";
    static final String ST = "st@chat.test";
    static final String OUTRA = "outra@chat.test";
    String conexao;

    String inv() { return "Bearer " + jwt.gerarToken(INV, "INVESTIDOR"); }
    String st() { return "Bearer " + jwt.gerarToken(ST, "STARTUP"); }
    String outra() { return "Bearer " + jwt.gerarToken(OUTRA, "STARTUP"); }

    void aprovar(String email) {
        Usuario u = users.findByEmail(email).orElseThrow();
        u.setStatus(StatusUsuario.APROVADO);
        users.saveAndFlush(u);
    }

    void startup(String email, String nome) throws Exception {
        http.perform(post("/auth/cadastro/startup").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"%s","password":"Senha@123","name":"%s","segment":"Healthtech","stage":"Ideação",
            "summary":"Telemedicina","terms":true,"privacy":true,"dataProcessing":true}
            """.formatted(email, nome))).andExpect(status().isCreated());
        aprovar(email);
    }

    JsonNode body(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    /** Investidor e startup com conexão firmada; outra startup sem conexão. */
    @BeforeEach
    void conexaoFirmada() throws Exception {
        http.perform(post("/auth/cadastro/investidor").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"%s","password":"Senha@123","name":"Bia Investidora","segments":["Healthtech"],
            "terms":true,"privacy":true,"dataProcessing":true}
            """.formatted(INV))).andExpect(status().isCreated());
        aprovar(INV);
        startup(ST, "TeleSertão");
        startup(OUTRA, "Outra");
        long perfil = users.findByEmail(ST).orElseThrow().getStartup().getId();
        http.perform(post("/matchmaking/investidor/startups/" + perfil + "/decisao").header("Authorization", inv())
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"accepted\"}"))
            .andExpect(status().isOk());
        String pedido = body(http.perform(get("/matchmaking/startup/pedidos").header("Authorization", st())))
                .get(0).get("id").asString();
        http.perform(post("/matchmaking/startup/pedidos/" + pedido + "/decisao").header("Authorization", st())
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"accepted\"}"))
            .andExpect(status().isOk());
        conexao = pedido;
    }

    ResultActions enviar(String token, String texto) throws Exception {
        return http.perform(post("/conversas/" + conexao + "/mensagens").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("text", texto))));
    }

    ResultActions convidar(LocalDate data, String hora, int duracao) throws Exception {
        return http.perform(post("/conversas/" + conexao + "/convites").header("Authorization", inv())
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"date":"%s","time":"%s","durationMinutes":%d,"subject":"Apresentação do pitch",
                    "location":"https://meet.example/abc"}
                    """.formatted(data, hora, duracao)));
    }

    /* ------------------------------------------------------------------ mensagens */

    @Test
    void connectedPartiesExchangeMessagesWithHistoryAndUnreadCount() throws Exception {
        enviar(inv(), "Olá! Gostei muito do projeto.").andExpect(status().isCreated())
            .andExpect(jsonPath("sender").value("me")).andExpect(jsonPath("type").value("TEXTO"));
        enviar(inv(), "Podemos conversar esta semana?").andExpect(status().isCreated());

        http.perform(get("/conversas").header("Authorization", st()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(conexao))
            .andExpect(jsonPath("$[0].name").value("Bia Investidora"))
            .andExpect(jsonPath("$[0].role").value("Investidor(a) anjo"))
            .andExpect(jsonPath("$[0].unread").value(2))
            .andExpect(jsonPath("$[0].lastMessage.text").value("Podemos conversar esta semana?"))
            .andExpect(jsonPath("$[0].lastMessage.mine").value(false));

        // abrir a conversa marca como lidas
        http.perform(get("/conversas/" + conexao + "/mensagens").header("Authorization", st()))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].sender").value("contact"))
            .andExpect(jsonPath("$[0].text").value("Olá! Gostei muito do projeto."));
        http.perform(get("/conversas").header("Authorization", st())).andExpect(jsonPath("$[0].unread").value(0));

        enviar(st(), "Claro, será um prazer.").andExpect(status().isCreated());
        http.perform(get("/conversas/" + conexao + "/mensagens").header("Authorization", inv()))
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].sender").value("me")).andExpect(jsonPath("$[0].read").value(true))
            .andExpect(jsonPath("$[2].sender").value("contact"));
        http.perform(get("/conversas").header("Authorization", inv()))
            .andExpect(jsonPath("$[0].name").value("TeleSertão"))
            .andExpect(jsonPath("$[0].role").value("Startup cadastrada"));
    }

    @Test
    void onlyParticipantsOfConfirmedConnectionsCanChat() throws Exception {
        enviar(outra(), "Oi").andExpect(status().isNotFound());
        http.perform(get("/conversas/" + conexao + "/mensagens").header("Authorization", outra()))
            .andExpect(status().isNotFound());
        http.perform(get("/conversas").header("Authorization", outra())).andExpect(content().json("[]"));
        http.perform(get("/conversas")).andExpect(status().isUnauthorized());
        http.perform(get("/conversas").header("Authorization", "Bearer " + jwt.gerarToken("a@x", "ADMIN")))
            .andExpect(status().isForbidden());
        // rotas antigas, que aceitavam remetente no corpo, não existem mais
        http.perform(post("/mensagens").header("Authorization", inv()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"remetenteId\":1,\"destinatarioId\":2,\"conteudo\":\"x\"}"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void messageTextIsValidated() throws Exception {
        enviar(inv(), "   ").andExpect(status().isBadRequest());
        enviar(inv(), "a".repeat(1001)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("message", containsString("1000")));
        enviar(inv(), "Emoji e acentos: ação 🚀").andExpect(status().isCreated());
    }

    /* ------------------------------------------------------------------- reuniões */

    @Test
    void investorInviteAcceptedByStartupAppearsInBothCalendars() throws Exception {
        LocalDate dia = LocalDate.now().plusDays(3);
        String reuniao = body(convidar(dia, "14:30", 45).andExpect(status().isCreated())
                .andExpect(jsonPath("type").value("CONVITE"))
                .andExpect(jsonPath("meeting.status").value("PENDENTE"))
                .andExpect(jsonPath("meeting.organizer").value(true))
                .andExpect(jsonPath("meeting.canRespond").value(false)))
                .get("meeting").get("id").asString();

        // startup vê o convite no chat, com opção de responder
        http.perform(get("/conversas/" + conexao + "/mensagens").header("Authorization", st()))
            .andExpect(jsonPath("$[0].type").value("CONVITE"))
            .andExpect(jsonPath("$[0].meeting.canRespond").value(true))
            .andExpect(jsonPath("$[0].meeting.subject").value("Apresentação do pitch"));
        // investidor não responde o próprio convite
        http.perform(post("/reunioes/" + reuniao + "/resposta").header("Authorization", inv())
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"accepted\"}"))
            .andExpect(status().isForbidden());

        http.perform(post("/reunioes/" + reuniao + "/resposta").header("Authorization", st())
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"accepted\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("status").value("ACEITO"));

        for (String token : new String[]{inv(), st()}) {
            http.perform(get("/reunioes").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACEITO"))
                .andExpect(jsonPath("$[0].date").value(dia.toString()))
                .andExpect(jsonPath("$[0].time").value(startsWith("14:30")))
                .andExpect(jsonPath("$[0].durationMinutes").value(45))
                .andExpect(jsonPath("$[0].location").value("https://meet.example/abc"))
                .andExpect(jsonPath("$[0].canCancel").value(true));
        }
        http.perform(get("/reunioes").header("Authorization", inv()))
            .andExpect(jsonPath("$[0].contactName").value("TeleSertão"));
        // o aceite gera um aviso no chat
        http.perform(get("/conversas/" + conexao + "/mensagens").header("Authorization", inv()))
            .andExpect(jsonPath("$[1].sender").value("system"))
            .andExpect(jsonPath("$[1].text", startsWith("Reunião confirmada para")));
        // métricas do admin contam a reunião
        http.perform(get("/admin/metricas").header("Authorization", "Bearer " + jwt.gerarToken("a@x", "ADMIN")))
            .andExpect(jsonPath("reunioes").value(1)).andExpect(jsonPath("reunioesAceitas").value(1));
    }

    @Test
    void onlyTheInvestorInvitesAndInvitesAreValidated() throws Exception {
        http.perform(post("/conversas/" + conexao + "/convites").header("Authorization", st())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"" + LocalDate.now().plusDays(2) + "\",\"time\":\"10:00\",\"subject\":\"x\"}"))
            .andExpect(status().isForbidden());
        convidar(LocalDate.now().minusDays(1), "10:00", 60).andExpect(status().isBadRequest());
        convidar(LocalDate.now().plusDays(2), "10:00", 5).andExpect(status().isBadRequest());
        convidar(LocalDate.now().plusMonths(7), "10:00", 60).andExpect(status().isBadRequest());
        // um convite aberto por vez
        convidar(LocalDate.now().plusDays(2), "10:00", 60).andExpect(status().isCreated());
        convidar(LocalDate.now().plusDays(4), "10:00", 60).andExpect(status().isConflict());
    }

    @Test
    void declinedAndCancelledMeetingsAreRecorded() throws Exception {
        String r1 = body(convidar(LocalDate.now().plusDays(2), "09:00", 60)).get("meeting").get("id").asString();
        http.perform(post("/reunioes/" + r1 + "/resposta").header("Authorization", st())
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"rejected\"}"))
            .andExpect(jsonPath("status").value("REJEITADO")).andExpect(jsonPath("canCancel").value(false));
        // depois da recusa o investidor pode convidar de novo
        String r2 = body(convidar(LocalDate.now().plusDays(3), "16:00", 30).andExpect(status().isCreated()))
                .get("meeting").get("id").asString();
        http.perform(post("/reunioes/" + r2 + "/resposta").header("Authorization", st())
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"accepted\"}"));
        http.perform(post("/reunioes/" + r2 + "/cancelar").header("Authorization", st()))
            .andExpect(status().isOk()).andExpect(jsonPath("status").value("CANCELADO"));
        http.perform(post("/reunioes/" + r2 + "/cancelar").header("Authorization", st()))
            .andExpect(status().isConflict());
        http.perform(post("/reunioes/" + r2 + "/cancelar").header("Authorization", outra()))
            .andExpect(status().isNotFound());
        http.perform(get("/conversas/" + conexao + "/mensagens").header("Authorization", inv()))
            .andExpect(jsonPath("$[?(@.sender=='system')].text", hasItem(containsString("cancelada por TeleSertão"))))
            .andExpect(jsonPath("$[?(@.sender=='system')].text", hasItem(startsWith("Convite recusado"))));
    }

    @Test
    void acceptingIsBlockedWhenItOverlapsAnotherConfirmedMeeting() throws Exception {
        LocalDate dia = LocalDate.now().plusDays(5);
        String r1 = body(convidar(dia, "10:00", 60)).get("meeting").get("id").asString();
        http.perform(post("/reunioes/" + r1 + "/resposta").header("Authorization", st())
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"accepted\"}"));
        // novo convite das 10:30 bate com a reunião das 10:00 às 11:00
        convidar(dia, "10:30", 60).andExpect(status().isConflict())
            .andExpect(jsonPath("message", containsString("já tem uma reunião confirmada das 10:00 às 11:00")));
        convidar(dia, "11:00", 60).andExpect(status().isCreated());
    }

    /** Métricas do admin: convites e avisos automáticos não contam como mensagens trocadas. */
    @Test
    void adminMetricsCountOnlyWrittenMessagesAndConfirmedMeetings() throws Exception {
        enviar(inv(), "Olá!").andExpect(status().isCreated());
        enviar(st(), "Oi, tudo bem?").andExpect(status().isCreated());
        String reuniao = body(convidar(LocalDate.now().plusDays(2), "09:00", 30).andExpect(status().isCreated()))
                .get("meeting").get("id").asString();
        convidar(LocalDate.now().plusDays(4), "09:00", 30).andExpect(status().isConflict()); // um convite aberto por vez
        http.perform(post("/reunioes/" + reuniao + "/resposta").header("Authorization", st())
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"accepted\"}"))
            .andExpect(status().isOk());

        http.perform(get("/admin/metricas").header("Authorization", "Bearer " + jwt.gerarToken("adm@chat.test", "ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("mensagens").value(2))          // CONVITE e SISTEMA ficam de fora
            .andExpect(jsonPath("conversas").value(1))
            .andExpect(jsonPath("matches").value(1))
            .andExpect(jsonPath("conexoes").value(1))
            .andExpect(jsonPath("reunioes").value(1))
            .andExpect(jsonPath("reunioesAceitas").value(1))
            .andExpect(jsonPath("reunioesAguardando").value(0))
            .andExpect(jsonPath("matchesComReuniao").value(1))
            .andExpect(jsonPath("startupsAprovadas").value(2))
            .andExpect(jsonPath("engajamento[1].percentual").value(100))
            .andExpect(jsonPath("engajamento[2].percentual").value(50))   // 1 de 2 startups aprovadas conversou
            .andExpect(jsonPath("engajamento[3].percentual").value(100))
            .andExpect(jsonPath("engajamento[4].percentual").value(100));
    }
}
