package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.InvalidQuoteException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyPolicy {
    public static final int PRICE_SCALE = 8;
    public static final int PRICE_PRECISION = 19;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private MoneyPolicy() {}

    public static BigDecimal quote(BigDecimal value) {
        if (value == null || value.signum() <= 0) throw new InvalidQuoteException("Cotação ausente ou não positiva.");
        BigDecimal normalized;
        try {
            normalized = value.setScale(PRICE_SCALE, ROUNDING);
        } catch (ArithmeticException ex) {
            throw new InvalidQuoteException("Cotação inválida.");
        }
        if (normalized.signum() <= 0 || normalized.precision() > PRICE_PRECISION) {
            throw new InvalidQuoteException("Cotação fora dos limites suportados.");
        }
        return normalized;
    }

    public static BigDecimal average(BigDecimal current, int currentQuantity, BigDecimal price, int quantity, int total) {
        try {
            BigDecimal cost = current.multiply(BigDecimal.valueOf(currentQuantity))
                    .add(price.multiply(BigDecimal.valueOf(quantity)));
            BigDecimal result = cost.divide(BigDecimal.valueOf(total), PRICE_SCALE, ROUNDING);
            if (result.precision() > PRICE_PRECISION) throw new ArithmeticException("precision");
            return result;
        } catch (ArithmeticException ex) {
            throw new BusinessException("NUMERIC_LIMIT_EXCEEDED", "O cálculo excede os limites numéricos suportados.");
        }
    }

    public static BigDecimal total(BigDecimal value) {
        try {
            return value.setScale(2, ROUNDING);
        } catch (ArithmeticException ex) {
            throw new BusinessException("NUMERIC_LIMIT_EXCEEDED", "O total excede os limites numéricos suportados.");
        }
    }
}
