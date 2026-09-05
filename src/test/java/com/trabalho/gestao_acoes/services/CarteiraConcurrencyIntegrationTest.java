package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Acao;
import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.dtos.AcaoDTO;
import com.trabalho.gestao_acoes.resources.AcaoResource;
import com.trabalho.gestao_acoes.resources.exceptions.ResourceExceptionHandler;
import com.trabalho.gestao_acoes.repositories.AcaoRepository;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.repositories.PosicaoCarteiraRepository;
import com.trabalho.gestao_acoes.repositories.TransacaoRepository;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
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
        "spring.jpa.hibernate.ddl-auto=create-drop"
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
    }

    @Autowired private CarteiraTransactionService service;
    @Autowired private AcaoRepository assets;
    @Autowired private CorretoraRepository brokers;
    @Autowired private PosicaoCarteiraRepository positions;
    @Autowired private TransacaoRepository transactions;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private DataSource dataSource;

    private ExecutorService executor;
    private Acao asset;
    private Corretora broker;

    @BeforeEach
    void setUp() {
        transactions.deleteAll();
        positions.deleteAll();
        assets.deleteAll();
        brokers.deleteAll();
        asset = assets.save(new Acao(null, "PETR4", "Petrobras", "BRASIL", "BRL",
                new BigDecimal("20.00000000"), LocalDateTime.now()));
        broker = brokers.save(new Corretora(null, "11222333000181", "Corretora Teste", "Teste",
                null, null, "01001000", null, null, null, null, null, "SP", "ATIVA", true, LocalDateTime.now()));
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }

    @Test
    @Timeout(10)
    void twoConcurrentFirstPurchasesAreBothCommittedIntoOnePosition() throws Exception {
        List<Future<Throwable>> results = runTogether(
                () -> service.comprar(asset.getId(), broker.getId(), 10, new BigDecimal("20.00000000")),
                () -> service.comprar(asset.getId(), broker.getId(), 5, new BigDecimal("26.00000000")));

        assertThat(results).allSatisfy(result -> assertThat(result.get(5, TimeUnit.SECONDS)).isNull());
        var position = positions.findByAcaoIdAndCorretoraId(asset.getId(), broker.getId()).orElseThrow();
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
        assertThat(positions.findByAcaoIdAndCorretoraId(asset.getId(), broker.getId()).orElseThrow().getQuantidadeTotal())
                .isEqualTo(3);
        assertThat(transactions.count()).isEqualTo(2);
    }

    @Test
    void preservesWeightedAverageOnPartialSaleAndAllowsRepurchaseAfterZero() {
        service.comprar(asset.getId(), broker.getId(), 1, new BigDecimal("10.00000000"));
        service.comprar(asset.getId(), broker.getId(), 2, new BigDecimal("10.01000000"));
        var bought = positions.findByAcaoIdAndCorretoraId(asset.getId(), broker.getId()).orElseThrow();
        assertThat(bought.getQuantidadeTotal()).isEqualTo(3);
        assertThat(bought.getPrecoMedio()).isEqualByComparingTo("10.00666667");

        service.vender(asset.getId(), broker.getId(), 1, new BigDecimal("11.00000000"));
        var partial = positions.findByAcaoIdAndCorretoraId(asset.getId(), broker.getId()).orElseThrow();
        assertThat(partial.getQuantidadeTotal()).isEqualTo(2);
        assertThat(partial.getPrecoMedio()).isEqualByComparingTo("10.00666667");

        service.vender(asset.getId(), broker.getId(), 2, new BigDecimal("11.00000000"));
        assertThat(positions.findByAcaoIdAndCorretoraId(asset.getId(), broker.getId())).isEmpty();
        service.comprar(asset.getId(), broker.getId(), 1, new BigDecimal("12.00000000"));
        assertThat(positions.findByAcaoIdAndCorretoraId(asset.getId(), broker.getId()).orElseThrow().getPrecoMedio())
                .isEqualByComparingTo("12.00000000");
        assertThat(transactions.count()).isEqualTo(5);
    }

    @Test
    void keepsPositionsSeparatedByBrokerAndRejectsQuantityOverflowWithoutHistory() {
        Corretora second = brokers.save(new Corretora(null, "19131243000197", "Segunda Corretora", "Segunda",
                null, null, "20040002", null, null, null, null, null, "RJ", "ATIVA", true, LocalDateTime.now()));
        service.comprar(asset.getId(), broker.getId(), 1, new BigDecimal("10.00000000"));
        service.comprar(asset.getId(), second.getId(), 2, new BigDecimal("20.00000000"));
        assertThat(positions.count()).isEqualTo(2);

        var first = positions.findByAcaoIdAndCorretoraId(asset.getId(), broker.getId()).orElseThrow();
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
            assertThat(positions.findByAcaoIdAndCorretoraId(asset.getId(), broker.getId())).isEmpty();
        } finally {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("DROP TRIGGER IF EXISTS fail_position_insert ON posicao_carteira");
                statement.execute("DROP FUNCTION IF EXISTS fail_position_insert()");
            }
        }
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
                return new com.trabalho.gestao_acoes.services.ports.CotacaoBolsa(new BigDecimal("30"), "BRL");
            }
            public boolean suportaMercado(String market) { return "BRASIL".equals(market); }
        };
        AcaoService registration = new AcaoService(assets, new CotacaoService(List.of(strategy)));
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
        CorretoraService registration = new CorretoraService(brokers, companyClient, addressClient);
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
            brokers.findByIdForUpdate(broker.getId()).orElseThrow();
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
        Acao collision = assets.save(new Acao(null, " petr4 ", "Duplicada", "NACIONAL", "BRL",
                new BigDecimal("20.00000000"), LocalDateTime.now()));
        String preflight = Files.readString(Path.of("db/stabilization/V001__preflight.sql"), StandardCharsets.UTF_8);
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> executeSql(statement, preflight))
                    .isInstanceOf(java.sql.SQLException.class);
        }
        assertThat(assets.findById(asset.getId())).isPresent();
        assertThat(assets.findById(collision.getId())).isPresent();

        assets.delete(collision);
        String migration = Files.readString(Path.of("db/stabilization/V002__normalize_and_constrain.sql"), StandardCharsets.UTF_8);
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            executeSql(statement, preflight);
            executeSql(statement, migration);
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
}
