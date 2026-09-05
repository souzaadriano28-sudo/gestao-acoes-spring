package com.trabalho.gestao_acoes.resources;

import com.trabalho.gestao_acoes.services.AcaoService;
import com.trabalho.gestao_acoes.resources.exceptions.ResourceExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdentifierResourceContractTest {
    @Test
    void invalidNumericPathParameterUsesMalformedRequestEnvelope() throws Exception {
        AcaoService service = mock(AcaoService.class);
        AcaoResource resource = new AcaoResource();
        ReflectionTestUtils.setField(resource, "service", service);
        var mvc = MockMvcBuilders.standaloneSetup(resource)
                .setControllerAdvice(new ResourceExceptionHandler()).build();

        mvc.perform(get("/acoes/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.path").value("/acoes/not-a-number"));
        verifyNoInteractions(service);
    }
}
