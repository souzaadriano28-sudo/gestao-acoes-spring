package com.trabalho.gestao_acoes.integrations.brasilapi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrasilApiAdapterTest {
    @Test
    void preservesTheUpstreamFailureAfterSanitizedLogging() {
        BrasilApiClient client = mock(BrasilApiClient.class);
        RuntimeException failure = new IllegalStateException("network unavailable");
        when(client.consultarCnpj("02332886000104")).thenThrow(failure);

        assertThatThrownBy(() -> new BrasilApiAdapter(client).buscarDadosPorCnpj("02332886000104"))
                .isSameAs(failure);
        verify(client).consultarCnpj("02332886000104");
    }
}
