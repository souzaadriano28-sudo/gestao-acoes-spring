package com.trabalho.gestao_acoes.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequiredRuntimePropertiesTest {
    @Test
    void rejectsBlankRequiredValuesWithoutIncludingTheirContents() {
        RequiredRuntimeProperties properties = new RequiredRuntimeProperties();
        properties.setDbPassword("  ");
        properties.setBrapiToken("");
        properties.setTwelvedataApiKey(null);
        properties.setAdminInitialUsername("");
        properties.setAdminInitialPassword(" ");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var messages = factory.getValidator().validate(properties).stream()
                    .map(violation -> violation.getMessage()).toList();
            assertThat(messages).containsExactlyInAnyOrder(
                    "DB_PASSWORD is required", "BRAPI_TOKEN is required", "TWELVEDATA_API_KEY is required",
                    "ADMIN_INITIAL_USERNAME is required", "ADMIN_INITIAL_PASSWORD is required");
        }
    }

    @Test
    void acceptsNonBlankExternalValues() {
        RequiredRuntimeProperties properties = new RequiredRuntimeProperties();
        properties.setDbPassword("external-db-value");
        properties.setBrapiToken("external-brapi-value");
        properties.setTwelvedataApiKey("external-twelve-value");
        properties.setAdminInitialUsername("atlas-admin");
        properties.setAdminInitialPassword("external-admin-value");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties)).isEmpty();
        }
    }
}
