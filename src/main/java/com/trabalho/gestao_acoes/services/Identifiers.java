package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.services.exceptions.BusinessException;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Identifiers {
    private static final Pattern CNPJ_DIGITS = Pattern.compile("\\d{14}");
    private static final Pattern CNPJ_MASK = Pattern.compile("\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}");

    private Identifiers() {}

    public static String ticker(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "O ticker é obrigatório.", "ticker");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public static String mercado(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "O mercado é obrigatório.", "mercado");
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "BRASIL", "NACIONAL" -> "BRASIL";
            case "AMERICANO", "INTERNACIONAL" -> "AMERICANO";
            default -> throw new BusinessException("VALIDATION_ERROR", "Mercado não suportado.", "mercado");
        };
    }

    public static String cnpjFromBody(String value) {
        if (value == null) {
            throw new BusinessException("VALIDATION_ERROR", "O CNPJ é obrigatório.", "cnpj");
        }
        String trimmed = value.trim();
        if (!CNPJ_DIGITS.matcher(trimmed).matches() && !CNPJ_MASK.matcher(trimmed).matches()) {
            throw new BusinessException("VALIDATION_ERROR", "Informe o CNPJ com 14 dígitos ou na máscara convencional.", "cnpj");
        }
        return validateCnpj(trimmed.replaceAll("[./-]", ""));
    }

    public static String cnpjFromPath(String value) {
        if (value == null || !CNPJ_DIGITS.matcher(value).matches()) {
            throw new BusinessException("VALIDATION_ERROR", "A consulta exige CNPJ com 14 dígitos, sem máscara.", "cnpj");
        }
        return validateCnpj(value);
    }

    private static String validateCnpj(String digits) {
        if (digits.chars().distinct().count() == 1 || checkDigit(digits, 12) != digits.charAt(12) - '0'
                || checkDigit(digits, 13) != digits.charAt(13) - '0') {
            throw new BusinessException("VALIDATION_ERROR", "CNPJ com dígitos verificadores inválidos.", "cnpj");
        }
        return digits;
    }

    private static int checkDigit(String digits, int length) {
        int[] weights = length == 12
                ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < length; i++) sum += (digits.charAt(i) - '0') * weights[i];
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    public static void validateTickerForMarket(String ticker, String mercado) {
        boolean valid = "BRASIL".equals(mercado)
                ? ticker.matches("^[A-Z]{4}\\d{1,2}$")
                : ticker.matches("^[A-Z]{1,5}$");
        if (!valid) throw new BusinessException("VALIDATION_ERROR", "Ticker inválido para o mercado informado.", "ticker");
    }

    public static void validateTickerSyntax(String ticker) {
        if (!ticker.matches("^[A-Z]{1,5}$|^[A-Z]{4}\\d{1,2}$")) {
            throw new BusinessException("VALIDATION_ERROR", "Ticker inválido.", "ticker");
        }
    }
}
