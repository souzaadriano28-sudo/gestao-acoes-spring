package com.trabalho.gestao_acoes.resources;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@CrossOrigin(origins = "http://localhost:4200")
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> resposta = new HashMap<>();
        resposta.put("message", ex.getMessage());


        return ResponseEntity.badRequest().body(resposta);
    }
}