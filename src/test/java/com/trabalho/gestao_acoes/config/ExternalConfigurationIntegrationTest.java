package com.trabalho.gestao_acoes.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "integrations.brapi.url=http://127.0.0.1:29090/custom-brapi",
        "integrations.twelvedata.url=http://127.0.0.1:29090/custom-twelve",
        "integrations.brasilapi.url=http://127.0.0.1:29090/custom-brasil",
        "integrations.viacep.url=http://127.0.0.1:29090/custom-viacep",
        "app.cors.allowed-origin=http://127.0.0.1:4300"
})
class ExternalConfigurationIntegrationTest {
    @Autowired Environment environment;
    @Autowired CorsConfig corsConfig;

    @Test
    void bindsAlternateIntegrationAndCorsValuesWithoutContactingProviders() {
        assertThat(corsConfig).isNotNull();
        assertThat(environment.getProperty("integrations.brapi.url")).endsWith("/custom-brapi");
        assertThat(environment.getProperty("integrations.twelvedata.url")).endsWith("/custom-twelve");
        assertThat(environment.getProperty("integrations.brasilapi.url")).endsWith("/custom-brasil");
        assertThat(environment.getProperty("integrations.viacep.url")).endsWith("/custom-viacep");
        assertThat(environment.getProperty("app.cors.allowed-origin")).isEqualTo("http://127.0.0.1:4300");
    }
}
