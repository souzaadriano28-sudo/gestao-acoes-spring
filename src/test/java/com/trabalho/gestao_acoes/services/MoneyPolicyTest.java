package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.services.exceptions.InvalidQuoteException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyPolicyTest {
    @Test
    void appliesScaleEightAndHalfUpToQuotesAndWeightedAverage() {
        assertThat(MoneyPolicy.quote(new BigDecimal("10.123456785")))
                .isEqualByComparingTo("10.12345679");
        assertThat(MoneyPolicy.average(new BigDecimal("10.00000000"), 1,
                new BigDecimal("10.01000000"), 2, 3)).isEqualByComparingTo("10.00666667");
    }

    @Test
    void roundsOnlyTheFinalPortfolioTotalToTwoPlaces() {
        BigDecimal unrounded = new BigDecimal("50.004").add(new BigDecimal("50.003"));
        assertThat(MoneyPolicy.total(unrounded)).isEqualByComparingTo("100.01");
    }

    @Test
    void rejectsNonPositiveOverflowingAndRoundedToZeroQuotes() {
        assertThatThrownBy(() -> MoneyPolicy.quote(BigDecimal.ZERO)).isInstanceOf(InvalidQuoteException.class);
        assertThatThrownBy(() -> MoneyPolicy.quote(new BigDecimal("-1"))).isInstanceOf(InvalidQuoteException.class);
        assertThatThrownBy(() -> MoneyPolicy.quote(new BigDecimal("0.000000001"))).isInstanceOf(InvalidQuoteException.class);
        assertThatThrownBy(() -> MoneyPolicy.quote(new BigDecimal("123456789012.12345678")))
                .isInstanceOf(InvalidQuoteException.class);
    }
}
