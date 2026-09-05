package com.trabalho.gestao_acoes.services.exceptions;

import org.springframework.http.HttpStatus;

public class UpstreamInvalidResponseException extends ApiException {
    public UpstreamInvalidResponseException(String message) { super(HttpStatus.BAD_GATEWAY, "UPSTREAM_INVALID_RESPONSE", message); }
}
