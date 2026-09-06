package com.trabalho.gestao_acoes.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trabalho.gestao_acoes.services.CarteiraService;
import com.trabalho.gestao_acoes.services.PortfolioReadService;
import com.trabalho.gestao_acoes.domains.dtos.portfolio.*;
import com.trabalho.gestao_acoes.domains.enums.Availability;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.ConflictException;
import com.trabalho.gestao_acoes.services.exceptions.InvalidQuoteException;
import com.trabalho.gestao_acoes.services.exceptions.NotFoundException;
import com.trabalho.gestao_acoes.resources.exceptions.ResourceExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

class CarteiraResourceContractTest {
    private CarteiraService service;
    private PortfolioReadService reads;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(CarteiraService.class);
        reads = mock(PortfolioReadService.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new CarteiraResource(service, reads))
                .setControllerAdvice(new ResourceExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @Test
    void dashboardContractUsesNumericMoneyIsoCurrencyAndUtcInstant() throws Exception {
        MoneyMetricDTO value = MoneyMetricDTO.available(new BigDecimal("1250.40"), "BRL");
        when(reads.dashboard()).thenReturn(new DashboardDTO(Instant.parse("2026-09-06T12:00:00Z"), "BRL", 1,
                value, value, value, PercentageMetricDTO.available(new BigDecimal("1.2500")),
                List.of(), List.of(), List.of(), new ExchangeProvenanceDTO(Availability.UNAVAILABLE,
                "USD", "BRL", null, null, null, null, null, null, "NOT_REQUIRED")));

        mvc.perform(get("/carteira/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf").value("2026-09-06T12:00:00Z"))
                .andExpect(jsonPath("$.presentationCurrency").value("BRL"))
                .andExpect(jsonPath("$.patrimony.value").value(1250.40))
                .andExpect(jsonPath("$.patrimony.availability").value("AVAILABLE"));
    }

    @Test
    void detailedAndMovementEndpointsForwardPagingAndFilters() throws Exception {
        when(reads.detailedPositions(1, 10, "BRASIL", 7L)).thenReturn(new PageDTO<>(List.of(), 1, 10, 0, 0));
        when(reads.movements(0, 5, "COMPRA", "PETR4", 7L, null, null)).thenReturn(new PageDTO<>(List.of(), 0, 5, 0, 0));
        mvc.perform(get("/carteira/posicoes/detalhadas?page=1&size=10&market=BRASIL&brokerId=7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(get("/carteira/movimentacoes?size=5&type=COMPRA&ticker=PETR4&brokerId=7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void readContractsExposeNativeCurrenciesProvenanceAndOffsetTimestamps() throws Exception {
        var provenance = new QuoteProvenanceDTO(Availability.AVAILABLE, "MARKET_DATA_PROVIDER", "BRAPI",
                Instant.parse("2026-09-06T11:58:00Z"), Instant.parse("2026-09-06T12:00:00Z"),
                "FETCH_TIME_PROXY", "BRL", null);
        var position = new DetailedPositionDTO(11L, 1L, "PETR4", "BRASIL", 7L, "Corretora Teste", 2, "BRL",
                MoneyMetricDTO.available(new BigDecimal("18.10"), "BRL"),
                MoneyMetricDTO.available(new BigDecimal("36.20"), "BRL"),
                MoneyMetricDTO.available(new BigDecimal("20.00"), "BRL"),
                MoneyMetricDTO.available(new BigDecimal("40.00"), "BRL"),
                MoneyMetricDTO.available(new BigDecimal("3.80"), "BRL"), provenance);
        var movement = new MovementDTO(31L, "COMPRA", 1L, "PETR4", "BRASIL", 7L, "Corretora Teste", 2,
                MoneyMetricDTO.available(new BigDecimal("18.10"), "BRL"),
                OffsetDateTime.parse("2026-09-05T15:30:00-03:00"), "LEGACY_SERVER_ZONE:America/Sao_Paulo",
                new QuoteProvenanceDTO(Availability.UNAVAILABLE, null, null, null, null, null, "BRL",
                        "HISTORICAL_QUOTE_NOT_RECORDED"));
        when(reads.detailedPositions(0, 20, null, null)).thenReturn(new PageDTO<>(List.of(position), 0, 20, 1, 1));
        when(reads.movements(0, 20, null, null, null, null, null)).thenReturn(new PageDTO<>(List.of(movement), 0, 20, 1, 1));

        mvc.perform(get("/carteira/posicoes/detalhadas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].nativeCurrency").value("BRL"))
                .andExpect(jsonPath("$.items[0].marketValue.value").value(40.00))
                .andExpect(jsonPath("$.items[0].quoteProvenance.referenceAt").value("2026-09-06T11:58:00Z"))
                .andExpect(jsonPath("$.items[0].quoteProvenance.referenceKind").value("FETCH_TIME_PROXY"));
        mvc.perform(get("/carteira/movimentacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice.currency").value("BRL"))
                .andExpect(jsonPath("$.items[0].recordedAt").value("2026-09-05T15:30:00-03:00"))
                .andExpect(jsonPath("$.items[0].timeBasis").value("LEGACY_SERVER_ZONE:America/Sao_Paulo"))
                .andExpect(jsonPath("$.items[0].historicalQuoteProvenance.availability").value("UNAVAILABLE"));
    }

    @Test
    void wrongJsonTypesDecimalsAndOutOfRangeIntegersAreMalformedWithoutSideEffects() throws Exception {
        for (String qtd : new String[]{"\"10\"", "10.0", "1.5", "999999999999999999999"}) {
            mvc.perform(post("/carteira/comprar").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\",\"qtd\":" + qtd + ",\"corretoraId\":1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                    .andExpect(jsonPath("$.path").value("/carteira/comprar"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void missingBlankAndNonPositiveFieldsReturnAllValidationErrors() throws Exception {
        mvc.perform(post("/carteira/comprar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\" \",\"mercado\":\"\",\"qtd\":0,\"corretoraId\":-1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));
        verifyNoInteractions(service);
    }

    @Test
    void successfulPurchaseAndSaleKeepTheExistingEmptyBodyContract() throws Exception {
        String body = "{\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\",\"qtd\":10,\"corretoraId\":1}";
        mvc.perform(post("/carteira/comprar").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(content().string(""));
        mvc.perform(post("/carteira/vender").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(content().string(""));
    }

    @Test
    void legacyReadsKeepPayloadAndAdvertiseSuccessorsWithoutPrematureSunset() throws Exception {
        when(service.calcularSaldoTotal()).thenReturn(new BigDecimal("123.45"));
        when(service.listarPosicoes()).thenReturn(List.of());

        mvc.perform(get("/carteira/saldo-total"))
                .andExpect(status().isOk()).andExpect(content().string("123.45"))
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Link", "</carteira/dashboard>; rel=\"successor-version\""))
                .andExpect(header().doesNotExist("Sunset"));
        mvc.perform(get("/carteira/posicoes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Link", "</carteira/posicoes/detalhadas>; rel=\"successor-version\""))
                .andExpect(header().doesNotExist("Sunset"));
    }

    @Test
    void versionedContractAndLegacyFixturesAreValidJsonAndCoverPublicOperations() throws Exception {
        var schema = new ObjectMapper().readTree(Files.readAllBytes(Path.of("docs/contracts/portfolio-read-model.schema.json")));
        var fixtures = new ObjectMapper().readTree(Files.readAllBytes(Path.of("docs/contracts/legacy-contract-fixtures.json")));
        assertThat(schema.path("$schema").asText()).contains("2020-12");
        assertThat(schema.path("$defs").has("dashboard")).isTrue();
        assertThat(schema.path("$defs").has("movement")).isTrue();
        assertThat(fixtures.has("balance")).isTrue();
        assertThat(fixtures.has("buyRequest")).isTrue();
        assertThat(fixtures.has("sellRequest")).isTrue();
    }

    @Test
    void classifiedFailuresUseTheStandardEnvelope() throws Exception {
        String body = "{\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\",\"qtd\":1,\"corretoraId\":1}";
        assertFailure(body, new BusinessException("INSUFFICIENT_POSITION", "insuficiente"), 422, "INSUFFICIENT_POSITION");
        assertFailure(body, new NotFoundException("ausente"), 404, "RESOURCE_NOT_FOUND");
        assertFailure(body, new ConflictException("CONCURRENT_OPERATION", "conflito"), 409, "CONCURRENT_OPERATION");
        assertFailure(body, new InvalidQuoteException("cotação inválida"), 502, "INVALID_QUOTE");
    }

    @Test
    void unsupportedMethodAndContentTypeKeepTheirHttpSemantics() throws Exception {
        mvc.perform(get("/carteira/comprar"))
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        mvc.perform(post("/carteira/comprar").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    private void assertFailure(String body, RuntimeException error, int status, String code) throws Exception {
        reset(service);
        doThrow(error).when(service).comprar(any(), any(), any(), any());
        mvc.perform(post("/carteira/comprar").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.timestamp").exists()).andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists()).andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
