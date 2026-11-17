package com.projeto_pi.Projeto.PI;

import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.matching.ia.AnaliseIndisponivelException;
import com.projeto_pi.Projeto.PI.matching.ia.AnalistaAfinidade;
import com.projeto_pi.Projeto.PI.matching.ia.AnalistaAfinidade.Dossie;
import com.projeto_pi.Projeto.PI.matching.ia.AnalistaAfinidade.PerfilInvestidor;
import com.projeto_pi.Projeto.PI.matching.ia.AnalistaAfinidade.PerfilStartup;
import com.projeto_pi.Projeto.PI.matching.ia.AnalistaAfinidade.ResultadoAnalise;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import com.projeto_pi.Projeto.PI.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Camada do analista generativo, com o modelo simulado: nota, cache, falha, privacidade e dossiê. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MatchmakingIaTests {

    @Autowired MockMvc http;
    @Autowired UsuarioRepository users;
    @Autowired JwtService jwt;
    @MockitoBean AnalistaAfinidade analista;

    static final String INV = "inv@ia.test";

    @BeforeEach
    void analistaLigado() {
        when(analista.disponivel()).thenReturn(true);
        when(analista.modelo()).thenReturn("modelo-de-teste");
        // nota alta para quem fala de telemedicina, baixa para o resto
        when(analista.avaliar(any(), anyList())).thenAnswer(chamada -> {
            List<PerfilStartup> startups = chamada.getArgument(1);
            return startups.stream().map(s -> new ResultadoAnalise(s.ref(),
                    s.resumo().contains("telemedicina") ? 95 : 20, 90, 80, 70,
                    "Justificativa do analista para " + s.ref(),
                    List.of("Forte 1", "Forte 2"), List.of("Atenção 1"), List.of("Pergunta 1?"))).toList();
        });
    }

    void startup(String email, String nome, String resumo) throws Exception {
        http.perform(post("/auth/cadastro/startup").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"%s","password":"Senha@123","name":"%s","profileOwner":"Responsavel Secreto",
            "cnpj":"12345678000100","website":"https://segredo.example",
            "segment":"Healthtech","stage":"Ideação","city":"Recife","state":"PE",
            "minimumTicket":80000,"maximumTicket":150000,"summary":"%s",
            "terms":true,"privacy":true,"dataProcessing":true}
            """.formatted(email, nome, resumo))).andExpect(status().isCreated());
        aprovar(email);
    }

    void investidor() throws Exception {
        http.perform(post("/auth/cadastro/investidor").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"%s","password":"Senha@123","name":"Nome Secreto","cpf":"12345678909",
            "linkedin":"https://linkedin.com/in/secreto","city":"Recife","state":"PE",
            "segments":["Healthtech"],"stages":["Ideação"],"minimumTicket":50000,"maximumTicket":200000,
            "lookingFor":"Saúde digital","terms":true,"privacy":true,"dataProcessing":true}
            """.formatted(INV))).andExpect(status().isCreated());
        aprovar(INV);
    }

    void aprovar(String email) {
        Usuario u = users.findByEmail(email).orElseThrow();
        u.setStatus(StatusUsuario.APROVADO);
        users.saveAndFlush(u);
    }

    String inv() { return "Bearer " + jwt.gerarToken(INV, "INVESTIDOR"); }

    ResultActions fila() throws Exception {
        return http.perform(get("/matchmaking/investidor").header("Authorization", inv()));
    }

    @Test
    void analystScoreReordersQueueAndExplainsIt() throws Exception {
        investidor();
        startup("generica@s.test", "Generica", "Aplicativo de agendamento para clínicas");
        startup("tele@s.test", "Tele", "Plataforma de telemedicina para o interior");

        fila().andExpect(status().isOk())
            .andExpect(jsonPath("ai.enabled").value(true)).andExpect(jsonPath("ai.pending").value(0))
            .andExpect(jsonPath("ai.model").value("modelo-de-teste"))
            .andExpect(jsonPath("items[0].name").value("Tele"))
            .andExpect(jsonPath("items[0].source").value("IA"))
            .andExpect(jsonPath("items[0].breakdown.ai").value(95))
            .andExpect(jsonPath("items[0].summary", startsWith("Justificativa do analista")))
            .andExpect(jsonPath("items[0].strengths", contains("Forte 1", "Forte 2")))
            .andExpect(jsonPath("items[0].concerns", contains("Atenção 1")))
            .andExpect(jsonPath("items[0].questions", contains("Pergunta 1?")))
            .andExpect(jsonPath("items[1].name").value("Generica"))
            .andExpect(jsonPath("items[1].affinity").value(lessThan(60)));
    }

    @Test
    void analysisIsCachedUntilAProfileChanges() throws Exception {
        investidor();
        startup("tele@s.test", "Tele", "Plataforma de telemedicina para o interior");
        fila();
        fila();
        verify(analista, times(1)).avaliar(any(), anyList());

        // o perfil da startup mudou: a análise guardada deixa de valer
        Usuario u = users.findByEmail("tele@s.test").orElseThrow();
        u.getStartup().setSummary("Agora vendemos software de gestão");
        users.saveAndFlush(u);
        fila().andExpect(jsonPath("items[0].breakdown.ai").value(20));
        verify(analista, times(2)).avaliar(any(), anyList());
    }

    @Test
    void onlyAnonymousBusinessDataReachesTheAnalyst() throws Exception {
        investidor();
        startup("tele@s.test", "NomeDaStartupSecreta", "Plataforma de telemedicina para o interior");
        fila();

        ArgumentCaptor<PerfilInvestidor> perfilInvestidor = ArgumentCaptor.forClass(PerfilInvestidor.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PerfilStartup>> perfis = ArgumentCaptor.forClass(List.class);
        verify(analista).avaliar(perfilInvestidor.capture(), perfis.capture());
        String enviado = perfilInvestidor.getValue().toString() + perfis.getValue().toString();
        for (String proibido : List.of("NomeDaStartupSecreta", "Nome Secreto", "Responsavel Secreto",
                "12345678000100", "12345678909", "segredo.example", "linkedin", "tele@s.test", INV)) {
            assertFalse(enviado.contains(proibido), "dado pessoal enviado ao analista: " + proibido);
        }
        assertTrue(enviado.contains("telemedicina"));
    }

    @Test
    void analystFailureFallsBackToLocalScoreWithoutError() throws Exception {
        investidor();
        startup("tele@s.test", "Tele", "Plataforma de telemedicina para o interior");
        when(analista.avaliar(any(), anyList())).thenThrow(new AnaliseIndisponivelException("fora do ar"));

        fila().andExpect(status().isOk())
            .andExpect(jsonPath("items[0].source").value("LOCAL"))
            .andExpect(jsonPath("items[0].breakdown.ai").value(nullValue()))
            .andExpect(jsonPath("ai.pending").value(0));
        // pausa depois da falha: não insiste a cada requisição
        fila();
        verify(analista, times(1)).avaliar(any(), anyList());
        http.perform(get("/admin/ia/status").header("Authorization", "Bearer " + jwt.gerarToken("a@x", "ADMIN")))
            .andExpect(jsonPath("ultimoErro", containsString("fora do ar")));
    }

    @Test
    void dossierUsesAnalystAndIsStored() throws Exception {
        investidor();
        startup("tele@s.test", "Tele", "Plataforma de telemedicina para o interior");
        when(analista.dossie(any(), any())).thenReturn(new Dossie("Resumo gerado", "Aderência gerada",
                List.of("Oportunidade"), List.of("Risco"), List.of("Pergunta?"), List.of("Passo")));
        long id = users.findByEmail("tele@s.test").orElseThrow().getStartup().getId();
        for (int i = 0; i < 2; i++) {
            http.perform(get("/matchmaking/investidor/startups/" + id + "/dossie").header("Authorization", inv()))
                .andExpect(status().isOk()).andExpect(jsonPath("source").value("IA"))
                .andExpect(jsonPath("executiveSummary").value("Resumo gerado"))
                .andExpect(jsonPath("risks", contains("Risco")));
        }
        verify(analista, times(1)).dossie(any(), any());
    }
}
