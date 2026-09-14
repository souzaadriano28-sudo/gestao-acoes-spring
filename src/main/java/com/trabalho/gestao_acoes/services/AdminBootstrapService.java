package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.config.AuthProperties;
import com.trabalho.gestao_acoes.domains.UserAccount;
import com.trabalho.gestao_acoes.domains.Portfolio;
import com.trabalho.gestao_acoes.repositories.UserAccountRepository;
import com.trabalho.gestao_acoes.repositories.PortfolioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Locale;

@Service
public class AdminBootstrapService {
    private final UserAccountRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PasswordEncoder encoder;
    private final AuthProperties properties;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public AdminBootstrapService(UserAccountRepository userRepository, PortfolioRepository portfolioRepository, PasswordEncoder encoder, AuthProperties properties, Clock clock, PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    public synchronized void bootstrap() {
        String username = normalize(properties.getInitialUsername());
        if (username.length() < 3 || username.length() > 64) throw new IllegalStateException("ADMIN_INITIAL_USERNAME is missing or invalid");
        String password = properties.getInitialPassword();
        if (password == null || password.length() < 12 || password.length() > 128 || password.equalsIgnoreCase(username))
            throw new IllegalStateException("ADMIN_INITIAL_PASSWORD is missing or invalid");
        try {
            transactionTemplate.executeWithoutResult(status -> {
                if (userRepository.count() > 0) return;
                UserAccount admin = new UserAccount(username, "admin@atlas.local", encoder.encode(password), clock.instant());
                admin = userRepository.saveAndFlush(admin);
                portfolioRepository.saveAndFlush(new Portfolio("Principal", admin, clock.instant()));
            });
        } catch (DataIntegrityViolationException collision) {
            Boolean winnerPersisted = transactionTemplate.execute(status ->
                    userRepository.findByUsername(username)
                            .map(user -> !portfolioRepository.findAllByOwnerId(user.getId()).isEmpty())
                            .orElse(false));
            if (!Boolean.TRUE.equals(winnerPersisted)) throw collision;
        }
    }

    public static String normalize(String value) { return value == null ? "" : value.strip().toLowerCase(Locale.ROOT); }
}
