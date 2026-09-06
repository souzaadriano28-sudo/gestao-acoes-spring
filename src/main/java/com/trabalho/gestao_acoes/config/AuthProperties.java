package com.trabalho.gestao_acoes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    private String initialUsername;
    private String initialPassword;
    private int maxAttempts = 5;
    private Duration attemptWindow = Duration.ofMinutes(15);
    private Duration lockDuration = Duration.ofMinutes(15);
    private Duration originRetention = Duration.ofMinutes(30);
    private boolean trustForwardedHeaders;

    public String getInitialUsername() { return initialUsername; }
    public void setInitialUsername(String initialUsername) { this.initialUsername = initialUsername; }
    public String getInitialPassword() { return initialPassword; }
    public void setInitialPassword(String initialPassword) { this.initialPassword = initialPassword; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public Duration getAttemptWindow() { return attemptWindow; }
    public void setAttemptWindow(Duration attemptWindow) { this.attemptWindow = attemptWindow; }
    public Duration getLockDuration() { return lockDuration; }
    public void setLockDuration(Duration lockDuration) { this.lockDuration = lockDuration; }
    public Duration getOriginRetention() { return originRetention; }
    public void setOriginRetention(Duration originRetention) { this.originRetention = originRetention; }
    public boolean isTrustForwardedHeaders() { return trustForwardedHeaders; }
    public void setTrustForwardedHeaders(boolean trustForwardedHeaders) { this.trustForwardedHeaders = trustForwardedHeaders; }
}
