package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.config.AuthProperties;
import com.trabalho.gestao_acoes.domains.AdminUser;
import com.trabalho.gestao_acoes.repositories.AdminUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthAttemptService {
    private final AdminUserRepository repository; private final AuthProperties properties; private final Clock clock;
    private final Map<String, OriginBucket> origins = new ConcurrentHashMap<>();
    public AuthAttemptService(AdminUserRepository repository, AuthProperties properties, Clock clock) {
        this.repository=repository; this.properties=properties; this.clock=clock;
    }
    public String origin(HttpServletRequest request) {
        if (properties.isTrustForwardedHeaders()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",", 2)[0].strip();
        }
        return request.getRemoteAddr();
    }
    public boolean isOriginBlocked(String origin) { return origins.computeIfAbsent(origin, key -> new OriginBucket()).blocked(clock.instant()); }
    @Transactional(readOnly = true)
    public boolean isAccountBlocked(String username) { return repository.findByUsername(username).map(u -> u.isLocked(clock.instant())).orElse(false); }
    public void recordOriginFailure(String origin) { origins.computeIfAbsent(origin, key -> new OriginBucket()).failure(clock.instant()); cleanup(); }
    @Transactional
    public void recordAccountFailure(String username) {
        repository.findForUpdateByUsername(username).ifPresent(user -> user.registerFailure(clock.instant(), properties.getMaxAttempts(), properties.getAttemptWindow(), properties.getLockDuration()));
    }
    public void clearOrigin(String origin) { origins.remove(origin); }
    @Transactional
    public void clearAccount(String username) { repository.findForUpdateByUsername(username).ifPresent(user -> user.clearFailures(clock.instant())); }
    private void cleanup() {
        Instant cutoff = clock.instant().minus(properties.getOriginRetention());
        origins.entrySet().removeIf(e -> e.getValue().lastSeen.isBefore(cutoff));
    }
    private final class OriginBucket {
        private final ArrayDeque<Instant> failures = new ArrayDeque<>(); private Instant lockedUntil; private Instant lastSeen = Instant.EPOCH;
        synchronized void failure(Instant now) {
            trim(now); failures.addLast(now); lastSeen=now;
            if (failures.size() >= properties.getMaxAttempts()) lockedUntil=now.plus(properties.getLockDuration());
        }
        synchronized boolean blocked(Instant now) {
            trim(now); lastSeen=now;
            if (lockedUntil != null && lockedUntil.isAfter(now)) return true;
            if (lockedUntil != null) { lockedUntil=null; failures.clear(); }
            return false;
        }
        private void trim(Instant now) { Instant cutoff=now.minus(properties.getAttemptWindow()); while(!failures.isEmpty() && failures.peekFirst().isBefore(cutoff)) failures.removeFirst(); }
    }
}
