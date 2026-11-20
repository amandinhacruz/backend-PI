package com.projeto_pi.Projeto.PI;

import com.projeto_pi.Projeto.PI.entity.*;
import com.projeto_pi.Projeto.PI.repository.*;
import com.projeto_pi.Projeto.PI.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IntegrationContractTests {
    @org.junit.jupiter.api.BeforeEach
    void isolateAdminSeed() { users.deleteAll(); users.flush(); }
    @Autowired MockMvc http;
    @Autowired UsuarioRepository users;
    @Autowired StartupRepository startups;
    @Autowired InvestidorMentorRepository investors;
    @Autowired JwtService jwt;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    static final String STARTUP = """
        {"email":"startup@example.com","password":"Senha@123","name":"NexaHub",
        "segment":"Fintech","stage":"MVP","city":"Recife","state":"PE",
        "summary":"Pagamentos simples","problem":"Taxas","cnpj":"12345678000100",
        "minimumTicket":"1000","maximumTicket":"2000","terms":true,"privacy":true,"dataProcessing":true}
        """;
    String token(String role) { return "Bearer " + jwt.gerarToken("qa@example.com", role); }
    void register() throws Exception {
        http.perform(post("/auth/cadastro/startup").contentType(MediaType.APPLICATION_JSON).content(STARTUP))
            .andExpect(status().isCreated()).andExpect(jsonPath("status").value("PENDENTE"))
            .andExpect(jsonPath("senhaHash").doesNotExist()).andExpect(jsonPath("password").doesNotExist());
    }
    void approve() {
        Usuario user = users.findByEmail("startup@example.com").orElseThrow();
        user.setStatus(StatusUsuario.APROVADO); users.saveAndFlush(user);
    }
    @Test void registrationPersistsActualReactFieldsAndHashedPassword() throws Exception {
        register();
        Usuario user = users.findByEmail("startup@example.com").orElseThrow();
        assertNotEquals("Senha@123", user.getSenhaHash());
        assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("Senha@123", user.getSenhaHash()));
        assertTrue(user.getConsentiuEm()); assertNotNull(user.getDataConsentimento());
        assertNotNull(user.getVersaoPoliticaConsentida());
        assertEquals("Recife", user.getStartup().getCity());
        assertEquals("Pagamentos simples", user.getStartup().getSummary());
        assertEquals("12345678000100", user.getStartup().getCnpj());
        entityManager.flush();
        assertTrue(jdbc.queryForObject("select cnpj from startups where id = ?",String.class,user.getStartup().getId()).startsWith("ENCv1:"));
        entityManager.clear();
        assertEquals("12345678000100", users.findByEmail("startup@example.com").orElseThrow().getStartup().getCnpj());
    }
    @Test void legacyPortugueseRegistrationStillWorks() throws Exception {
        http.perform(post("/auth/cadastro/startup").contentType(MediaType.APPLICATION_JSON)
            .content(""" 
                {"email":"old@example.com","senha":"Senha@123","nome":"Legada","segmento":"Saúde",
                "estagio":"MVP","consentimentoLGPD":true}
                """))
            .andExpect(status().isCreated());
    }
    @Test void investorReactStepsPersistProfileAndTickets() throws Exception {
        http.perform(post("/auth/cadastro/investidor").contentType(MediaType.APPLICATION_JSON).content("""
            {"corporateEmail":"investor@example.com","password":"Senha@123","name":"Ana",
            "cpf":"12345678900","city":"Recife","state":"PE","segment":"Fintech","stage":"MVP",
            "minimumTicket":"1000","maximumTicket":"5000","aboutInvestor":"Investidora anjo",
            "career":"Tecnologia","lookingFor":"Startups","terms":true,"privacy":true,"dataProcessing":true}
            """))
            .andExpect(status().isCreated()).andExpect(jsonPath("papel").value("INVESTIDOR"));
        var profile = users.findByEmail("investor@example.com").orElseThrow().getInvestidorMentor();
        assertEquals("Ana", profile.getName()); assertEquals(TipoInvestidorMentor.INVESTIDOR, profile.getTipo());
        assertEquals(0, profile.getTicketMax().compareTo(new java.math.BigDecimal("5000")));
    }
    @Test void duplicateEmailIsCaseInsensitiveAndDoesNotCreateOrphan() throws Exception {
        register();
        http.perform(post("/auth/cadastro/startup").contentType(MediaType.APPLICATION_JSON)
            .content(STARTUP.replace("startup@example.com", " STARTUP@EXAMPLE.COM ")))
            .andExpect(status().isConflict());
        assertEquals(1, users.count()); assertEquals(1, startups.count());
    }
    @ParameterizedTest @ValueSource(strings={"consent", "email", "password", "name", "tickets", "confirmation", "terms", "privacy"})
    void invalidRegistrationDoesNotPersist(String scenario) throws Exception {
        String body = switch(scenario) {
            case "consent" -> STARTUP.replace("\"dataProcessing\":true", "\"dataProcessing\":false");
            case "email" -> STARTUP.replace("startup@example.com", "invalid");
            case "password" -> STARTUP.replace("Senha@123", "");
            case "name" -> STARTUP.replace("NexaHub", "");
            case "tickets" -> STARTUP.replace("\"2000\"", "\"500\"");
            case "confirmation" -> STARTUP.replace("\"password\":\"Senha@123\"", "\"password\":\"Senha@123\",\"confirmPassword\":\"different\"");
            default -> STARTUP.replace("\"" + scenario + "\":true", "\"" + scenario + "\":false");
        };
        http.perform(post("/auth/cadastro/startup").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("message").isNotEmpty());
        assertEquals(0, users.count()); assertEquals(0, startups.count());
    }
    @Test void loginAcceptsPasswordAndSenhaAndNormalizesEmail() throws Exception {
        register(); approve();
        for (String field : new String[]{"password","senha"}) {
            var result = http.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\" STARTUP@EXAMPLE.COM \",\""+field+"\":\"Senha@123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("token").isNotEmpty())
                .andExpect(jsonPath("papel").value("STARTUP")).andReturn();
            assertFalse(result.getResponse().getContentAsString().contains("senhaHash"));
        }
    }
    @Test void pendingAndRejectedUsersCannotLogin() throws Exception {
        register();
        for (StatusUsuario state : StatusUsuario.values()) {
            if (state == StatusUsuario.APROVADO) continue;
            var user = users.findByEmail("startup@example.com").orElseThrow(); user.setStatus(state); users.saveAndFlush(user);
            http.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"startup@example.com\",\"password\":\"Senha@123\"}"))
                .andExpect(status().isForbidden());
        }
    }
    @ParameterizedTest @ValueSource(strings={"unknown@example.com", "startup@example.com"})
    void invalidCredentialsReturn401(String email) throws Exception {
        register(); approve();
        http.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\""+email+"\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("message").value("Email ou senha inválidos"));
    }
    @Test void investorCanReadApprovedFeedWithoutPersonalData() throws Exception {
        register();
        http.perform(get("/startups").header("Authorization",token("INVESTIDOR")))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
        approve();
        http.perform(get("/startups").header("Authorization",token("INVESTIDOR")))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("NexaHub"))
            .andExpect(jsonPath("$[0].id").isString()).andExpect(jsonPath("$[0].location").value("Recife/PE"))
            .andExpect(jsonPath("$[0].description").value("Pagamentos simples"))
            .andExpect(jsonPath("$[0].cnpj").doesNotExist()).andExpect(jsonPath("$[0].email").doesNotExist());
    }
    @Test void feedFiltersMatchFrontend() throws Exception {
        register(); approve();
        http.perform(get("/startups").queryParam("search"," neXA ").queryParam("segment","Fintech")
            .queryParam("stage","MVP").queryParam("location","Recife/PE").header("Authorization",token("INVESTIDOR")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        for(String key : new String[]{"search","segment","stage","location"})
            http.perform(get("/startups").queryParam(key,"inexistente").header("Authorization",token("INVESTIDOR")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
    @Test void anonymousAndInvalidTokensCannotReadFeed() throws Exception {
        http.perform(get("/startups")).andExpect(status().isUnauthorized());
        http.perform(get("/startups").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
    }
    @Test void investorCannotWriteStartup() throws Exception {
        http.perform(post("/startups").header("Authorization",token("INVESTIDOR"))
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
    }
    @Test void unapprovedAndMissingProfilesAre404() throws Exception {
        register();
        var id=users.findByEmail("startup@example.com").orElseThrow().getStartup().getId();
        http.perform(get("/startups/"+id).header("Authorization",token("INVESTIDOR"))).andExpect(status().isNotFound());
        approve();
        http.perform(get("/startups/"+id).header("Authorization",token("INVESTIDOR")))
            .andExpect(status().isOk()).andExpect(jsonPath("name").value("NexaHub"));
        http.perform(get("/startups/999999").header("Authorization",token("INVESTIDOR"))).andExpect(status().isNotFound());
    }
    @Test void vitePreflightIsAllowed() throws Exception {
        http.perform(options("/auth/login").header("Origin","http://localhost:5173")
            .header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","content-type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"));
        http.perform(options("/auth/login").header("Origin","https://untrusted.example")
            .header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden());
    }
    @Test void multipartFormFieldsAndFilesArePersisted() throws Exception {
        http.perform(multipart("/auth/cadastro/startup")
            .file(new MockMultipartFile("pitchDeck","pitch.pdf","application/pdf","%PDF-test".getBytes()))
            .file(new MockMultipartFile("businessModelCanvas","canvas.pptx","application/octet-stream","test".getBytes()))
            .param("email","multipart@example.com").param("password","Senha@123")
            .param("nomeStartup","Empresa").param("segmento","Fintech").param("estagio","MVP")
            .param("resumo","Descrição").param("cidade","Recife").param("estado","PE")
            .param("aceitaTermos","on").param("cientePrivacidade","on").param("autorizaTratamento","on"))
            .andExpect(status().isCreated());
        var s=users.findByEmail("multipart@example.com").orElseThrow().getStartup();
        assertArrayEquals("%PDF-test".getBytes(),s.getPitchDeckData());
        assertArrayEquals("test".getBytes(),s.getBusinessCanvasData());
        assertEquals("Descrição",s.getSummary());
        entityManager.flush(); entityManager.clear();
        s = users.findByEmail("multipart@example.com").orElseThrow().getStartup();
        assertArrayEquals("%PDF-test".getBytes(),s.getPitchDeckData());
        var user=users.findByEmail("multipart@example.com").orElseThrow();
        user.setStatus(StatusUsuario.APROVADO); users.saveAndFlush(user);
        http.perform(get("/startups/"+s.getId()+"/materiais/pitch").header("Authorization",token("INVESTIDOR")))
            .andExpect(status().isOk()).andExpect(content().bytes("%PDF-test".getBytes()))
            .andExpect(header().string("X-Content-Type-Options","nosniff"));
    }
    @Test void invalidUploadIsRejectedBeforePersistence() throws Exception {
        http.perform(multipart("/auth/cadastro/startup")
            .file(new MockMultipartFile("pitchDeck","malware.exe","application/octet-stream",new byte[]{1})))
            .andExpect(status().isBadRequest());
        assertEquals(0,users.count());
    }
    @Test void malformedJsonReturnsReadable400() throws Exception {
        http.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("message").isNotEmpty());
    }

    // --- Contrato exato enviado pelo front integrado (src/integration/AuthRoutes.tsx) ---

    @Test void frontendMultipartWithReactFieldNamesAndFiles() throws Exception {
        // Etapas 1-4 da startup com arquivos: o front envia os nomes do React em multipart.
        http.perform(multipart("/auth/cadastro/startup")
            .file(new MockMultipartFile("pitchDeck","pitch.pdf","application/pdf","%PDF-front".getBytes()))
            .param("email","front@example.com").param("password","Senha@123")
            .param("name","Front Startup").param("city","Recife").param("state","PE")
            .param("segment","Saúde").param("stage","Ideação")
            .param("minimumTicket","1000.50").param("maximumTicket","2000")
            .param("summary","Resumo").param("presentationVideo","https://exemplo.com/v")
            .param("terms","true").param("privacy","true").param("dataProcessing","true"))
            .andExpect(status().isCreated()).andExpect(jsonPath("status").value("PENDENTE"));
        approve("front@example.com");
        http.perform(get("/startups").header("Authorization",token("INVESTIDOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Front Startup"))
            .andExpect(jsonPath("$[0].segment").value("Saúde"))
            .andExpect(jsonPath("$[0].stage").value("Ideação"))
            .andExpect(jsonPath("$[0].location").value("Recife/PE"));
        var s = users.findByEmail("front@example.com").orElseThrow().getStartup();
        assertArrayEquals("%PDF-front".getBytes(), s.getPitchDeckData());
    }

    @Test void frontendInvestorJsonWithoutCorporateEmailThenLogin() throws Exception {
        // O front omite corporateEmail e campos vazios; tickets já normalizados.
        http.perform(post("/auth/cadastro/investidor").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"Front.Investor@Example.com","password":"Senha@123","name":"Bia",
            "city":"Recife","state":"PE","segment":"Tecnologia","stage":"Escala",
            "minimumTicket":"1000","maximumTicket":"1500.75",
            "terms":true,"privacy":true,"dataProcessing":true}
            """))
            .andExpect(status().isCreated()).andExpect(jsonPath("email").value("front.investor@example.com"));
        approve("front.investor@example.com");
        http.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"front.investor@example.com\",\"password\":\"Senha@123\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("papel").value("INVESTIDOR"));
    }

    @Test void pendingLoginReturnsMessageShownByFrontend() throws Exception {
        register();
        http.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"startup@example.com\",\"password\":\"Senha@123\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("message").isNotEmpty());
    }

    @Test void feedPreflightAllowsAuthorizationHeader() throws Exception {
        http.perform(options("/startups").header("Origin","http://localhost:5173")
            .header("Access-Control-Request-Method","GET").header("Access-Control-Request-Headers","authorization"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"));
    }

    void approve(String email) {
        Usuario user = users.findByEmail(email).orElseThrow();
        user.setStatus(StatusUsuario.APROVADO); users.saveAndFlush(user);
    }
}
