package com.trabalho.gestao_acoes.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trabalho.gestao_acoes.services.CarteiraService;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.ConflictException;
import com.trabalho.gestao_acoes.services.exceptions.InvalidQuoteException;
import com.trabalho.gestao_acoes.services.exceptions.NotFoundException;
import com.trabalho.gestao_acoes.resources.exceptions.ResourceExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CarteiraResourceContractTest {
    private CarteiraService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(CarteiraService.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new CarteiraResource(service))
                .setControllerAdvice(new ResourceExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @Test
    void wrongJsonTypesDecimalsAndOutOfRangeIntegersAreMalformedWithoutSideEffects() throws Exception {
        for (String qtd : new String[]{"\"10\"", "10.0", "1.5", "999999999999999999999"}) {
            mvc.perform(post("/carteira/comprar").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\",\"qtd\":" + qtd + ",\"corretoraId\":1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                    .andExpect(jsonPath("$.path").value("/carteira/comprar"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void missingBlankAndNonPositiveFieldsReturnAllValidationErrors() throws Exception {
        mvc.perform(post("/carteira/comprar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\" \",\"mercado\":\"\",\"qtd\":0,\"corretoraId\":-1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));
        verifyNoInteractions(service);
    }

    @Test
    void successfulPurchaseAndSaleKeepTheExistingEmptyBodyContract() throws Exception {
        String body = "{\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\",\"qtd\":10,\"corretoraId\":1}";
        mvc.perform(post("/carteira/comprar").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(content().string(""));
        mvc.perform(post("/carteira/vender").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(content().string(""));
    }

    @Test
    void classifiedFailuresUseTheStandardEnvelope() throws Exception {
        String body = "{\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\",\"qtd\":1,\"corretoraId\":1}";
        assertFailure(body, new BusinessException("INSUFFICIENT_POSITION", "insuficiente"), 422, "INSUFFICIENT_POSITION");
        assertFailure(body, new NotFoundException("ausente"), 404, "RESOURCE_NOT_FOUND");
        assertFailure(body, new ConflictException("CONCURRENT_OPERATION", "conflito"), 409, "CONCURRENT_OPERATION");
        assertFailure(body, new InvalidQuoteException("cotação inválida"), 502, "INVALID_QUOTE");
    }

    @Test
    void unsupportedMethodAndContentTypeKeepTheirHttpSemantics() throws Exception {
        mvc.perform(get("/carteira/comprar"))
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        mvc.perform(post("/carteira/comprar").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    private void assertFailure(String body, RuntimeException error, int status, String code) throws Exception {
        reset(service);
        doThrow(error).when(service).comprar(any(), any(), any(), any());
        mvc.perform(post("/carteira/comprar").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.timestamp").exists()).andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists()).andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
