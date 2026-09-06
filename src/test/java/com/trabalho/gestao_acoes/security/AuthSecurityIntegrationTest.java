package com.trabalho.gestao_acoes.security;

import com.trabalho.gestao_acoes.repositories.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class AuthSecurityIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired AdminUserRepository users;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc= MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        users.findByUsername("atlas-test-admin").ifPresent(user -> { user.clearFailures(Instant.now()); users.saveAndFlush(user); });
    }

    @Test
    void anonymousSurfaceIsDenyByDefaultAndCsrfRunsBeforeBusinessLogic() throws Exception {
        for (String path : new String[]{"/acoes","/corretoras","/carteira/posicoes","/carteira/saldo-total",
                "/carteira/dashboard","/carteira/posicoes/detalhadas","/carteira/movimentacoes","/v3/api-docs","/h2-console"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        }
        mvc.perform(post("/carteira/comprar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(post("/corretoras/evidencia-regulatoria/atualizar"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void loginRequiresCsrfRenewsSessionAndLogoutInvalidatesIt() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials("atlas-test-admin","Academic-test-password-123!")))
                .andExpect(status().isForbidden());

        CsrfFixture initial=csrf("10.0.0.11"); String oldId=initial.session().getId();
        MvcResult login=mvc.perform(post("/auth/login").session(initial.session()).header("X-CSRF-TOKEN",initial.token())
                        .with(req -> { req.setRemoteAddr("10.0.0.11"); return req; })
                        .contentType(MediaType.APPLICATION_JSON).content(credentials("atlas-test-admin","Academic-test-password-123!")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.authenticated").value(true)).andExpect(jsonPath("$.username").value("atlas-test-admin"))
                .andExpect(jsonPath("$.password").doesNotExist()).andReturn();
        MockHttpSession authenticated=(MockHttpSession)login.getRequest().getSession(false);
        assertThat(authenticated.getId()).isNotEqualTo(oldId);
        mvc.perform(get("/auth/session").session(authenticated)).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("atlas-test-admin"));
        mvc.perform(get("/acoes").session(authenticated)).andExpect(status().isOk());

        mvc.perform(post("/auth/logout").session(authenticated).header("X-CSRF-TOKEN",initial.token()))
                .andExpect(status().isForbidden());
        String renewed=token(mvc.perform(get("/auth/csrf").session(authenticated)).andExpect(status().isOk()).andReturn());
        assertThat(renewed).isNotEqualTo(initial.token());
        mvc.perform(post("/auth/logout").session(authenticated).header("X-CSRF-TOKEN",renewed))
                .andExpect(status().isNoContent()).andExpect(cookie().maxAge("ATLAS_SESSION",0));
        assertThat(authenticated.isInvalid()).isTrue();
        mvc.perform(get("/acoes")).andExpect(status().isUnauthorized());
    }

    @Test
    void failuresAreGenericAndSixthAttemptIsTemporarilyLimited() throws Exception {
        String origin="10.0.0.22";
        for(int i=0;i<5;i++) {
            CsrfFixture fixture=csrf(origin);
            mvc.perform(post("/auth/login").session(fixture.session()).header("X-CSRF-TOKEN",fixture.token())
                    .with(req->{req.setRemoteAddr(origin);return req;}).contentType(MediaType.APPLICATION_JSON)
                    .content(credentials("atlas-test-admin","wrong-password-value")))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Não foi possível entrar. Verifique os dados ou tente novamente mais tarde."));
        }
        CsrfFixture limited=csrf(origin);
        mvc.perform(post("/auth/login").session(limited.session()).header("X-CSRF-TOKEN",limited.token())
                .with(req->{req.setRemoteAddr(origin);return req;}).contentType(MediaType.APPLICATION_JSON)
                .content(credentials("unknown-administrator","another-wrong-password")))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.message").value("Não foi possível entrar. Verifique os dados ou tente novamente mais tarde."));
    }

    @Test
    void blankAndUnknownCredentialsDoNotEchoSecretsOrIdentifyAccounts(CapturedOutput output) throws Exception {
        CsrfFixture blank=csrf("10.0.0.31");
        String blankBody=mvc.perform(post("/auth/login").session(blank.session()).header("X-CSRF-TOKEN",blank.token())
                .with(req->{req.setRemoteAddr("10.0.0.31");return req;}).contentType(MediaType.APPLICATION_JSON).content(credentials("","")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        CsrfFixture unknown=csrf("10.0.0.32");
        String unknownBody=mvc.perform(post("/auth/login").session(unknown.session()).header("X-CSRF-TOKEN",unknown.token())
                .with(req->{req.setRemoteAddr("10.0.0.32");return req;}).contentType(MediaType.APPLICATION_JSON).content(credentials("absent-user","super-secret-sample")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(mapper.readTree(blankBody).get("message").asText()).isEqualTo(mapper.readTree(unknownBody).get("message").asText());
        assertThat(unknownBody).doesNotContain("absent-user","super-secret-sample","password");
        assertThat(output.getAll()).doesNotContain("absent-user", "super-secret-sample");
    }

    @Test
    void corsAcceptsOnlyConfiguredOriginWithCredentials() throws Exception {
        mvc.perform(options("/auth/login").header("Origin","http://localhost:4200").header("Access-Control-Request-Method","POST"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials","true"));
        mvc.perform(options("/auth/login").header("Origin","https://evil.invalid").header("Access-Control-Request-Method","POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bootstrapStoresOnlyAdaptiveHashAndDoesNotReplaceIt() {
        var user=users.findByUsername("atlas-test-admin").orElseThrow(); String hash=user.getPasswordHash();
        assertThat(hash).startsWith("{").doesNotContain("Academic-test-password-123!");
        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findByUsername("atlas-test-admin").orElseThrow().getPasswordHash()).isEqualTo(hash);
    }

    private CsrfFixture csrf(String origin) throws Exception {
        MvcResult result=mvc.perform(get("/auth/csrf").with(req->{req.setRemoteAddr(origin);return req;})).andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN")).andReturn();
        return new CsrfFixture((MockHttpSession)result.getRequest().getSession(false),token(result));
    }
    private String token(MvcResult result) throws Exception { return mapper.readTree(result.getResponse().getContentAsByteArray()).get("token").asText(); }
    private String credentials(String username,String password) throws Exception { return mapper.writeValueAsString(new Credentials(username,password)); }
    private record Credentials(String username,String password){}
    private record CsrfFixture(MockHttpSession session,String token){}
}
