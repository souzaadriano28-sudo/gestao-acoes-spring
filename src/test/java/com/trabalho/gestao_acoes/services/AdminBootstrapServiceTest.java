package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.config.AuthProperties;
import com.trabalho.gestao_acoes.domains.UserAccount;
import com.trabalho.gestao_acoes.repositories.UserAccountRepository;
import com.trabalho.gestao_acoes.repositories.PortfolioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminBootstrapServiceTest {
    @Test
    void bootstrapsAdminUserWhenDatabaseIsEmpty() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PortfolioRepository portfolioRepository = mock(PortfolioRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode("admin12345678")).thenReturn("{noop}admin123");
        when(repository.count()).thenReturn(0L);
        PlatformTransactionManager tx = transactionManager();

        AuthProperties props = new AuthProperties();
        props.setInitialUsername("atlas-admin");
        props.setInitialPassword("admin12345678");

        Clock clock = Clock.fixed(Instant.parse("2026-09-06T12:00:00Z"), ZoneOffset.UTC);
        AdminBootstrapService service = new AdminBootstrapService(repository, portfolioRepository, encoder, props, clock, tx);

        service.bootstrap();

        verify(repository).saveAndFlush(any(UserAccount.class));
        verify(portfolioRepository).saveAndFlush(any());
    }

    @Test
    void skipsBootstrapWhenDatabaseIsNotEmpty() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PortfolioRepository portfolioRepository = mock(PortfolioRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(repository.count()).thenReturn(1L);
        PlatformTransactionManager tx = transactionManager();

        AuthProperties props = new AuthProperties();
        props.setInitialUsername("atlas-admin");
        props.setInitialPassword("admin12345678");

        AdminBootstrapService service = new AdminBootstrapService(repository, portfolioRepository, encoder, props, Clock.systemUTC(), tx);
        service.bootstrap();

        verify(repository, never()).save(any());
    }

    private static PlatformTransactionManager transactionManager() {
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        when(tx.getTransaction(any())).thenReturn(status);
        return tx;
    }

    @Test
    void normalizesUsername() {
        assertThat(AdminBootstrapService.normalize("  AtLas-AdMiN  ")).isEqualTo("atlas-admin");
        assertThat(AdminBootstrapService.normalize("")).isEqualTo("");
        assertThat(AdminBootstrapService.normalize(null)).isEqualTo("");
    }
}
