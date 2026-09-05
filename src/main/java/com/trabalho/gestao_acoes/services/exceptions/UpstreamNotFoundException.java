package com.trabalho.gestao_acoes.services.exceptions;

import org.springframework.http.HttpStatus;

public class UpstreamNotFoundException extends ApiException {
    public UpstreamNotFoundException(String message) { super(HttpStatus.NOT_FOUND, "UPSTREAM_NOT_FOUND", message); }
}
