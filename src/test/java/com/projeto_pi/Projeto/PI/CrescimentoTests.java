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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Aba "Crescimento": configuração, atualizações mensais, indicadores calculados e acesso. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CrescimentoTests {

    @Autowired MockMvc http;
    @Autowired UsuarioRepository users;
    @Autowired JwtService jwt;

    static final String INV = "inv@crescimento.test";
    static final String ST = "st@crescimento.test";
    static final String OUTRA = "outra@crescimento.test";
    static final String PEND = "pend@crescimento.test";
    static final byte[] PDF = "%PDF-1.4 teste".getBytes();

    static final String M2 = YearMonth.now().minusMonths(2).toString();
    static final String M1 = YearMonth.now().minusMonths(1).toString();

    String token(String email, String papel) { return "Bearer " + jwt.gerarToken(email, papel); }
    String inv() { return token(INV, "INVESTIDOR"); }
    String st() { return token(ST, "STARTUP"); }
    long perfil(String email) { return users.findByEmail(email).orElseThrow().getStartup().getId(); }

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
            {"email":"%s","password":"Senha@123","name":"Ana","segments":["Fintech"],
            "terms":true,"privacy":true,"dataProcessing":true}
            """.formatted(INV))).andExpect(status().isCreated());
        aprovar(INV);
        startup(ST, "CrediFeira");
        aprovar(ST);
        startup(OUTRA, "Outra");
        aprovar(OUTRA);
        startup(PEND, "Pendente");
    }

    ResultActions configurar(String corpo) throws Exception {
        return http.perform(put("/crescimento/configuracao").header("Authorization", st())
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    ResultActions enviar(String mes, String corpo) throws Exception {
        return http.perform(put("/crescimento/atualizacoes/" + mes).header("Authorization", st())
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    static final String SEM_FATURAMENTO = """
        {"mode":"SEM_FATURAMENTO","businessModel":"B2B","indicators":["Pilotos ativos","Cartas de intenção"],
         "milestone":"Lançar o MVP","milestoneDeadline":"2027-03"}""";

    @Test
    void firstVisitHasNoSettingsAndUpdatesNeedConfiguration() throws Exception {
        http.perform(get("/crescimento").header("Authorization", st()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("settings").value(nullValue()))
            .andExpect(jsonPath("updates", hasSize(0)))
            .andExpect(jsonPath("owner").value(true));
        enviar(M1, "{\"cash\":1000}").andExpect(status().isConflict());
    }

    @Test
    void revenueStartupGetsFinancialIndicatorsAndInvestorSeesThem() throws Exception {
        configurar("{\"mode\":\"COM_FATURAMENTO\"}").andExpect(status().isBadRequest()); // falta o critério
        configurar("{\"mode\":\"COM_FATURAMENTO\",\"revenueCriterion\":\"RECEBIDA\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("settings.revenueCriterion").value("RECEBIDA"))
            .andExpect(jsonPath("nextMonth").value(M1));

        enviar(M2, """
            {"revenue":40000,"directCosts":10000,"operatingExpenses":45000,"cash":300000}""")
            .andExpect(status().isOk());
        enviar(M1, """
            {"revenue":50000,"directCosts":12000,"operatingExpenses":48000,"cash":290000,
             "newFunding":true,"fundingAmount":500000,"comment":"Fechamos dois contratos"}""")
            .andExpect(status().isOk())
            .andExpect(jsonPath("updates", hasSize(2)))
            .andExpect(jsonPath("pendingMonths", hasSize(0)))
            .andExpect(jsonPath("updates[1].operatingResult").value(-10000.0))
            .andExpect(jsonPath("updates[1].burn").value(10000.0))
            .andExpect(jsonPath("updates[1].runwayMonths").value(29.0))
            .andExpect(jsonPath("updates[1].grossProfit").value(38000.0))
            .andExpect(jsonPath("updates[1].grossMarginPercent").value(76.0))
            .andExpect(jsonPath("updates[1].revenueGrowthPercent").value(25.0))
            .andExpect(jsonPath("updates[1].revenueCriterion").value("RECEBIDA"));

        http.perform(get("/startups/" + perfil(ST) + "/crescimento").header("Authorization", inv()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("owner").value(false))
            .andExpect(jsonPath("startupName").value("CrediFeira"))
            .andExpect(jsonPath("pendingMonths", hasSize(0)))
            .andExpect(jsonPath("updates[1].newFunding").value(true))
            .andExpect(jsonPath("updates[1].fundingAmount").value(500000.0))
            .andExpect(jsonPath("updates[1].comment").value("Fechamos dois contratos"));
    }

    @Test
    void profitableMonthHasNoBurnAndNoRunway() throws Exception {
        configurar("{\"mode\":\"COM_FATURAMENTO\",\"revenueCriterion\":\"FATURADA\"}");
        enviar(M1, "{\"revenue\":100000,\"directCosts\":20000,\"operatingExpenses\":30000,\"cash\":80000}")
            .andExpect(jsonPath("updates[0].operatingResult").value(50000.0))
            .andExpect(jsonPath("updates[0].burn").value(0.0))
            .andExpect(jsonPath("updates[0].runwayMonths").value(nullValue()));
    }

    @Test
    void preRevenueStartupTracksTractionMilestoneAndRunway() throws Exception {
        configurar("""
            {"mode":"SEM_FATURAMENTO","businessModel":"B2B","indicators":["Pilotos ativos"],"milestone":"MVP"}""")
            .andExpect(status().isBadRequest()); // só um indicador
        configurar("""
            {"mode":"SEM_FATURAMENTO","businessModel":"B2B","indicators":["A","B","C","D"],"milestone":"Lançar o MVP"}""")
            .andExpect(status().isBadRequest()); // quatro indicadores
        configurar(SEM_FATURAMENTO)
            .andExpect(status().isOk())
            .andExpect(jsonPath("settings.indicators", contains("Pilotos ativos", "Cartas de intenção")))
            .andExpect(jsonPath("settings.milestoneDeadline").value("2027-03"));

        // falta um indicador
        enviar(M1, """
            {"expenses":20000,"cash":120000,"indicators":{"Pilotos ativos":2},"milestoneStatus":"EM_ANDAMENTO","milestoneProgress":40}""")
            .andExpect(status().isBadRequest());
        // indicador que não foi configurado
        enviar(M1, """
            {"expenses":20000,"cash":120000,"indicators":{"Pilotos ativos":2,"Cartas de intenção":1,"Outro":3},
             "milestoneStatus":"EM_ANDAMENTO","milestoneProgress":40}""")
            .andExpect(status().isBadRequest());

        enviar(M1, """
            {"expenses":20000,"cash":120000,"indicators":{"Pilotos ativos":2,"Cartas de intenção":1},
             "milestoneStatus":"EM_ANDAMENTO","milestoneProgress":40,"newDebt":true,"debtAmount":50000}""")
            .andExpect(status().isOk())
            .andExpect(jsonPath("updates[0].mode").value("SEM_FATURAMENTO"))
            .andExpect(jsonPath("updates[0].burn").value(20000.0))
            .andExpect(jsonPath("updates[0].runwayMonths").value(6.0))
            .andExpect(jsonPath("updates[0].operatingResult").value(-20000.0))
            .andExpect(jsonPath("updates[0].indicators['Pilotos ativos']").value(2))
            .andExpect(jsonPath("updates[0].milestone").value("Lançar o MVP"))
            .andExpect(jsonPath("updates[0].milestoneProgress").value(40))
            .andExpect(jsonPath("updates[0].newDebt").value(true))
            .andExpect(jsonPath("updates[0].revenue").value(nullValue()));
    }

    @Test
    void switchingToRevenueKeepsHistoryAndChangesNextUpdates() throws Exception {
        configurar(SEM_FATURAMENTO);
        enviar(M2, """
            {"expenses":15000,"cash":100000,"indicators":{"Pilotos ativos":3,"Cartas de intenção":2},
             "milestoneStatus":"CONCLUIDO","milestoneProgress":10}""")
            .andExpect(jsonPath("updates[0].milestoneProgress").value(100)); // concluído = 100%
        configurar("{\"mode\":\"COM_FATURAMENTO\",\"revenueCriterion\":\"FATURADA\"}")
            .andExpect(jsonPath("settings.mode").value("COM_FATURAMENTO"));
        // agora o mês pede receita
        enviar(M1, "{\"expenses\":15000,\"cash\":100000}").andExpect(status().isBadRequest());
        enviar(M1, "{\"revenue\":8000,\"directCosts\":2000,\"operatingExpenses\":16000,\"cash\":90000}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("updates[*].mode", contains("SEM_FATURAMENTO", "COM_FATURAMENTO")))
            .andExpect(jsonPath("updates[1].revenueGrowthPercent").value(nullValue())); // mês anterior sem receita
    }

    @Test
    void sameMonthIsCorrectedNotDuplicated() throws Exception {
        configurar("{\"mode\":\"COM_FATURAMENTO\",\"revenueCriterion\":\"FATURADA\"}");
        enviar(M1, "{\"revenue\":1000,\"directCosts\":0,\"operatingExpenses\":0,\"cash\":10}");
        enviar(M1, "{\"revenue\":2000,\"directCosts\":0,\"operatingExpenses\":0,\"cash\":10}")
            .andExpect(jsonPath("updates", hasSize(1)))
            .andExpect(jsonPath("updates[0].revenue").value(2000.0));
    }

    @Test
    void invalidUpdatesAreRejected() throws Exception {
        configurar("{\"mode\":\"COM_FATURAMENTO\",\"revenueCriterion\":\"FATURADA\"}");
        String ok = "\"revenue\":1000,\"directCosts\":100,\"operatingExpenses\":100,\"cash\":500";
        enviar(YearMonth.now().plusMonths(1).toString(), "{" + ok + "}").andExpect(status().isBadRequest());
        enviar("2026-13", "{" + ok + "}").andExpect(status().isBadRequest());
        enviar(M1, "{\"revenue\":-1,\"directCosts\":100,\"operatingExpenses\":100,\"cash\":500}")
            .andExpect(status().isBadRequest());
        enviar(M1, "{" + ok + ",\"newFunding\":true}").andExpect(status().isBadRequest());
        enviar(M1, "{" + ok + ",\"comment\":\"" + "a".repeat(1001) + "\"}").andExpect(status().isBadRequest());
        enviar(M1, "{\"directCosts\":100,\"operatingExpenses\":100,\"cash\":500}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value(containsString("receita")));
    }

    @Test
    void accessRules() throws Exception {
        configurar("{\"mode\":\"COM_FATURAMENTO\",\"revenueCriterion\":\"FATURADA\"}");
        long id = perfil(ST);
        // investidor não edita o painel de ninguém
        http.perform(get("/crescimento").header("Authorization", inv())).andExpect(status().isForbidden());
        // outra startup não vê o painel de uma concorrente; a própria vê
        http.perform(get("/startups/" + id + "/crescimento").header("Authorization", token(OUTRA, "STARTUP")))
            .andExpect(status().isForbidden());
        http.perform(get("/startups/" + id + "/crescimento").header("Authorization", st()))
            .andExpect(status().isOk()).andExpect(jsonPath("owner").value(true));
        // startup pendente: nem painel próprio nem visível
        http.perform(get("/crescimento").header("Authorization", token(PEND, "STARTUP"))).andExpect(status().isForbidden());
        http.perform(get("/startups/" + perfil(PEND) + "/crescimento").header("Authorization", inv()))
            .andExpect(status().isNotFound());
        int anonimo = http.perform(get("/startups/" + id + "/crescimento")).andReturn().getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertTrue(anonimo == 401 || anonimo == 403);
    }
}
