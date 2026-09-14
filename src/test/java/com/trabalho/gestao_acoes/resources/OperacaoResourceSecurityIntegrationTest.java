package com.trabalho.gestao_acoes.resources;

import com.trabalho.gestao_acoes.domains.*;
import com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus;
import com.trabalho.gestao_acoes.repositories.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class OperacaoResourceSecurityIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired UserAccountRepository users;
    @Autowired PortfolioRepository portfolios;
    @Autowired AcaoRepository acoes;
    @Autowired CorretoraRepository corretoras;
    @Autowired TransacaoRepository transactions;
    @Autowired PosicaoCarteiraRepository positions;

    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void everyOperationEndpointRequiresAuthentication() throws Exception {
        mvc.perform(get("/operacoes")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        for (var request : java.util.List.of(post("/operacoes"), post("/operacoes/previa"),
                post("/operacoes/1/previa"), put("/operacoes/1"), delete("/operacoes/1"))) {
            mvc.perform(request).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
    }

    @Test
    void authenticatedUsersAreIsolatedAndCannotMutateForeignOperation() throws Exception {
        Session a = register("a");
        UserAccount au = users.findByEmail(a.email).orElseThrow();
        Portfolio ap = portfolios.findFirstByOwnerIdOrderByIdAsc(au.getId()).orElseThrow();
        Acao asset = asset(au, "PETR4", "BRL");
        Corretora broker = broker(au, "11111111111111");
        String payload = body(asset.getId(), broker.getId());
        MvcResult created = mvc.perform(post("/operacoes").session(a.session).header("X-CSRF-TOKEN", a.token)
                        .contentType("application/json").content(payload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ticker").value("PETR4")).andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/operacoes").session(a.session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id)).andExpect(jsonPath("$[0].valorTotal").value(21));

        Session b = register("b");
        mvc.perform(get("/operacoes").session(b.session)).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        for (var request : java.util.List.of(post("/operacoes/" + id + "/previa").content(payload),
                put("/operacoes/" + id).content(payload), delete("/operacoes/" + id))) {
            mvc.perform(request.session(b.session).header("X-CSRF-TOKEN", b.token).contentType("application/json"))
                    .andExpect(status().isNotFound());
        }

        var tx = transactions.findByIdAndPortfolioId(id, ap.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(tx.getQuantidade()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(positions.findByAcaoIdAndCorretoraIdAndPortfolioId(asset.getId(), broker.getId(), ap.getId())
                        .orElseThrow().getPrecoMedio()).isEqualByComparingTo("10.50000000");
        org.assertj.core.api.Assertions.assertThat(transactions.findAllByPortfolioId(ap.getId()))
                .extracting(Transacao::getId).containsExactly(id);
    }

    Session register(String prefix) throws Exception {
        String n = prefix + UUID.randomUUID().toString().substring(0, 8);
        MvcResult csrf = mvc.perform(get("/auth/csrf")).andReturn();
        MockHttpSession s = (MockHttpSession) csrf.getRequest().getSession(false);
        String token = mapper.readTree(csrf.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/auth/register").session(s).header("X-CSRF-TOKEN", token).contentType("application/json")
                        .content("{\"username\":\"" + n + "\",\"email\":\"" + n + "@t.test\",\"password\":\"Academic-test-password-123!\",\"passwordConfirmation\":\"Academic-test-password-123!\",\"termsAccepted\":true}"))
                .andExpect(status().isOk());
        MvcResult renewed = mvc.perform(get("/auth/csrf").session(s)).andReturn();
        return new Session(s, mapper.readTree(renewed.getResponse().getContentAsString()).get("token").asText(), n + "@t.test");
    }

    Acao asset(UserAccount user, String ticker, String currency) {
        Acao asset = new Acao();
        asset.setOwner(user);
        asset.setTicker(ticker);
        asset.setMercado("BRASIL");
        asset.setMoeda(currency);
        asset.setCotacaoAtual(BigDecimal.TEN);
        asset.setDataHoraCotacao(LocalDateTime.now());
        return acoes.save(asset);
    }

    Corretora broker(UserAccount user, String cnpj) {
        Corretora broker = new Corretora();
        broker.setOwner(user);
        broker.setCnpj(cnpj);
        broker.setRazaoSocial("B");
        broker.setCep("01001000");
        broker.setRegulatoryStatus(RegulatoryStatus.VERIFIED);
        return corretoras.save(broker);
    }

    String body(Long assetId, Long brokerId) {
        return "{\"tipo\":\"COMPRA\",\"ativoId\":" + assetId + ",\"corretoraId\":" + brokerId
                + ",\"dataHora\":\"2026-01-02T10:00:00\",\"quantidade\":2,\"moeda\":\"BRL\",\"precoUnitario\":10,\"corretagem\":1,\"taxas\":0,\"impostos\":0,\"outrosCustos\":0}";
    }

    record Session(MockHttpSession session, String token, String email) { }
}
