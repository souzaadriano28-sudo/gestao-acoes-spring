package com.trabalho.gestao_acoes.services.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidQuoteException extends ApiException {
    public InvalidQuoteException(String message) { super(HttpStatus.BAD_GATEWAY, "INVALID_QUOTE", message); }
}
