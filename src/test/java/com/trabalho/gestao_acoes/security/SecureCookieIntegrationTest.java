package com.trabalho.gestao_acoes.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={"spring.datasource.url=jdbc:h2:mem:secure-cookie;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","server.servlet.session.cookie.secure=true"})
@ActiveProfiles("test")
@DirtiesContext
class SecureCookieIntegrationTest {
    @LocalServerPort int port;
    @Test void productionHttpsProfileEmitsSecureHttpOnlySameSiteCookie() throws Exception {
        var response=HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/auth/csrf")).GET().build(),
                HttpResponse.BodyHandlers.discarding());
        String cookie=response.headers().firstValue("set-cookie").orElseThrow();
        assertThat(cookie).contains("ATLAS_SESSION=", "Path=/", "HttpOnly", "Secure", "SameSite=Lax");
    }
}
