package com.projeto_pi.Projeto.PI;

import com.projeto_pi.Projeto.PI.service.JwtService;
import com.projeto_pi.Projeto.PI.service.PerfilService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Regressão dos bugs da edição de perfil:
 * - DELETE barrado pelo CORS (remover foto e materiais falhava no navegador);
 * - erro inesperado chegando ao navegador como 403 "Acesso não permitido";
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrosECorsTests {

    @Autowired MockMvc http;
    @Autowired JwtService jwt;
    @MockitoBean PerfilService perfis;

    static final String ORIGEM = "http://localhost:5173";

    String token(String email, String papel) { return "Bearer " + jwt.gerarToken(email, papel); }

    @Test
    void corsAllowsDeleteForPhotoAndMaterials() throws Exception {
        for (String rota : new String[]{"/perfil/foto", "/perfil/materiais/pitch"}) {
            http.perform(options(rota)
                    .header("Origin", ORIGEM)
                    .header("Access-Control-Request-Method", "DELETE")
                    .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("DELETE")));
        }
    }

    @Test
    void unexpectedErrorIsA500WithMessageNotAccessDenied() throws Exception {
        when(perfis.proprio(any())).thenThrow(new IllegalStateException("falha simulada"));
        http.perform(get("/perfil").header("Authorization", token("inv@erro.test", "INVESTIDOR")))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("message").value(containsString("Erro inesperado")));
    }

    @Test
    void errorPageIsNotBlockedBySecurity() throws Exception {
        int codigo = http.perform(get("/error")).andReturn().getResponse().getStatus();
        assertTrue(codigo != 401 && codigo != 403, "/error respondeu " + codigo);
    }

    @Test
    void unknownRouteStays404AndWrongMethodStays405() throws Exception {
        http.perform(get("/rota-que-nao-existe").header("Authorization", token("inv@erro.test", "INVESTIDOR")))
            .andExpect(status().isNotFound());
        http.perform(delete("/perfil/investidor").header("Authorization", token("inv@erro.test", "INVESTIDOR")))
            .andExpect(status().isMethodNotAllowed());
    }
}
