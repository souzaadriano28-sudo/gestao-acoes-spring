package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.config.AuthProperties;
import com.trabalho.gestao_acoes.domains.AdminUser;
import com.trabalho.gestao_acoes.repositories.AdminUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

@Service
public class AdminBootstrapService {
    private final AdminUserRepository repository; private final PasswordEncoder encoder;
    private final AuthProperties properties; private final Clock clock;
    public AdminBootstrapService(AdminUserRepository repository, PasswordEncoder encoder, AuthProperties properties, Clock clock) {
        this.repository=repository; this.encoder=encoder; this.properties=properties; this.clock=clock;
    }
    @Transactional
    public synchronized void bootstrap() {
        if (repository.count() > 0) return;
        String username = normalize(properties.getInitialUsername());
        String password = properties.getInitialPassword();
        if (username.length() < 3 || username.length() > 64) throw new IllegalStateException("ADMIN_INITIAL_USERNAME is missing or invalid");
        if (password == null || password.length() < 12 || password.length() > 128 || password.equalsIgnoreCase(username))
            throw new IllegalStateException("ADMIN_INITIAL_PASSWORD is missing or invalid");
        repository.saveAndFlush(new AdminUser(username, encoder.encode(password), clock.instant()));
    }
    public static String normalize(String value) { return value == null ? "" : value.strip().toLowerCase(Locale.ROOT); }
}
