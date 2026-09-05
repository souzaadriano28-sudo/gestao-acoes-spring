package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.dtos.AcaoDTO;
import com.trabalho.gestao_acoes.domains.dtos.CorretoraDTO;
import com.trabalho.gestao_acoes.integrations.brasilapi.BrasilApiResponse;
import com.trabalho.gestao_acoes.integrations.viacep.ViaCepResponse;
import com.trabalho.gestao_acoes.repositories.AcaoRepository;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.ports.CepClientPort;
import com.trabalho.gestao_acoes.services.ports.CnpjClientPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {
    @Test
    void rejectsClientSuppliedAssetIdBeforeQuoteOrPersistence() {
        AcaoRepository repository = mock(AcaoRepository.class);
        CotacaoService quotes = mock(CotacaoService.class);
        AcaoDTO dto = new AcaoDTO();
        dto.setId(99L); dto.setTicker("PETR4"); dto.setMercado("BRASIL");

        assertThatThrownBy(() -> new AcaoService(repository, quotes).insert(dto))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(repository, quotes);
    }

    @Test
    void normalizesCnpjBeforeCallingFixedCompanyAndAddressStubs() {
        CorretoraRepository repository = mock(CorretoraRepository.class);
        CnpjClientPort companyClient = mock(CnpjClientPort.class);
        CepClientPort addressClient = mock(CepClientPort.class);
        BrasilApiResponse company = new BrasilApiResponse();
        company.setRazaoSocial("Corretora Teste"); company.setNomeFantasia("Teste");
        company.setDescricaoSituacaoCadastral("ATIVA"); company.setCnaeFiscal(6612601);
        ViaCepResponse address = new ViaCepResponse();
        address.setCep("01001-000"); address.setLogradouro("Praça da Sé");
        address.setBairro("Sé"); address.setLocalidade("São Paulo"); address.setUf("SP");
        when(companyClient.buscarDadosPorCnpj("11222333000181")).thenReturn(company);
        when(addressClient.buscarEnderecoPorCep("01001000")).thenReturn(address);
        when(repository.save(any())).thenAnswer(call -> {
            var entity = call.getArgument(0, com.trabalho.gestao_acoes.domains.Corretora.class);
            entity.setId(1L); return entity;
        });
        CorretoraDTO dto = new CorretoraDTO();
        dto.setCnpj(" 11.222.333/0001-81 "); dto.setCep("01001000");

        CorretoraDTO result = new CorretoraService(repository, companyClient, addressClient).insert(dto);

        assertThat(result.getCnpj()).isEqualTo("11222333000181");
        assertThat(result.getRazaoSocial()).isEqualTo("Corretora Teste");
        verify(companyClient).buscarDadosPorCnpj("11222333000181");
        verify(addressClient).buscarEnderecoPorCep("01001000");
    }

    @Test
    void invalidCnpjAndClientSuppliedBrokerIdHaveNoExternalSideEffects() {
        CorretoraRepository repository = mock(CorretoraRepository.class);
        CnpjClientPort companyClient = mock(CnpjClientPort.class);
        CepClientPort addressClient = mock(CepClientPort.class);
        CorretoraService service = new CorretoraService(repository, companyClient, addressClient);
        CorretoraDTO invalid = new CorretoraDTO(); invalid.setCnpj("11222333000182"); invalid.setCep("01001000");
        assertThatThrownBy(() -> service.insert(invalid)).isInstanceOf(BusinessException.class);
        CorretoraDTO withId = new CorretoraDTO(); withId.setId(5L); withId.setCnpj("11222333000181"); withId.setCep("01001000");
        assertThatThrownBy(() -> service.insert(withId)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(repository, companyClient, addressClient);
    }
}
