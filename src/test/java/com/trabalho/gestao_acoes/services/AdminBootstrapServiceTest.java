package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.config.AuthProperties;
import com.trabalho.gestao_acoes.domains.AdminUser;
import com.trabalho.gestao_acoes.repositories.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AdminBootstrapServiceTest {
    @Test
    void missingFirstCredentialFailsWithoutPrintingItsValue() {
        AdminUserRepository repository=mock(AdminUserRepository.class);when(repository.count()).thenReturn(0L);
        AuthProperties properties=new AuthProperties();properties.setInitialUsername("atlas-admin");properties.setInitialPassword("short");
        var service=new AdminBootstrapService(repository, PasswordEncoderFactories.createDelegatingPasswordEncoder(),properties, Clock.systemUTC());
        assertThatThrownBy(service::bootstrap).isInstanceOf(IllegalStateException.class).hasMessage("ADMIN_INITIAL_PASSWORD is missing or invalid").hasMessageNotContaining("short");
        verify(repository,never()).saveAndFlush(any(AdminUser.class));
    }
    @Test
    void existingAccountIsNeverOverwritten() {
        AdminUserRepository repository=mock(AdminUserRepository.class);when(repository.count()).thenReturn(1L);
        AuthProperties properties=new AuthProperties();properties.setInitialUsername("replacement");properties.setInitialPassword("replacement-password-value");
        new AdminBootstrapService(repository,PasswordEncoderFactories.createDelegatingPasswordEncoder(),properties,Clock.systemUTC()).bootstrap();
        verify(repository,never()).saveAndFlush(any(AdminUser.class));
    }
}
