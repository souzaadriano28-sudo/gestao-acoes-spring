package com.trabalho.gestao_acoes.resources;

import com.trabalho.gestao_acoes.domains.Acao;
import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.UserAccount;
import com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus;
import com.trabalho.gestao_acoes.repositories.AcaoRepository;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.repositories.UserAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OperacaoResourceOrderingIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired UserAccountRepository users;
    @Autowired AcaoRepository assets;
    @Autowired CorretoraRepository brokers;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void listReturnsPersistedOperationsByDateDescendingThenIdDescendingAndNeverLeaksOtherOwner() throws Exception {
        Session owner = register("operations-order-owner");
        UserAccount ownerUser = users.findByEmail(owner.email()).orElseThrow();
        Acao petr4 = asset(ownerUser, "PETR4", "BRL");
        Acao vale3 = asset(ownerUser, "VALE3", "BRL");
        Corretora firstBroker = broker(ownerUser, "11");
        Corretora secondBroker = broker(ownerUser, "12");

        long oldest = create(owner, petr4, firstBroker, "COMPRA", "2026-02-01T09:00:00", 20);
        long middle = create(owner, vale3, firstBroker, "COMPRA", "2026-02-02T09:00:00", 3);
        long newer = create(owner, petr4, secondBroker, "COMPRA", "2026-02-03T09:00:00", 2);
        long tiedLowerId = create(owner, petr4, firstBroker, "COMPRA", "2026-02-04T10:00:00", 4);
        long tiedHigherId = create(owner, petr4, firstBroker, "VENDA", "2026-02-04T10:00:00", 1);

        Session other = register("operations-order-other");
        UserAccount otherUser = users.findByEmail(other.email()).orElseThrow();
        Acao otherAsset = asset(otherUser, "ABCD", "BRL");
        Corretora otherBroker = broker(otherUser, "13");
        long foreign = create(other, otherAsset, otherBroker, "COMPRA", "2026-12-01T09:00:00", 1);

        JsonNode json = list(owner, "");
        assertThat(ids(json)).containsExactly(tiedHigherId, tiedLowerId, newer, middle, oldest);
        assertThat(ids(json)).doesNotContain(foreign);
        assertThat(ids(list(other, ""))).containsExactly(foreign);
    }

    @Test
    void everySupportedFilterPreservesDescendingDateAndIdOrderInTheHttpJson() throws Exception {
        Session owner = register("operations-filter-owner");
        UserAccount user = users.findByEmail(owner.email()).orElseThrow();
        Acao petr4 = asset(user, "PETR4", "BRL");
        Acao vale3 = asset(user, "VALE3", "BRL");
        Corretora firstBroker = broker(user, "21");
        Corretora secondBroker = broker(user, "22");

        long oldPurchase = create(owner, petr4, firstBroker, "COMPRA", "2026-03-01T09:00:00", 20);
        long valePurchase = create(owner, vale3, firstBroker, "COMPRA", "2026-03-02T09:00:00", 3);
        long secondBrokerPurchase = create(owner, petr4, secondBroker, "COMPRA", "2026-03-03T09:00:00", 2);
        long tiedPurchase = create(owner, petr4, firstBroker, "COMPRA", "2026-03-04T10:00:00", 4);
        long tiedSale = create(owner, petr4, firstBroker, "VENDA", "2026-03-04T10:00:00", 1);

        assertThat(ids(list(owner, "?tipo=COMPRA"))).containsExactly(tiedPurchase, secondBrokerPurchase, valePurchase, oldPurchase);
        assertThat(ids(list(owner, "?ticker=PETR4"))).containsExactly(tiedSale, tiedPurchase, secondBrokerPurchase, oldPurchase);
        assertThat(ids(list(owner, "?corretoraId=" + firstBroker.getId()))).containsExactly(tiedSale, tiedPurchase, valePurchase, oldPurchase);
        assertThat(ids(list(owner, "?de=2026-03-02T00:00:00&ate=2026-03-04T10:00:00"))).containsExactly(tiedSale, tiedPurchase, secondBrokerPurchase, valePurchase);
    }

    private JsonNode list(Session session, String query) throws Exception {
        MvcResult result = mvc.perform(get("/operacoes" + query).session(session.session())).andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private List<Long> ids(JsonNode json) {
        return java.util.stream.StreamSupport.stream(json.spliterator(), false).map(item -> item.get("id").asLong()).toList();
    }

    private long create(Session session, Acao asset, Corretora broker, String type, String dateTime, int quantity) throws Exception {
        String payload = "{\"tipo\":\"" + type + "\",\"ativoId\":" + asset.getId() + ",\"corretoraId\":" + broker.getId()
                + ",\"dataHora\":\"" + dateTime + "\",\"quantidade\":" + quantity
                + ",\"moeda\":\"BRL\",\"precoUnitario\":10,\"corretagem\":0,\"taxas\":0,\"impostos\":0,\"outrosCustos\":0}";
        MvcResult result = mvc.perform(post("/operacoes").session(session.session()).header("X-CSRF-TOKEN", session.token())
                        .contentType("application/json").content(payload))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Session register(String prefix) throws Exception {
        String name = prefix + UUID.randomUUID().toString().substring(0, 8);
        MvcResult csrf = mvc.perform(get("/auth/csrf")).andReturn();
        MockHttpSession session = (MockHttpSession) csrf.getRequest().getSession(false);
        String token = mapper.readTree(csrf.getResponse().getContentAsString()).get("token").asText();
        String payload = "{\"username\":\"" + name + "\",\"email\":\"" + name + "@t.test\",\"password\":\"Academic-test-password-123!\",\"passwordConfirmation\":\"Academic-test-password-123!\",\"termsAccepted\":true}";
        mvc.perform(post("/auth/register").session(session).header("X-CSRF-TOKEN", token).contentType("application/json").content(payload)).andExpect(status().isOk());
        MvcResult renewed = mvc.perform(get("/auth/csrf").session(session)).andReturn();
        return new Session(session, mapper.readTree(renewed.getResponse().getContentAsString()).get("token").asText(), name + "@t.test");
    }

    private Acao asset(UserAccount owner, String ticker, String currency) {
        Acao asset = new Acao();
        asset.setOwner(owner); asset.setTicker(ticker); asset.setMercado("BRASIL"); asset.setMoeda(currency);
        asset.setCotacaoAtual(BigDecimal.TEN); asset.setDataHoraCotacao(LocalDateTime.now());
        return assets.save(asset);
    }

    private Corretora broker(UserAccount owner, String suffix) {
        Corretora broker = new Corretora();
        broker.setOwner(owner); broker.setCnpj("998877665500" + suffix); broker.setRazaoSocial("Corretora " + suffix);
        broker.setCep("01001000"); broker.setRegulatoryStatus(RegulatoryStatus.VERIFIED);
        return brokers.save(broker);
    }

    private record Session(MockHttpSession session, String token, String email) { }
}
