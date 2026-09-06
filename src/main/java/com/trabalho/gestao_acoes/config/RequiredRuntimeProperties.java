package com.trabalho.gestao_acoes.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Profile("dev")
@Validated
@ConfigurationProperties(prefix = "app.required")
public class RequiredRuntimeProperties {
    @NotBlank(message = "DB_PASSWORD is required")
    private String dbPassword;
    @NotBlank(message = "BRAPI_TOKEN is required")
    private String brapiToken;
    @NotBlank(message = "TWELVEDATA_API_KEY is required")
    private String twelvedataApiKey;
    @NotBlank(message = "ADMIN_INITIAL_USERNAME is required")
    private String adminInitialUsername;
    @NotBlank(message = "ADMIN_INITIAL_PASSWORD is required")
    private String adminInitialPassword;

    public String getDbPassword() { return dbPassword; }
    public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }
    public String getBrapiToken() { return brapiToken; }
    public void setBrapiToken(String brapiToken) { this.brapiToken = brapiToken; }
    public String getTwelvedataApiKey() { return twelvedataApiKey; }
    public void setTwelvedataApiKey(String twelvedataApiKey) { this.twelvedataApiKey = twelvedataApiKey; }
    public String getAdminInitialUsername() { return adminInitialUsername; }
    public void setAdminInitialUsername(String adminInitialUsername) { this.adminInitialUsername = adminInitialUsername; }
    public String getAdminInitialPassword() { return adminInitialPassword; }
    public void setAdminInitialPassword(String adminInitialPassword) { this.adminInitialPassword = adminInitialPassword; }
}
