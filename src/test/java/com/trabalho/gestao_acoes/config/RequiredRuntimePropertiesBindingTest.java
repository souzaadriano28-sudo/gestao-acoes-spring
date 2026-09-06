package com.trabalho.gestao_acoes.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class RequiredRuntimePropertiesBindingTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindingFailsFastForBlankExternalSecretAndNamesTheVariable() {
        runner.withPropertyValues(
                "app.required.db-password= ",
                "app.required.brapi-token=fake-but-nonblank",
                "app.required.twelvedata-api-key=fake-but-nonblank",
                "app.required.admin-initial-username=atlas-admin",
                "app.required.admin-initial-password=fake-admin-password")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()).getMessage())
                            .contains("app.required.db-password", "DB_PASSWORD is required")
                            .doesNotContain("fake-but-nonblank");
                });
    }

    @Test
    void bindingAcceptsCompleteExternalConfiguration() {
        runner.withPropertyValues(
                "app.required.db-password=fake-db",
                "app.required.brapi-token=fake-brapi",
                "app.required.twelvedata-api-key=fake-twelve",
                "app.required.admin-initial-username=atlas-admin",
                "app.required.admin-initial-password=fake-admin-password")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RequiredRuntimeProperties.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RequiredRuntimeProperties.class)
    static class PropertiesConfiguration { }

    private static Throwable rootCause(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) current = current.getCause();
        return current;
    }
}
