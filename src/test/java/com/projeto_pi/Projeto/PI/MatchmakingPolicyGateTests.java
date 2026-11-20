package com.projeto_pi.Projeto.PI;

import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.matching.ia.AnalistaAfinidade;
import com.projeto_pi.Projeto.PI.matching.ia.AnalistaAfinidade.ResultadoAnalise;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import com.projeto_pi.Projeto.PI.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Trava de governança (cláusula 5.4 da Política): perfis de quem só aceitou a versão anterior
 * à que trata de IA não são enviados ao analista generativo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "vertice.ia.politica-minima=1.1")
class MatchmakingPolicyGateTests {

    @Autowired MockMvc http;
    @Autowired UsuarioRepository users;
    @Autowired JwtService jwt;
    @MockitoBean AnalistaAfinidade analista;

    void cadastrar(String caminho, String corpo, String email, String versaoPolitica) throws Exception {
        http.perform(post(caminho).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isCreated());
        Usuario u = users.findByEmail(email).orElseThrow();
        u.setStatus(StatusUsuario.APROVADO);
        u.setVersaoPoliticaConsentida(versaoPolitica);
        users.saveAndFlush(u);
    }

    @Test
    void profilesGoToTheAnalystOnlyAfterBothSidesAcceptedTheAiPolicy() throws Exception {
        when(analista.disponivel()).thenReturn(true);
        when(analista.modelo()).thenReturn("modelo-de-teste");
        when(analista.avaliar(any(), anyList())).thenAnswer(chamada -> {
            List<AnalistaAfinidade.PerfilStartup> startups = chamada.getArgument(1);
            return startups.stream().map(s -> new ResultadoAnalise(s.ref(), 90, 90, 90, 90, "ok",
                    List.of(), List.of(), List.of())).toList();
        });
        cadastrar("/auth/cadastro/investidor", """
            {"email":"inv@gate.test","password":"Senha@123","name":"Bia","segments":["Healthtech"],
            "terms":true,"privacy":true,"dataProcessing":true}
            """, "inv@gate.test", "1.1");
        String startup = """
            {"email":"%s","password":"Senha@123","name":"%s","segment":"Healthtech","stage":"Ideação",
            "summary":"Telemedicina","terms":true,"privacy":true,"dataProcessing":true}
            """;
        cadastrar("/auth/cadastro/startup", startup.formatted("antiga@gate.test", "Antiga"), "antiga@gate.test", "1.0");
        String token = "Bearer " + jwt.gerarToken("inv@gate.test", "INVESTIDOR");

        // startup só aceitou a 1.0: aparece no matching, mas com nota local e sem chamada à IA
        http.perform(get("/matchmaking/investidor").header("Authorization", token))
            .andExpect(jsonPath("items.length()").value(1))
            .andExpect(jsonPath("items[0].source").value("LOCAL"));
        verify(analista, never()).avaliar(any(), anyList());

        // outra startup aceitou a 1.1: só ela é enviada
        cadastrar("/auth/cadastro/startup", startup.formatted("nova@gate.test", "Nova"), "nova@gate.test", "1.1");
        http.perform(get("/matchmaking/investidor").header("Authorization", token))
            .andExpect(jsonPath("items[?(@.name=='Nova')].source").value("IA"))
            .andExpect(jsonPath("items[?(@.name=='Antiga')].source").value("LOCAL"));
        verify(analista, times(1)).avaliar(any(), argThat(lista -> lista.size() == 1));
    }
}
