package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Acao;
import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.dtos.AcaoDTO;
import com.trabalho.gestao_acoes.domains.dtos.OperationDTO;
import com.trabalho.gestao_acoes.domains.dtos.OperationRequestDTO;
import com.trabalho.gestao_acoes.domains.enums.TipoTransacao;
import com.trabalho.gestao_acoes.resources.AcaoResource;
import com.trabalho.gestao_acoes.resources.exceptions.ResourceExceptionHandler;
import com.trabalho.gestao_acoes.repositories.AcaoRepository;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.repositories.PosicaoCarteiraRepository;
import com.trabalho.gestao_acoes.repositories.TransacaoRepository;
import com.trabalho.gestao_acoes.repositories.ExchangeRateSnapshotRepository;
import com.trabalho.gestao_acoes.domains.ExchangeRateSnapshot;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Import(CarteiraConcurrencyIntegrationTest.PostgresTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
class CarteiraConcurrencyIntegrationTest {
    @TestConfiguration(proxyBeanMethods = false)
    static class PostgresTestConfiguration {
        @Bean(destroyMethod = "close")
        io.zonky.test.db.postgres.embedded.EmbeddedPostgres embeddedPostgres() throws IOException {
            return io.zonky.test.db.postgres.embedded.EmbeddedPostgres.builder().start();
        }

        @Bean
        DataSource dataSource(io.zonky.test.db.postgres.embedded.EmbeddedPostgres postgres) {
            SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
            dataSource.setDriverClass(org.postgresql.Driver.class);
            dataSource.setUrl(postgres.getJdbcUrl("postgres", "postgres"));
            dataSource.setUsername("postgres");
            dataSource.setPassword("");
            return dataSource;
        }

        @Bean
        @org.springframework.context.annotation.Primary
        SecurityUtils securityUtils() {
            return org.mockito.Mockito.mock(SecurityUtils.class);
        }
    }

    @Autowired private CarteiraTransactionService service;
    @Autowired private OperationLedgerService ledger;
    @Autowired private AcaoRepository assets;
    @Autowired private CorretoraRepository brokers;
    @Autowired private PosicaoCarteiraRepository positions;
    @Autowired private TransacaoRepository transactions;
    @Autowired private ExchangeRateSnapshotRepository exchangeRates;
    @Autowired private com.trabalho.gestao_acoes.repositories.UserAccountRepository users;
    @Autowired private com.trabalho.gestao_acoes.repositories.PortfolioRepository portfolios;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private DataSource dataSource;
    @Autowired private liquibase.integration.spring.SpringLiquibase liquibase;

    @Autowired private SecurityUtils securityUtils;


    private ExecutorService executor;
    private Acao asset;
    private Corretora broker;
    private com.trabalho.gestao_acoes.domains.Portfolio defaultPortfolio;
    private com.trabalho.gestao_acoes.domains.UserAccount user;

    @BeforeEach
    void setUp() {
        exchangeRates.deleteAll();
        transactions.deleteAll();
        positions.deleteAll();
        assets.deleteAll();
        brokers.deleteAll();
        portfolios.deleteAll();
        users.deleteAll();

        user = users.save(new com.trabalho.gestao_acoes.domains.UserAccount("testuser", "test@user.com", "pass", Instant.now()));
        defaultPortfolio = portfolios.save(new com.trabalho.gestao_acoes.domains.Portfolio("Principal", user, Instant.now()));
        org.mockito.Mockito.when(securityUtils.currentUser()).thenReturn(user);
        org.mockito.Mockito.when(securityUtils.currentOwnerId()).thenReturn(user.getId());

        asset = new Acao(null, "PETR4", "Petrobras", "BRASIL", "BRL",
                new BigDecimal("20.00000000"), LocalDateTime.now());
        asset.setOwner(user);
        asset = assets.save(asset);
        broker = new Corretora(null, "11222333000181", "Corretora Teste", "Teste",
                null, null, "01001000", null, null, null, null, null, "SP", "ATIVA", true, LocalDateTime.now());
        broker.setRegulatoryStatus(com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus.VERIFIED);
        broker.setOwner(user);
        broker = brokers.save(broker);
        executor = Executors.newFixedThreadPool(2);

    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    @Timeout(10)
    void aLiquibaseCreatesPostgreSqlSchemaOnceAndReleasesMigrationLock() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM databasechangelog")) {
                result.next();
                assertThat(result.getLong(1)).isEqualTo(15);
            }
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM databasechangeloglock WHERE locked = false")) {
                result.next();
                assertThat(result.getLong(1)).isEqualTo(1);
            }
        }
        liquibase.afterPropertiesSet();
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM databasechangelog")) {
            result.next();
            assertThat(result.getLong(1)).isEqualTo(15);
        }
    }

    @Test
    @Timeout(10)
    void aaPortfolioReadMigrationUsesPostgreSqlTimezoneColumnsAndCreatesIndexes() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            try (var columns = statement.executeQuery("""
                    SELECT column_name, data_type
                      FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'acao'
                       AND column_name IN ('quote_provider', 'quote_reference_at', 'quote_fetched_at')
                     ORDER BY column_name
                    """)) {
                assertThat(Stream.generate(() -> {
                    try {
                        return columns.next() ? columns.getString(1) + ":" + columns.getString(2) : null;
                    } catch (java.sql.SQLException error) {
                        throw new RuntimeException(error);
                    }
                }).takeWhile(java.util.Objects::nonNull).toList()).containsExactly(
                        "quote_fetched_at:timestamp with time zone",
                        "quote_provider:character varying",
                        "quote_reference_at:timestamp with time zone");
            }
            try (var indexes = statement.executeQuery("""
                    SELECT indexname FROM pg_indexes
                     WHERE schemaname = 'public' AND tablename = 'transacao'
                       AND indexname IN ('idx_transacao_data_id', 'idx_transacao_corretora_data',
                                         'idx_transacao_tipo_data_id', 'idx_transacao_acao_data_id')
                     ORDER BY indexname
                    """)) {
                assertThat(Stream.generate(() -> {
                    try {
                        return indexes.next() ? indexes.getString(1) : null;
                    } catch (java.sql.SQLException error) {
                        throw new RuntimeException(error);
                    }
                }).takeWhile(java.util.Objects::nonNull).toList()).containsExactly(
                        "idx_transacao_acao_data_id", "idx_transacao_corretora_data",
                        "idx_transacao_data_id", "idx_transacao_tipo_data_id");
            }
        }
    }

    @Test
    @Timeout(10)
    void aabExchangeSnapshotRoundTripsExactDecimalAndTimezoneOnPostgreSql() {
        ExchangeRateSnapshot snapshot = new ExchangeRateSnapshot();
        snapshot.setBaseCurrency("USD"); snapshot.setQuoteCurrency("BRL");
        snapshot.setRate(new BigDecimal("5.43219876")); snapshot.setSourceType("OFFICIAL_REFERENCE_RATE");
        snapshot.setProvider("BANCO_CENTRAL_DO_BRASIL_PTAX");
        snapshot.setReferenceAt(Instant.parse("2026-09-05T16:05:00Z"));
        snapshot.setFetchedAt(Instant.parse("2026-09-06T15:00:00Z"));
        snapshot.setReferenceKind("BCB_PTAX_CLOSING_REFERENCE");

        exchangeRates.saveAndFlush(snapshot);
        var persisted = exchangeRates.findByBaseCurrencyAndQuoteCurrency("USD", "BRL").orElseThrow();

        assertThat(persisted.getRate()).isEqualByComparingTo("5.43219876");
        assertThat(persisted.getReferenceAt()).isEqualTo(snapshot.getReferenceAt());
        assertThat(persisted.getFetchedAt()).isEqualTo(snapshot.getFetchedAt());
    }

    @Test
    @Timeout(10)
    void aacRepresentativePagedReadsUseBoundedIndexPlans() throws Exception {
        service.comprar(asset.getId(), broker.getId(), 1, new BigDecimal("20.00000000"));
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("SET enable_seqscan = off");
            String movementPlan = explain(statement, "SELECT id FROM transacao WHERE corretora_id = " + broker.getId()
                    + " ORDER BY data_hora DESC, id DESC LIMIT 20");
            String positionPlan = explain(statement, "SELECT id FROM posicao_carteira WHERE corretora_id = " + broker.getId()
                    + " ORDER BY acao_id, id LIMIT 20");
            assertThat(movementPlan).contains("Limit", "idx_transacao_corretora_data").doesNotContain("Seq Scan");
            assertThat(positionPlan).contains("Limit", "idx_posicao_corretora_acao_id").doesNotContain("Seq Scan");
        }
    }

    @Test
    @Timeout(10)
    void twoConcurrentFirstPurchasesAreBothCommittedIntoOnePosition() throws Exception {
        List<Future<Throwable>> results = runTogether(
                () -> service.comprar(asset.getId(), broker.getId(), 10, new BigDecimal("20.00000000")),
                () -> service.comprar(asset.getId(), broker.getId(), 5, new BigDecimal("26.00000000")));

        assertThat(results).allSatisfy(result -> assertThat(result.get(5, TimeUnit.SECONDS)).isNull());
        var position = positions.findByAcaoIdAndCorretoraIdAndPortfolioId(asset.getId(), broker.getId(), defaultPortfolio.getId()).orElseThrow();
        assertThat(position.getQuantidadeTotal()).isEqualTo(15);
        assertThat(position.getPrecoMedio()).isEqualByComparingTo("22.00000000");
        assertThat(transactions.count()).isEqualTo(2);
    }

    @Test
    @Timeout(10)
    void onlyOneOfTwoCompetingSalesCommitsAndHistoryStaysConsistent() throws Exception {
        service.comprar(asset.getId(), broker.getId(), 10, new BigDecimal("20.00000000"));

        List<Future<Throwable>> results = runTogether(
                () -> service.vender(asset.getId(), broker.getId(), 7, new BigDecimal("21.00000000")),
                () -> service.vender(asset.getId(), broker.getId(), 7, new BigDecimal("21.00000000")));
        long success = 0;
        long rejected = 0;
        for (Future<Throwable> result : results) {
            Throwable error = result.get(5, TimeUnit.SECONDS);
            if (error == null) success++;
            else if (error instanceof BusinessException) rejected++;
        }

        assertThat(success).isEqualTo(1);
        assertThat(rejected).isEqualTo(1);
        assertThat(positions.findByAcaoIdAndCorretoraIdAndPortfolioId(asset.getId(), broker.getId(), defaultPortfolio.getId()).orElseThrow().getQuantidadeTotal())
                .isEqualTo(3);
        assertThat(transactions.count()).isEqualTo(2);
    }

    @Test
    void preservesWeightedAverageOnPartialSaleAndAllowsRepurchaseAfterZero() {
        service.comprar(asset.getId(), broker.getId(), 1, new BigDecimal("10.00000000"));
        service.comprar(asset.getId(), broker.getId(), 2, new BigDecimal("10.01000000"));
        var bought = positions.findByAcaoIdAndCorretoraIdAndPortfolioId(asset.getId(), broker.getId(), defaultPortfolio.getId()).orElseThrow();
        assertThat(bought.getQuantidadeTotal()).isEqualTo(3);
        assertThat(bought.getPrecoMedio()).isEqualByComparingTo("10.00666667");

        service.vender(asset.getId(), broker.getId(), 1, new BigDecimal("11.00000000"));
        var partial = positions.findByAcaoIdAndCorretoraIdAndPortfolioId(asset.getId(), broker.getId(), defaultPortfolio.getId()).orElseThrow();
        assertThat(partial.getQuantidadeTotal()).isEqualTo(2);
        assertThat(partial.getPrecoMedio()).isEqualByComparingTo("10.00666667");

        service.vender(asset.getId(), broker.getId(), 2, new BigDecimal("11.00000000"));
        assertThat(positions.findByAcaoIdAndCorretoraIdAndPortfolioId(asset.getId(), broker.getId(), defaultPortfolio.getId())).isEmpty();
        service.comprar(asset.getId(), broker.getId(), 1, new BigDecimal("12.00000000"));
        assertThat(positions.findByAcaoIdAndCorretoraIdAndPortfolioId(asset.getId(), broker.getId(), defaultPortfolio.getId()).orElseThrow().getPrecoMedio())
                .isEqualByComparingTo("12.00000000");
        assertThat(transactions.count()).isEqualTo(5);
    }

    @Test
    void keepsPositionsSeparatedByBrokerAndRejectsQuantityOverflowWithoutHistory() {
        Corretora second = new Corretora(null, "19131243000197", "Segunda Corretora", "Segunda",
                null, null, "20040002", null, null, null, null, null, "RJ", "ATIVA", true, LocalDateTime.now());
        second.setRegulatoryStatus(com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus.VERIFIED);
        second.setOwner(user);
        second = brokers.save(second);
        service.comprar(asset.getId(), broker.getId(), 1, new BigDecimal("10.00000000"));
        service.comprar(asset.getId(), second.getId(), 2, new BigDecimal("20.00000000"));
        assertThat(positions.count()).isEqualTo(2);

        var first = positions.findByAcaoIdAndCorretoraIdAndPortfolioId(asset.getId(), broker.getId(), defaultPortfolio.getId()).orElseThrow();
        first.setQuantidadeTotal(Integer.MAX_VALUE);
        positions.saveAndFlush(first);
        long historyBefore = transactions.count();
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.comprar(asset.getId(), broker.getId(), 1, new BigDecimal("10.00000000")))
                .isInstanceOf(BusinessException.class);
        assertThat(transactions.count()).isEqualTo(historyBefore);
    }

    @Test
    void persistenceFailureRollsBackBothHistoryAndPosition() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE FUNCTION fail_position_insert() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'controlled position failure'; END; $$");
            statement.execute("CREATE TRIGGER fail_position_insert BEFORE INSERT ON posicao_carteira FOR EACH ROW EXECUTE FUNCTION fail_position_insert() ");
        }
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    service.comprar(asset.getId(), broker.getId(), 1, new BigDecimal("20.00000000")))
                    .isInstanceOf(RuntimeException.class);
            assertThat(transactions.count()).isZero();
            assertThat(positions.findByAcaoIdAndCorretoraIdAndPortfolioId(asset.getId(), broker.getId(), defaultPortfolio.getId())).isEmpty();
        } finally {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("DROP TRIGGER IF EXISTS fail_position_insert ON posicao_carteira");
                statement.execute("DROP FUNCTION IF EXISTS fail_position_insert()");
            }
        }
    }

    @Test
    @Timeout(10)
    void equivalentConcurrentLedgerCreatesWithTheSameKeyPersistOneTransaction() throws Exception {
        OperationRequestDTO request = ledgerRequest(broker, "ledger-equivalent", 2);

        List<Future<LedgerAttempt>> attempts = runTogetherValues(
                () -> ledgerAttempt(request), () -> ledgerAttempt(request));

        LedgerAttempt first = attempts.get(0).get(5, TimeUnit.SECONDS);
        LedgerAttempt second = attempts.get(1).get(5, TimeUnit.SECONDS);
        assertThat(first.error()).isNull();
        assertThat(second.error()).isNull();
        assertThat(first.operation().id()).isEqualTo(second.operation().id());
        assertThat(transactions.findAllByPortfolioId(defaultPortfolio.getId())).hasSize(1);
    }

    @Test
    @Timeout(10)
    void concurrentLedgerCreatesWithSameKeyAcrossBrokersReturnIdempotencyConflict() throws Exception {
        Corretora otherBroker = secondBroker("19131243000197");
        List<Future<LedgerAttempt>> attempts = runTogetherValues(
                () -> ledgerAttempt(ledgerRequest(broker, "ledger-cross-broker", 1)),
                () -> ledgerAttempt(ledgerRequest(otherBroker, "ledger-cross-broker", 1)));

        List<LedgerAttempt> results = List.of(attempts.get(0).get(5, TimeUnit.SECONDS), attempts.get(1).get(5, TimeUnit.SECONDS));
        assertThat(results).filteredOn(result -> result.error() == null).hasSize(1);
        assertThat(results).filteredOn(result -> result.error() != null).allSatisfy(result -> {
            assertThat(result.error()).isInstanceOf(ConflictException.class);
            assertThat(((ConflictException) result.error()).getCode()).isEqualTo("IDEMPOTENCY_CONFLICT");
        });
        assertThat(transactions.findAllByPortfolioId(defaultPortfolio.getId())).hasSize(1);
    }

    @Test
    @Timeout(10)
    void differentKeysRemainIndependentAndDifferentPortfoliosMayReuseAKey() throws Exception {
        List<Future<LedgerAttempt>> attempts = runTogetherValues(
                () -> ledgerAttempt(ledgerRequest(broker, "ledger-first", 1)),
                () -> ledgerAttempt(ledgerRequest(broker, "ledger-second", 1)));
        assertThat(attempts).allSatisfy(attempt -> assertThat(attempt.get(5, TimeUnit.SECONDS).error()).isNull());
        assertThat(transactions.findAllByPortfolioId(defaultPortfolio.getId())).hasSize(2);

        var otherUser = users.save(new com.trabalho.gestao_acoes.domains.UserAccount("second-owner", "second-owner@test.local", "pass", Instant.now()));
        var otherPortfolio = portfolios.save(new com.trabalho.gestao_acoes.domains.Portfolio("Principal", otherUser, Instant.now()));
        Acao otherAsset = new Acao(null, "VALE3", "Vale", "BRASIL", "BRL", new BigDecimal("20.00000000"), LocalDateTime.now());
        otherAsset.setOwner(otherUser); otherAsset = assets.save(otherAsset);
        Corretora otherBroker = new Corretora(null, "12345678000195", "Outra Corretora", "Outra", null, null, "01001000", null, null, null, null, null, "SP", "ATIVA", true, LocalDateTime.now());
        otherBroker.setOwner(otherUser); otherBroker.setRegulatoryStatus(com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus.VERIFIED); otherBroker = brokers.save(otherBroker);

        org.mockito.Mockito.when(securityUtils.currentOwnerId()).thenReturn(otherUser.getId());
        org.mockito.Mockito.when(securityUtils.currentUser()).thenReturn(otherUser);
        OperationDTO otherOperation = ledger.create(new OperationRequestDTO(TipoTransacao.COMPRA, otherAsset.getId(), otherBroker.getId(), LocalDateTime.of(2026, 1, 2, 12, 0), 1, "BRL", new BigDecimal("20"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, "ledger-first"));
        assertThat(otherOperation.id()).isNotNull();
        assertThat(transactions.findAllByPortfolioId(otherPortfolio.getId())).hasSize(1);
    }

    @Test
    @Timeout(10)
    void equivalentConcurrentAssetRegistrationsReturnExactlyOneCreatedAndOneConflict() throws Exception {
        CountDownLatch providersReady = new CountDownLatch(2);
        CountDownLatch releaseProvider = new CountDownLatch(1);
        var strategy = new com.trabalho.gestao_acoes.services.ports.CotacaoStrategy() {
            public com.trabalho.gestao_acoes.services.ports.CotacaoBolsa buscarCotacao(String ticker) {
                providersReady.countDown();
                try {
                    if (!releaseProvider.await(2, TimeUnit.SECONDS)) throw new AssertionError("provider barrier timed out");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(ex);
                }
                var quote = new com.trabalho.gestao_acoes.services.ports.CotacaoBolsa(new BigDecimal("30"), "BRL",
                        "TEST_FIXTURE", "CONCURRENCY_PROVIDER", null, null, null);
                quote.setNomeEmpresa("Vale S.A.");
                return quote;
            }
            public boolean suportaMercado(String market) { return "BRASIL".equals(market); }
        };
        AcaoService registration = new AcaoService(assets, new CotacaoService(List.of(strategy)), securityUtils);
        AcaoResource resource = new AcaoResource();
        ReflectionTestUtils.setField(resource, "service", registration);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(resource)
                .setControllerAdvice(new ResourceExceptionHandler()).setValidator(validator).build();

        Callable<Integer> request = () -> mvc.perform(post("/acoes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticker\":\" vale3 \",\"mercado\":\"nacional\"}"))
                .andReturn().getResponse().getStatus();
        Future<Integer> first = executor.submit(request);
        Future<Integer> second = executor.submit(request);
        assertThat(providersReady.await(2, TimeUnit.SECONDS)).isTrue();
        releaseProvider.countDown();

        assertThat(Stream.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)).sorted().toList())
                .containsExactly(201, 409);
        assertThat(assets.findAll().stream().filter(a -> "VALE3".equals(a.getTicker()))).hasSize(1);
    }

    @Test
    @Timeout(10)
    void equivalentConcurrentBrokerRegistrationsReturnExactlyOneCreatedAndOneConflict() throws Exception {
        CountDownLatch providersReady = new CountDownLatch(2);
        CountDownLatch releaseProvider = new CountDownLatch(1);
        com.trabalho.gestao_acoes.services.ports.CnpjClientPort companyClient = cnpj -> {
            providersReady.countDown();
            try {
                if (!releaseProvider.await(2, TimeUnit.SECONDS)) throw new AssertionError("provider barrier timed out");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError(ex);
            }
            var company = new com.trabalho.gestao_acoes.integrations.brasilapi.BrasilApiResponse();
            company.setRazaoSocial("Corretora Concorrente");
            company.setNomeFantasia("Concorrente");
            company.setDescricaoSituacaoCadastral("ATIVA");
            company.setCnaeFiscal(6612601);
            return company;
        };
        com.trabalho.gestao_acoes.services.ports.CepClientPort addressClient = cep -> {
            var address = new com.trabalho.gestao_acoes.integrations.viacep.ViaCepResponse();
            address.setCep("01001-000"); address.setLogradouro("Praça da Sé"); address.setBairro("Sé");
            address.setLocalidade("São Paulo"); address.setUf("SP");
            return address;
        };
        java.time.Clock registrationClock = java.time.Clock.systemUTC();
        com.trabalho.gestao_acoes.services.ports.RegulatoryRegistryPort registry = () ->
                new com.trabalho.gestao_acoes.services.ports.RegulatoryRegistrySnapshot("CVM",
                        registrationClock.instant().minusSeconds(60), registrationClock.instant(), java.util.Map.of(
                        "19131243000197", java.util.List.of(new com.trabalho.gestao_acoes.services.ports.RegulatoryRegistrySnapshot.RegulatoryEntry(
                                "CORRETORAS", "EM FUNCIONAMENTO NORMAL", "123"))));
        RegulatoryVerificationService verification = new RegulatoryVerificationService(registry, registrationClock,
                java.time.Duration.ofDays(7), java.util.Set.of("CORRETORAS"));
        CorretoraService registration = new CorretoraService(brokers, companyClient, addressClient, verification,
                registrationClock, java.time.Duration.ofDays(7), securityUtils);
        com.trabalho.gestao_acoes.resources.CorretoraResource resource = new com.trabalho.gestao_acoes.resources.CorretoraResource();
        ReflectionTestUtils.setField(resource, "service", registration);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(resource)
                .setControllerAdvice(new ResourceExceptionHandler()).setValidator(validator).build();

        Callable<Integer> digits = () -> mvc.perform(post("/corretoras").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cnpj\":\"19131243000197\",\"cep\":\"01001000\"}"))
                .andReturn().getResponse().getStatus();
        Callable<Integer> masked = () -> mvc.perform(post("/corretoras").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cnpj\":\"19.131.243/0001-97\",\"cep\":\"01001000\"}"))
                .andReturn().getResponse().getStatus();
        Future<Integer> first = executor.submit(digits);
        Future<Integer> second = executor.submit(masked);
        assertThat(providersReady.await(2, TimeUnit.SECONDS)).isTrue();
        releaseProvider.countDown();

        assertThat(Stream.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)).sorted().toList())
                .containsExactly(201, 409);
        assertThat(brokers.findAll().stream().filter(b -> "19131243000197".equals(b.getCnpj()))).hasSize(1);
    }

    @Test
    @Timeout(10)
    void controlledLockTimeoutFailsWithinTheLimitWithoutPartialWrites() throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> holder = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            brokers.findByIdAndOwnerIdForUpdate(broker.getId(), user.getId()).orElseThrow();
            locked.countDown();
            try { release.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new RuntimeException(ex); }
        }));
        assertThat(locked.await(2, TimeUnit.SECONDS)).isTrue();
        Future<Throwable> competing = executor.submit(() -> {
            try {
                service.comprar(asset.getId(), broker.getId(), 1, new BigDecimal("20.00000000"));
                return null;
            } catch (Throwable error) { return error; }
        });

        Throwable error;
        try { error = competing.get(3, TimeUnit.SECONDS); }
        finally { release.countDown(); holder.get(3, TimeUnit.SECONDS); }
        assertThat(error).isNotNull();
        assertThat(transactions.count()).isZero();
        assertThat(positions.count()).isZero();
    }

    @Test
    void zVersionedMigrationBlocksCanonicalCollisionsAndPreservesValidIds() throws Exception {
        Acao collision = new Acao(null, " petr4 ", "Duplicada", "NACIONAL", "BRL",
                new BigDecimal("20.00000000"), LocalDateTime.now());
        collision.setOwner(user);
        collision = assets.save(collision);
        String preflight = Files.readString(Path.of("db/adoption/preflight-existing-postgresql.sql"), StandardCharsets.UTF_8);
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> executeSql(statement, preflight))
                    .isInstanceOf(java.sql.SQLException.class);
        }
        assertThat(assets.findById(asset.getId())).isPresent();
        assertThat(assets.findById(collision.getId())).isPresent();

        assets.delete(collision);
        String migration = Files.readString(Path.of("db/adoption/normalize-existing-postgresql.sql"), StandardCharsets.UTF_8);
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            executeSql(statement, preflight);
            executeSql(statement, migration);
            String equivalence = Files.readString(Path.of("db/adoption/verify-schema-equivalence.sql"), StandardCharsets.UTF_8);
            executeSql(statement, equivalence);
            statement.execute("ALTER TABLE acao ALTER COLUMN ticker TYPE varchar(11)");
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> executeSql(statement, equivalence))
                    .isInstanceOf(java.sql.SQLException.class)
                    .hasMessageContaining("schema equivalence");
            statement.execute("ALTER TABLE acao ALTER COLUMN ticker TYPE varchar(10)");
            executeSql(statement, equivalence);
        }
        assertThat(assets.findById(asset.getId()).orElseThrow().getTicker()).isEqualTo("PETR4");
        assertThat(brokers.findById(broker.getId()).orElseThrow().getCnpj()).isEqualTo("11222333000181");
    }

    private static void executeSql(java.sql.Statement statement, String script) throws java.sql.SQLException {
        String dollarTag = null;
        int start = 0;
        for (int index = 0; index < script.length(); index++) {
            if (script.charAt(index) == '$') {
                int end = script.indexOf('$', index + 1);
                if (end > index) {
                    String candidate = script.substring(index, end + 1);
                    if (candidate.matches("\\$[A-Za-z_]*\\$")) {
                        if (dollarTag == null) dollarTag = candidate;
                        else if (dollarTag.equals(candidate)) dollarTag = null;
                        index = end;
                    }
                }
            } else if (script.charAt(index) == ';' && dollarTag == null) {
                String sql = script.substring(start, index + 1).trim();
                if (!sql.isEmpty()) statement.execute(sql);
                start = index + 1;
            }
        }
        String remainder = script.substring(start).trim();
        if (!remainder.isEmpty()) statement.execute(remainder + ";");
    }

    private static String explain(java.sql.Statement statement, String sql) throws java.sql.SQLException {
        StringBuilder plan = new StringBuilder();
        try (var result = statement.executeQuery("EXPLAIN " + sql)) {
            while (result.next()) plan.append(result.getString(1)).append('\n');
        }
        return plan.toString();
    }

    private Corretora secondBroker(String cnpj) {
        Corretora second = new Corretora(null, cnpj, "Segunda Corretora", "Segunda", null, null, "20040002", null, null, null, null, null, "RJ", "ATIVA", true, LocalDateTime.now());
        second.setRegulatoryStatus(com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus.VERIFIED);
        second.setOwner(user);
        return brokers.save(second);
    }

    private OperationRequestDTO ledgerRequest(Corretora targetBroker, String key, int quantity) {
        return new OperationRequestDTO(TipoTransacao.COMPRA, asset.getId(), targetBroker.getId(),
                LocalDateTime.of(2026, 1, 2, 10, 0), quantity, "BRL", new BigDecimal("20"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "concurrent", key);
    }

    private LedgerAttempt ledgerAttempt(OperationRequestDTO request) {
        try { return new LedgerAttempt(ledger.create(request), null); }
        catch (Throwable error) { return new LedgerAttempt(null, error); }
    }

    private <T> List<Future<T>> runTogetherValues(Callable<T> first, Callable<T> second) {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<T> a = executor.submit(valueWrap(first, ready, start));
        Future<T> b = executor.submit(valueWrap(second, ready, start));
        try {
            if (!ready.await(2, TimeUnit.SECONDS)) throw new AssertionError("workers did not reach the barrier");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
        start.countDown();
        return List.of(a, b);
    }

    private <T> Callable<T> valueWrap(Callable<T> action, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(2, TimeUnit.SECONDS)) throw new AssertionError("start barrier timed out");
            return action.call();
        };
    }

    private List<Future<Throwable>> runTogether(ThrowingAction first, ThrowingAction second) {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Throwable> wrapFirst = wrap(first, ready, start);
        Callable<Throwable> wrapSecond = wrap(second, ready, start);
        Future<Throwable> a = executor.submit(wrapFirst);
        Future<Throwable> b = executor.submit(wrapSecond);
        try {
            if (!ready.await(2, TimeUnit.SECONDS)) throw new AssertionError("workers did not reach the barrier");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
        start.countDown();
        return List.of(a, b);
    }

    private Callable<Throwable> wrap(ThrowingAction action, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(2, TimeUnit.SECONDS)) return new AssertionError("start barrier timed out");
            try { action.run(); return null; }
            catch (Throwable error) { return error; }
        };
    }

    @FunctionalInterface
    private interface ThrowingAction { void run(); }
    private record LedgerAttempt(OperationDTO operation, Throwable error) { }
}
