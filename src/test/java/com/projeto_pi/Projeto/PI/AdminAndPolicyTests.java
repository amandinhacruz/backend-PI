package com.projeto_pi.Projeto.PI;

import com.projeto_pi.Projeto.PI.entity.*;
import com.projeto_pi.Projeto.PI.repository.*;
import com.projeto_pi.Projeto.PI.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Política de senha, CPF/CNPJ do investidor e APIs do painel administrativo. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminAndPolicyTests {

    @Autowired MockMvc http;
    @Autowired UsuarioRepository users;
    @Autowired AuditoriaAdminRepository audit;
    @Autowired JwtService jwt;

    @BeforeEach
    void clean() { audit.deleteAll(); users.deleteAll(); users.flush(); }

    static String startup(String email, String password) {
        return """
            {"email":"%s","password":"%s","name":"Nexa","profileOwner":"Ana",
            "segment":"Fintech","stage":"MVP","cnpj":"12.345.678/0001-00",
            "terms":true,"privacy":true,"dataProcessing":true}
            """.formatted(email, password);
    }

    static String investor(String email, String docField, String doc) {
        return """
            {"email":"%s","password":"Senha@123","name":"Bia","%s":"%s",
            "city":"Recife","state":"PE","segment":"Fintech","stage":"MVP",
            "terms":true,"privacy":true,"dataProcessing":true}
            """.formatted(email, docField, doc);
    }

    String admin() { return "Bearer " + jwt.gerarToken("admin@test.local", "ADMIN"); }

    long register(String email) throws Exception {
        http.perform(post("/auth/cadastro/startup").contentType(MediaType.APPLICATION_JSON)
                .content(startup(email, "Senha@123"))).andExpect(status().isCreated());
        return users.findByEmail(email).orElseThrow().getId();
    }

    // ---------------------------------------------------------------- senha

    @ParameterizedTest
    @ValueSource(strings = {"Ab@1234", "SemNumero@", "SemEspecial1", "12345678"})
    void weakPasswordsAreRejected(String password) throws Exception {
        http.perform(post("/auth/cadastro/startup").contentType(MediaType.APPLICATION_JSON)
                .content(startup("fraca@example.com", password)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value(containsString("8 caracteres")));
        assertEquals(0, users.count());
    }

    @Test
    void loginStillAcceptsLegacyPasswordsOutsideThePolicy() throws Exception {
        // A política vale para novos cadastros; contas existentes (ex.: seed do admin) continuam entrando.
        Usuario legacy = new Usuario();
        legacy.setEmail("legado@example.com");
        legacy.setSenhaHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("admin123"));
        legacy.setPapel(Papel.ADMIN);
        legacy.setStatus(StatusUsuario.APROVADO);
        users.saveAndFlush(legacy);
        http.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"legado@example.com\",\"password\":\"admin123\"}"))
            .andExpect(status().isOk());
    }

    // ------------------------------------------------------------- CPF/CNPJ

    @Test
    void startupCnpjMaskIsStrippedBeforePersisting() throws Exception {
        register("mask@example.com");
        assertEquals("12345678000100", users.findByEmail("mask@example.com").orElseThrow().getStartup().getCnpj());
    }

    @Test
    void investorCanRegisterWithCpfOrCnpj() throws Exception {
        http.perform(post("/auth/cadastro/investidor").contentType(MediaType.APPLICATION_JSON)
                .content(investor("pf@example.com", "cpf", "123.456.789-09"))).andExpect(status().isCreated());
        http.perform(post("/auth/cadastro/investidor").contentType(MediaType.APPLICATION_JSON)
                .content(investor("pj@example.com", "cnpj", "12.345.678/0001-00"))).andExpect(status().isCreated());
        var pf = users.findByEmail("pf@example.com").orElseThrow().getInvestidorMentor();
        var pj = users.findByEmail("pj@example.com").orElseThrow().getInvestidorMentor();
        assertEquals("12345678909", pf.getCpf()); assertNull(pf.getCnpj());
        assertEquals("12345678000100", pj.getCnpj()); assertNull(pj.getCpf());
    }

    @Test
    void investorDocumentWithWrongLengthOrBothIsRejected() throws Exception {
        http.perform(post("/auth/cadastro/investidor").contentType(MediaType.APPLICATION_JSON)
                .content(investor("x@example.com", "cpf", "123.456")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("message").value("CPF deve conter 11 dígitos"));
        http.perform(post("/auth/cadastro/investidor").contentType(MediaType.APPLICATION_JSON)
                .content(investor("y@example.com", "cpf", "12345678909").replace("\"city\"", "\"cnpj\":\"12345678000100\",\"city\"")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("message").value("Informe CPF ou CNPJ, não ambos"));
        assertEquals(0, users.count());
    }

    // ---------------------------------------------------------- painel admin

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        http.perform(get("/admin/usuarios")).andExpect(status().isUnauthorized());
        http.perform(get("/admin/metricas").header("Authorization", "Bearer " + jwt.gerarToken("i@x.com", "INVESTIDOR")))
            .andExpect(status().isForbidden());
    }

    @Test
    void listsRegistrationsWithoutSensitiveData() throws Exception {
        register("lista@example.com");
        http.perform(get("/admin/usuarios").header("Authorization", admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].tipo").value("Startup"))
            .andExpect(jsonPath("$[0].nome").value("Nexa"))
            .andExpect(jsonPath("$[0].responsavel").value("Ana"))
            .andExpect(jsonPath("$[0].status").value("PENDENTE"))
            .andExpect(jsonPath("$[0].enviadoEm").isNotEmpty())
            .andExpect(jsonPath("$[0].senhaHash").doesNotExist())
            .andExpect(jsonPath("$[0].cnpj").doesNotExist());
        http.perform(get("/admin/usuarios").queryParam("status", "aprovado").header("Authorization", admin()))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void moderationUpdatesStatusAndWritesAudit() throws Exception {
        long id = register("moderar@example.com");
        http.perform(put("/admin/usuarios/" + id + "/aprovar").header("Authorization", admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("status").value("APROVADO"))
            .andExpect(jsonPath("senhaHash").doesNotExist());
        assertEquals(StatusUsuario.APROVADO, users.findById(id).orElseThrow().getStatus());
        http.perform(get("/admin/auditoria").header("Authorization", admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].acao").value("Cadastro aprovado"))
            .andExpect(jsonPath("$[0].alvo").value("Nexa"))
            .andExpect(jsonPath("$[0].ator").value("admin@test.local"));
    }

    @Test
    void moderatingUnknownUserReturns404() throws Exception {
        http.perform(put("/admin/usuarios/999999/rejeitar").header("Authorization", admin()))
            .andExpect(status().isNotFound());
    }

    @Test
    void metricsCountRealRegistrations() throws Exception {
        register("m1@example.com");
        long approved = register("m2@example.com");
        http.perform(post("/auth/cadastro/investidor").contentType(MediaType.APPLICATION_JSON)
                .content(investor("inv@example.com", "cpf", "12345678909"))).andExpect(status().isCreated());
        http.perform(put("/admin/usuarios/" + approved + "/aprovar").header("Authorization", admin()));
        http.perform(get("/admin/metricas").header("Authorization", admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("startups").value(2))
            .andExpect(jsonPath("startupsNoMes").value(2))
            .andExpect(jsonPath("investidores").value(1))
            .andExpect(jsonPath("pendentes").value(2))
            .andExpect(jsonPath("conexoes").value(0))
            .andExpect(jsonPath("engajamento.length()").value(5));
    }
}
