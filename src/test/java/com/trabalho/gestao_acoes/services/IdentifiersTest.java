package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentifiersTest {
    @Test
    void normalizesTickerAndAllMarketAliases() {
        assertThat(Identifiers.ticker(" petr4 ")).isEqualTo("PETR4");
        assertThat(Identifiers.mercado(" nacional ")).isEqualTo("BRASIL");
        assertThat(Identifiers.mercado("brasil")).isEqualTo("BRASIL");
        assertThat(Identifiers.mercado(" internacional ")).isEqualTo("AMERICANO");
        assertThat(Identifiers.mercado("americano")).isEqualTo("AMERICANO");
    }

    @Test
    void acceptsOnlyTheTwoDocumentedBodyCnpjFormatsAndCanonicalizesThem() {
        assertThat(Identifiers.cnpjFromBody(" 11.222.333/0001-81 ")).isEqualTo("11222333000181");
        assertThat(Identifiers.cnpjFromBody("11222333000181")).isEqualTo("11222333000181");
        assertThatThrownBy(() -> Identifiers.cnpjFromBody("11 222 333 0001 81"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void pathRequiresDigitsAndEveryCnpjRequiresValidCheckDigits() {
        assertThat(Identifiers.cnpjFromPath("11222333000181")).isEqualTo("11222333000181");
        assertThatThrownBy(() -> Identifiers.cnpjFromPath("11.222.333/0001-81"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> Identifiers.cnpjFromBody("11222333000182"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> Identifiers.cnpjFromBody("00000000000000"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validatesTickerSyntaxForQueriesWithoutMarketContext() {
        org.assertj.core.api.Assertions.assertThatCode(() -> Identifiers.validateTickerSyntax("PETR4")).doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(() -> Identifiers.validateTickerSyntax("AAPL")).doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Identifiers.validateTickerSyntax("$$"))
                .isInstanceOf(com.trabalho.gestao_acoes.services.exceptions.BusinessException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Identifiers.validateTickerSyntax("ABC123"))
                .isInstanceOf(com.trabalho.gestao_acoes.services.exceptions.BusinessException.class);
    }
}
