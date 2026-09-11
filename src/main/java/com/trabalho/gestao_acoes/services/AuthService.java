package com.trabalho.gestao_acoes.services;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final Logger log= LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final AuthAttemptService attempts;
    private final PasswordEncoder encoder;
    private final com.trabalho.gestao_acoes.repositories.UserAccountRepository userRepository;
    private final com.trabalho.gestao_acoes.repositories.PortfolioRepository portfolioRepository;
    private final java.time.Clock clock;
    private final String dummyHash;

    public AuthService(AuthenticationManager authenticationManager, AuthAttemptService attempts, PasswordEncoder encoder, com.trabalho.gestao_acoes.repositories.UserAccountRepository userRepository, com.trabalho.gestao_acoes.repositories.PortfolioRepository portfolioRepository, java.time.Clock clock) {
        this.authenticationManager=authenticationManager; this.attempts=attempts; this.encoder=encoder;
        this.userRepository = userRepository; this.portfolioRepository = portfolioRepository; this.clock = clock;
        this.dummyHash=encoder.encode("constant-dummy-password-never-used");
    }

    public Authentication authenticate(String candidate, String password, HttpServletRequest request) {
        String username=AdminBootstrapService.normalize(candidate); String origin=attempts.origin(request);
        if (attempts.isOriginBlocked(origin) || attempts.isAccountBlocked(username)) {
            log.warn("Login temporarily limited"); throw new AuthenticationLimitedException();
        }
        if (username.length()<3 || username.length()>255 || password==null || password.isBlank() || password.length()>128) {
            encoder.matches(password == null ? "" : password, dummyHash); fail(username, origin); throw new AuthenticationRejectedException();
        }
        try {
            Authentication authentication=authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username,password));
            attempts.clearAccount(username); attempts.clearOrigin(origin); log.info("Login succeeded"); return authentication;
        } catch (AuthenticationException ex) {
            fail(username, origin); log.warn("Login rejected"); throw new AuthenticationRejectedException();
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public Authentication register(String username, String email, String password, String passwordConfirmation, boolean termsAccepted, HttpServletRequest request) {
        String normUsername=AdminBootstrapService.normalize(username);
        String normEmail=AdminBootstrapService.normalize(email);
        String origin=attempts.origin(request);
        if (attempts.isOriginBlocked(origin)) {
            log.warn("Registration temporarily limited"); throw new AuthenticationLimitedException();
        }
        if (!termsAccepted || !password.equals(passwordConfirmation)) throw new AuthenticationRejectedException();
        if (password.length() < 12) throw new AuthenticationRejectedException();

        try {
            com.trabalho.gestao_acoes.domains.UserAccount user = new com.trabalho.gestao_acoes.domains.UserAccount(normUsername, normEmail, encoder.encode(password), clock.instant());
            user = userRepository.saveAndFlush(user);
            portfolioRepository.saveAndFlush(new com.trabalho.gestao_acoes.domains.Portfolio("Principal", user, clock.instant()));
            return authenticate(normEmail, password, request);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new RegistrationConflictException();
        }
    }

    private void fail(String username,String origin){ attempts.recordAccountFailure(username); attempts.recordOriginFailure(origin); }


    public static final class AuthenticationRejectedException extends RuntimeException {}
    public static final class AuthenticationLimitedException extends RuntimeException {}
    public static final class RegistrationConflictException extends RuntimeException {}
}
