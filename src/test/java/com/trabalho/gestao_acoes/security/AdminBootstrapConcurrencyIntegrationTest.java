package com.trabalho.gestao_acoes.security;

import com.trabalho.gestao_acoes.repositories.UserAccountRepository;
import com.trabalho.gestao_acoes.config.AuthProperties;
import com.trabalho.gestao_acoes.services.AdminBootstrapService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:bootstrap-concurrency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","app.auth.initial-username=concurrent-admin","app.auth.initial-password=Concurrent-runtime-password-123!"})
@DirtiesContext
class AdminBootstrapConcurrencyIntegrationTest {
    @Autowired UserAccountRepository users;
    @Autowired com.trabalho.gestao_acoes.repositories.PortfolioRepository portfolios;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Autowired AuthProperties properties;
    @Autowired Clock clock;
    @Autowired PlatformTransactionManager transactionManager;
    @Test void concurrentFirstBootstrapCreatesExactlyOneAdministrator() throws Exception {
        portfolios.deleteAll();
        users.deleteAll();
        AdminBootstrapService firstBootstrap = new AdminBootstrapService(users, portfolios, encoder, properties, clock, transactionManager);
        AdminBootstrapService secondBootstrap = new AdminBootstrapService(users, portfolios, encoder, properties, clock, transactionManager);
        CountDownLatch ready=new CountDownLatch(2);CountDownLatch start=new CountDownLatch(1);
        var executor=Executors.newFixedThreadPool(2);
        try {
            var first=executor.submit(()->{ready.countDown();start.await();firstBootstrap.bootstrap();return null;});
            var second=executor.submit(()->{ready.countDown();start.await();secondBootstrap.bootstrap();return null;});
            ready.await();start.countDown();first.get();second.get();
        } finally { executor.shutdownNow(); }
        assertThat(users.count()).isEqualTo(1);
        var user=users.findByUsername("concurrent-admin").orElseThrow();
        assertThat(encoder.matches("Concurrent-runtime-password-123!",user.getPasswordHash())).isTrue();
    }
}
