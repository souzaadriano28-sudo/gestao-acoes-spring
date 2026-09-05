package com.trabalho.gestao_acoes.resources.exceptions;

import com.trabalho.gestao_acoes.services.exceptions.ApiException;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import feign.FeignException;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class ResourceExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<StandardError> api(ApiException ex, HttpServletRequest request) {
        List<StandardError.FieldViolation> fields = ex instanceof BusinessException business && business.getField() != null
                ? List.of(new StandardError.FieldViolation(business.getField(), business.getMessage())) : List.of();
        return response(ex.getStatus(), ex.getCode(), ex.getMessage(), request, fields);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<StandardError.FieldViolation> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new StandardError.FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(StandardError.FieldViolation::field)).toList();
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "Revise os campos informados.", request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<StandardError> constraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<StandardError.FieldViolation> fields = ex.getConstraintViolations().stream()
                .map(v -> new StandardError.FieldViolation(v.getPropertyPath().toString(), v.getMessage())).toList();
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "Revise os campos informados.", request, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardError> malformed(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "O corpo da requisição é inválido.", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<StandardError> malformedPath(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "O parâmetro da rota é inválido.", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<StandardError> method(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Método HTTP não permitido.", request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<StandardError> media(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Tipo de conteúdo não suportado.", request, List.of());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<StandardError> upstream(FeignException ex, HttpServletRequest request) {
        if (ex.status() == 404) return response(HttpStatus.NOT_FOUND, "UPSTREAM_NOT_FOUND", "Dado não encontrado no serviço externo.", request, List.of());
        if (ex.status() == 429) return response(HttpStatus.TOO_MANY_REQUESTS, "UPSTREAM_RATE_LIMIT", "Limite temporário do serviço externo atingido.", request, List.of());
        return response(HttpStatus.SERVICE_UNAVAILABLE, "UPSTREAM_UNAVAILABLE", "Serviço externo temporariamente indisponível.", request, List.of());
    }

    @ExceptionHandler({CannotAcquireLockException.class, PessimisticLockingFailureException.class,
            QueryTimeoutException.class, PessimisticLockException.class, LockTimeoutException.class})
    public ResponseEntity<StandardError> concurrent(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "CONCURRENT_OPERATION", "Outra operação está atualizando esta corretora. Tente novamente.", request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StandardError> integrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String detail = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase();
        if (detail.contains("cnpj")) return response(HttpStatus.CONFLICT, "DUPLICATE_CNPJ", "CNPJ já cadastrado.", request, List.of());
        if (detail.contains("ticker")) return response(HttpStatus.CONFLICT, "DUPLICATE_TICKER", "Ticker já cadastrado.", request, List.of());
        return response(HttpStatus.CONFLICT, "CONCURRENT_OPERATION", "Conflito ao persistir a operação.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> unexpected(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Ocorreu um erro interno inesperado.", request, List.of());
    }

    private ResponseEntity<StandardError> response(HttpStatus status, String code, String message,
                                                   HttpServletRequest request, List<StandardError.FieldViolation> fields) {
        StandardError error = new StandardError(Instant.now(), status.value(), code,
                status.getReasonPhrase(), message, request.getRequestURI(), fields);
        return ResponseEntity.status(status).body(error);
    }
}
