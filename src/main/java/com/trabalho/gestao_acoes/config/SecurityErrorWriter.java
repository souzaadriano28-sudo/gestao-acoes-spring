package com.trabalho.gestao_acoes.config;

import com.trabalho.gestao_acoes.resources.exceptions.StandardError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class SecurityErrorWriter {
    private final ObjectMapper mapper;
    public SecurityErrorWriter(ObjectMapper mapper) { this.mapper=mapper; }
    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value()); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        mapper.writeValue(response.getOutputStream(), new StandardError(Instant.now(),status.value(),code,status.getReasonPhrase(),message,request.getRequestURI(), List.of()));
    }
}
