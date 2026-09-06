package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.config.AuthProperties;
import com.trabalho.gestao_acoes.domains.AdminUser;
import com.trabalho.gestao_acoes.repositories.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthAttemptServiceTest {
    @Test
    void originLimitExpiresWithControlledClockAndIgnoresUntrustedForwardedHeader() {
        MutableClock clock=new MutableClock(Instant.parse("2026-09-06T12:00:00Z"));
        AuthProperties properties=new AuthProperties(); properties.setMaxAttempts(5); properties.setAttemptWindow(Duration.ofMinutes(15)); properties.setLockDuration(Duration.ofMinutes(15));
        AuthAttemptService service=new AuthAttemptService(mock(AdminUserRepository.class),properties,clock);
        MockHttpServletRequest request=new MockHttpServletRequest(); request.setRemoteAddr("127.0.0.9"); request.addHeader("X-Forwarded-For","203.0.113.99");
        assertThat(service.origin(request)).isEqualTo("127.0.0.9");
        for(int i=0;i<5;i++) service.recordOriginFailure("127.0.0.9");
        assertThat(service.isOriginBlocked("127.0.0.9")).isTrue();
        clock.advance(Duration.ofMinutes(16));
        assertThat(service.isOriginBlocked("127.0.0.9")).isFalse();
    }

    @Test
    void accountLimitIncrementsBlocksExpiresAndClearsWithControlledClock() {
        MutableClock clock=new MutableClock(Instant.parse("2026-09-06T12:00:00Z"));
        AuthProperties properties=new AuthProperties(); properties.setMaxAttempts(5); properties.setAttemptWindow(Duration.ofMinutes(15)); properties.setLockDuration(Duration.ofMinutes(15));
        AdminUser user=new AdminUser("atlas-admin", "{bcrypt}not-used", clock.instant());
        AdminUserRepository repository=mock(AdminUserRepository.class);
        when(repository.findForUpdateByUsername("atlas-admin")).thenReturn(java.util.Optional.of(user));
        when(repository.findByUsername("atlas-admin")).thenReturn(java.util.Optional.of(user));
        AuthAttemptService service=new AuthAttemptService(repository,properties,clock);
        for(int i=0;i<5;i++) service.recordAccountFailure("atlas-admin");
        assertThat(user.getFailedAttempts()).isEqualTo(5);
        assertThat(service.isAccountBlocked("atlas-admin")).isTrue();
        clock.advance(Duration.ofMinutes(16));
        assertThat(service.isAccountBlocked("atlas-admin")).isFalse();
        service.clearAccount("atlas-admin");
        assertThat(user.getFailedAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }
    private static final class MutableClock extends Clock {
        private Instant instant; MutableClock(Instant instant){this.instant=instant;} void advance(Duration duration){instant=instant.plus(duration);}
        @Override public ZoneId getZone(){return ZoneOffset.UTC;} @Override public Clock withZone(ZoneId zone){return this;} @Override public Instant instant(){return instant;}
    }
}
