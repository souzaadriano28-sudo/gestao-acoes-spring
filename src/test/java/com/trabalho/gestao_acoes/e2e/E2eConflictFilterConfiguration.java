package com.trabalho.gestao_acoes.e2e;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/** Test-classpath-only fault boundary used by the browser contract suite. */
@Configuration
@Profile("test")
public class E2eConflictFilterConfiguration {
    @Bean
    FilterRegistrationBean<OncePerRequestFilter> e2eConflictFilter() {
        OncePerRequestFilter filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain chain) throws ServletException, IOException {
                boolean controlledConflict = "POST".equals(request.getMethod())
                        && request.getRequestURI().endsWith("/carteira/comprar")
                        && ("conflict".equals(request.getParameter("e2eFault"))
                                || "conflict".equals(request.getHeader("X-Atlas-E2E-Fault")));
                if (!controlledConflict) {
                    chain.doFilter(request, response);
                    return;
                }
                byte[] payload = ("{\"status\":409,\"code\":\"CONCURRENT_OPERATION\","
                        + "\"message\":\"Conflito concorrente controlado para E2E.\",\"fieldErrors\":[]}")
                        .getBytes(StandardCharsets.UTF_8);
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentLength(payload.length);
                response.getOutputStream().write(payload);
                response.flushBuffer();
            }
        };
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
