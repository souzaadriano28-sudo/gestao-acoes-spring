package com.trabalho.gestao_acoes.resources.exceptions;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public class StandardError implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Instant timestamp;
    private final Integer status;
    private final String code;
    private final String error;
    private final String message;
    private final String path;
    private final List<FieldViolation> fieldErrors;

    public StandardError(Instant timestamp, Integer status, String code, String error,
                         String message, String path, List<FieldViolation> fieldErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.code = code;
        this.error = error;
        this.message = message;
        this.path = path;
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public Instant getTimestamp() { return timestamp; }
    public Integer getStatus() { return status; }
    public String getCode() { return code; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public List<FieldViolation> getFieldErrors() { return fieldErrors; }

    public record FieldViolation(String field, String message) implements Serializable {}
}
