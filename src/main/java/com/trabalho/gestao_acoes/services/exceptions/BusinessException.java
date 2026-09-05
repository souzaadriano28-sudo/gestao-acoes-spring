package com.trabalho.gestao_acoes.services.exceptions;

import org.springframework.http.HttpStatus;

public class BusinessException extends ApiException {
    private final String field;

    public BusinessException(String code, String message) {
        this(code, message, null);
    }

    public BusinessException(String code, String message, String field) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
        this.field = field;
    }

    public String getField() { return field; }
}
