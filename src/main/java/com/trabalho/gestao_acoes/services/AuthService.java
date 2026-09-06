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
    private final AuthenticationManager authenticationManager; private final AuthAttemptService attempts; private final PasswordEncoder encoder;
    private final String dummyHash;
    public AuthService(AuthenticationManager authenticationManager, AuthAttemptService attempts, PasswordEncoder encoder) {
        this.authenticationManager=authenticationManager; this.attempts=attempts; this.encoder=encoder;
        this.dummyHash=encoder.encode("constant-dummy-password-never-used");
    }
    public Authentication authenticate(String candidate, String password, HttpServletRequest request) {
        String username=AdminBootstrapService.normalize(candidate); String origin=attempts.origin(request);
        if (attempts.isOriginBlocked(origin) || attempts.isAccountBlocked(username)) {
            log.warn("Administrative login temporarily limited"); throw new AuthenticationLimitedException();
        }
        if (username.length()<3 || username.length()>64 || password==null || password.isBlank() || password.length()>128) {
            encoder.matches(password == null ? "" : password, dummyHash); fail(username, origin); throw new AuthenticationRejectedException();
        }
        try {
            Authentication authentication=authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username,password));
            attempts.clearAccount(username); attempts.clearOrigin(origin); log.info("Administrative login succeeded"); return authentication;
        } catch (AuthenticationException ex) {
            fail(username, origin); log.warn("Administrative login rejected"); throw new AuthenticationRejectedException();
        }
    }
    private void fail(String username,String origin){ attempts.recordAccountFailure(username); attempts.recordOriginFailure(origin); }
    public static final class AuthenticationRejectedException extends RuntimeException {}
    public static final class AuthenticationLimitedException extends RuntimeException {}
}
