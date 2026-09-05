package com.trabalho.gestao_acoes.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trabalho.gestao_acoes.resources.exceptions.ResourceExceptionHandler;
import feign.FeignException;
import jakarta.persistence.LockTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceExceptionHandlerTest {
    private final ResourceExceptionHandler handler = new ResourceExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/acoes");

    @Test
    void mapsExternalRateLimitNotFoundAndUnavailabilityWithoutLeakingProviderMessage() throws Exception {
        assertUpstream(404, HttpStatus.NOT_FOUND, "UPSTREAM_NOT_FOUND");
        assertUpstream(429, HttpStatus.TOO_MANY_REQUESTS, "UPSTREAM_RATE_LIMIT");
        FeignException failure = mock(FeignException.class);
        when(failure.status()).thenReturn(500);
        when(failure.getMessage()).thenReturn("provider-secret-value");
        var response = handler.upstream(failure, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getCode()).isEqualTo("UPSTREAM_UNAVAILABLE");
        assertThat(new ObjectMapper().findAndRegisterModules().writeValueAsString(response.getBody()))
                .doesNotContain("provider-secret-value");
    }

    @Test
    void mapsCanonicalDuplicatesLockTimeoutAndUnexpectedFailure() {
        var cnpj = handler.integrity(new DataIntegrityViolationException("x",
                new RuntimeException("uk_corretora_cnpj_canonical")), request);
        assertThat(cnpj.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(cnpj.getBody().getCode()).isEqualTo("DUPLICATE_CNPJ");

        var ticker = handler.integrity(new DataIntegrityViolationException("x",
                new RuntimeException("uk_acao_ticker_canonical")), request);
        assertThat(ticker.getBody().getCode()).isEqualTo("DUPLICATE_TICKER");

        var lock = handler.concurrent(new LockTimeoutException("timeout"), request);
        assertThat(lock.getBody().getCode()).isEqualTo("CONCURRENT_OPERATION");
        assertThat(handler.unexpected(new RuntimeException("internal-secret"), request).getBody().getMessage())
                .doesNotContain("internal-secret");
    }

    private void assertUpstream(int providerStatus, HttpStatus expected, String code) {
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(providerStatus);
        var response = handler.upstream(exception, request);
        assertThat(response.getStatusCode()).isEqualTo(expected);
        assertThat(response.getBody().getCode()).isEqualTo(code);
    }
}
