package com.trabalho.gestao_acoes.resources.exceptions;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class ResourceExceptionHandler {

    // 1. Captura os erros de Regra de Negócio (Nossos RuntimeExceptions dos Services)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<StandardError> regrasDeNegocio(RuntimeException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST; // Código 400

        StandardError err = new StandardError(
                Instant.now(),
                status.value(),
                "Erro de Regra de Negócio",
                e.getMessage(), // Aqui entra a nossa mensagem: "O Ticker PETR4 já está cadastrado", etc.
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(err);
    }

    // 2. Captura os erros de Validação dos DTOs (As anotações @NotBlank e @Size)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> errosDeValidacao(MethodArgumentNotValidException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY; // Código 422

        // Pega a primeira mensagem de erro da lista do Spring Validation
        String mensagemExata = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        StandardError err = new StandardError(
                Instant.now(),
                status.value(),
                "Dados Inválidos",
                mensagemExata, // Ex: "O CNPJ deve ter entre 14 e 18 caracteres"
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(err);
    }

    // 3. Captura falhas das APIs externas (API fora do ar, Limite de requisições, etc)
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<StandardError> errosDeIntegracao(FeignException e, HttpServletRequest request) {
        // Por padrão, se o Feign falhar, dizemos que o serviço está indisponível (503)
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        String mensagemAmigavel = "Serviço externo temporariamente indisponível. Tente novamente mais tarde.";

        // Regra específica: Limite de requisições excedido (Código HTTP 429)
        if (e.status() == 429) {
            status = HttpStatus.TOO_MANY_REQUESTS;
            mensagemAmigavel = "O limite de consultas gratuitas na API externa foi excedido. Tente amanhã.";
        }
        // Regra específica: API não encontrou os dados (Código HTTP 404)
        else if (e.status() == 404) {
            status = HttpStatus.NOT_FOUND;
            mensagemAmigavel = "Dados não encontrados na base de dados externa (Receita, CVM ou Bolsa).";
        }

        StandardError err = new StandardError(
                Instant.now(),
                status.value(),
                "Falha de Comunicação Externa",
                mensagemAmigavel,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(err);
    }
}