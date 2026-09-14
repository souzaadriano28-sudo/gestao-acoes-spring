package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.Acao;
import com.trabalho.gestao_acoes.domains.dtos.AcaoDTO;
import com.trabalho.gestao_acoes.domains.dtos.CorretoraDTO;
import com.trabalho.gestao_acoes.integrations.brasilapi.BrasilApiResponse;
import com.trabalho.gestao_acoes.integrations.viacep.ViaCepResponse;
import com.trabalho.gestao_acoes.repositories.AcaoRepository;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.ConflictException;
import com.trabalho.gestao_acoes.services.exceptions.UpstreamNotFoundException;
import com.trabalho.gestao_acoes.services.ports.CepClientPort;
import com.trabalho.gestao_acoes.services.ports.CnpjClientPort;
import com.trabalho.gestao_acoes.services.ports.RegulatoryRegistryPort;
import com.trabalho.gestao_acoes.services.ports.RegulatoryRegistrySnapshot;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {
    private static final String CNPJ = "11222333000181";
    private static final Instant NOW = Instant.parse("2026-09-07T15:00:00Z");

    @Test
    void rejectsClientSuppliedAssetIdBeforeQuoteOrPersistence() {
        AcaoRepository repository = mock(AcaoRepository.class); CotacaoService quotes = mock(CotacaoService.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        com.trabalho.gestao_acoes.domains.UserAccount user = new com.trabalho.gestao_acoes.domains.UserAccount(); user.setId(2L);
        when(securityUtils.currentOwnerId()).thenReturn(2L);
        when(securityUtils.currentUser()).thenReturn(user);
        AcaoDTO dto = new AcaoDTO(); dto.setId(99L); dto.setTicker("PETR4"); dto.setMercado("BRASIL");
        assertThatThrownBy(() -> new AcaoService(repository, quotes, securityUtils).insert(dto)).isInstanceOf(BusinessException.class);

        verifyNoInteractions(repository, quotes);
    }

    @Test
    void scopesTickerUniquenessToOwner() {
        AcaoRepository repository = mock(AcaoRepository.class);
        CotacaoService quotes = mock(CotacaoService.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        var firstOwner = new com.trabalho.gestao_acoes.domains.UserAccount(); firstOwner.setId(1L);
        var secondOwner = new com.trabalho.gestao_acoes.domains.UserAccount(); secondOwner.setId(2L);
        var currentOwner = new com.trabalho.gestao_acoes.domains.UserAccount[] {firstOwner};
        var registered = new java.util.HashSet<String>();
        when(securityUtils.currentOwnerId()).thenAnswer(invocation -> currentOwner[0].getId());
        when(securityUtils.currentUser()).thenAnswer(invocation -> currentOwner[0]);
        when(repository.findByTickerAndMercadoAndOwnerId(anyString(), anyString(), anyLong())).thenAnswer(invocation ->
                registered.contains(invocation.getArgument(2) + ":" + invocation.getArgument(0) + ":" + invocation.getArgument(1))
                        ? java.util.Optional.of(new Acao()) : java.util.Optional.empty());
        when(repository.save(any(Acao.class))).thenAnswer(invocation -> {
            Acao saved = invocation.getArgument(0);
            registered.add(saved.getOwner().getId() + ":" + saved.getTicker() + ":" + saved.getMercado());
            return saved;
        });
        var quote = new com.trabalho.gestao_acoes.services.ports.CotacaoBolsa(new java.math.BigDecimal("20"), "BRL");
        quote.setNomeEmpresa("PetrÃ³leo Brasileiro S.A.");
        when(quotes.buscar("PETR4", "BRASIL")).thenReturn(quote);

        AcaoService service = new AcaoService(repository, quotes, securityUtils);
        service.insert(assetRequest("PETR4"));
        currentOwner[0] = secondOwner;
        assertThatCode(() -> service.insert(assetRequest("PETR4"))).doesNotThrowAnyException();
        currentOwner[0] = firstOwner;
        assertThatThrownBy(() -> service.insert(assetRequest("PETR4")))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo("DUPLICATE_TICKER");
    }

    @Test
    void completesRegistrationWithNormalizedIdentifiersAndOfficialEvidence() {
        Fixture f = fixture(activeEntry("CORRETORAS"));
        when(f.repository.save(any())).thenAnswer(call -> { Corretora entity = call.getArgument(0); entity.setId(1L); return entity; });
        CorretoraDTO result = f.service.insert(request(" 11.222.333/0001-81 ", "01001-000"));
        assertThat(result.getCnpj()).isEqualTo(CNPJ);
        assertThat(result.getCep()).isEqualTo("01001000");
        assertThat(result.getRazaoSocial()).isEqualTo("Corretora Teste");
        assertThat(result.getValidadaNaCvm()).isTrue();
        assertThat(result.getRegulatoryEvidence().category()).isEqualTo("CORRETORAS");
        verify(f.company).buscarDadosPorCnpj(CNPJ); verify(f.address).buscarEnderecoPorCep("01001000");
    }

    @Test
    void rejectsInvalidMissingInactiveAndDuplicateCompaniesBeforePersistence() {
        Fixture invalid = fixture(activeEntry("CORRETORAS"));
        assertThatThrownBy(() -> invalid.service.consultCnpj("11222333000182")).isInstanceOf(BusinessException.class);
        verifyNoInteractions(invalid.company, invalid.registry);

        Fixture missing = fixture(activeEntry("CORRETORAS"));
        when(missing.company.buscarDadosPorCnpj(CNPJ)).thenThrow(new UpstreamNotFoundException("CNPJ não encontrado."));
        assertThatThrownBy(() -> missing.service.consultCnpj(CNPJ)).isInstanceOf(UpstreamNotFoundException.class);
        verifyNoInteractions(missing.registry);

        Fixture inactive = fixture(activeEntry("CORRETORAS")); inactive.companyResponse.setDescricaoSituacaoCadastral("BAIXADA");
        assertThatThrownBy(() -> inactive.service.consultCnpj(CNPJ)).isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("BUSINESS_INACTIVE");
        verifyNoInteractions(inactive.registry);

        Fixture duplicate = fixture(activeEntry("CORRETORAS")); when(duplicate.repository.findByCnpjAndOwnerId(CNPJ, 2L)).thenReturn(Optional.of(new Corretora()));
        assertThatThrownBy(() -> duplicate.service.consultCnpj(CNPJ)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(duplicate.company, duplicate.registry);
    }

    @Test
    void reportsNotFoundInactiveAndIncompatibleCvmWithoutAuthorizing() {
        Fixture absent = fixture();
        assertThat(absent.service.consultCnpj(CNPJ).autorizadaPelaCvm()).isFalse();
        assertThatThrownBy(() -> absent.service.insert(request(CNPJ, "01001000"))).isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("CVM_NOT_FOUND");

        Fixture inactive = fixture(entry("CORRETORAS", "CANCELADA"));
        assertThat(inactive.service.consultCnpj(CNPJ).situacaoCvm()).isEqualTo("Fora de funcionamento normal");
        assertThatThrownBy(() -> inactive.service.insert(request(CNPJ, "01001000"))).extracting("code").isEqualTo("CVM_INACTIVE");

        Fixture incompatible = fixture(activeEntry("CUSTODIANTES DE VALORES MOBILIÁRIOS"));
        assertThat(incompatible.service.consultCnpj(CNPJ).categoriaCvm()).contains("CUSTODIANTES");
        assertThatThrownBy(() -> incompatible.service.insert(request(CNPJ, "01001000"))).extracting("code").isEqualTo("CVM_CATEGORY_INCOMPATIBLE");
    }

    @Test
    void cnaeIsComplementaryAndCannotBypassPostValidation() {
        Fixture f = fixture(); f.companyResponse.setCnaeFiscal(6612601);
        assertThat(f.service.consultCnpj(CNPJ).cnaeCompativel()).isTrue();
        CorretoraDTO forged = request(CNPJ, "01001000"); forged.setValidadaNaCvm(true); forged.setRazaoSocial("Forjada");
        assertThatThrownBy(() -> f.service.insert(forged)).isInstanceOf(BusinessException.class);
        verify(f.repository, never()).save(any()); verifyNoInteractions(f.address);
    }

    @Test
    void rejectsUnknownCepAfterSuccessfulRegulatoryValidation() {
        Fixture f = fixture(activeEntry("DISTRIBUIDORAS")); ViaCepResponse missing = new ViaCepResponse(); missing.setErro(true);
        when(f.address.buscarEnderecoPorCep("01001000")).thenReturn(missing);
        assertThatThrownBy(() -> f.service.insert(request(CNPJ, "01001-000"))).isInstanceOf(UpstreamNotFoundException.class);
        verify(f.repository, never()).save(any());
    }

    private static AcaoDTO assetRequest(String ticker) {
        AcaoDTO request = new AcaoDTO();
        request.setTicker(ticker);
        request.setMercado("BRASIL");
        return request;
    }

    private static Fixture fixture(RegulatoryRegistrySnapshot.RegulatoryEntry... entries) {
        CorretoraRepository repository = mock(CorretoraRepository.class); CnpjClientPort company = mock(CnpjClientPort.class);
        CepClientPort address = mock(CepClientPort.class); RegulatoryRegistryPort registry = mock(RegulatoryRegistryPort.class);
        BrasilApiResponse companyResponse = new BrasilApiResponse(); companyResponse.setRazaoSocial("Corretora Teste");
        companyResponse.setNomeFantasia("Teste"); companyResponse.setDescricaoSituacaoCadastral("ATIVA");
        companyResponse.setCnaeFiscal(6612601); companyResponse.setCep("01001-000");
        ViaCepResponse addressResponse = new ViaCepResponse(); addressResponse.setCep("01001-000");
        addressResponse.setLogradouro("Praça da Sé"); addressResponse.setBairro("Sé");
        addressResponse.setLocalidade("São Paulo"); addressResponse.setUf("SP");
        when(company.buscarDadosPorCnpj(CNPJ)).thenReturn(companyResponse);
        when(address.buscarEnderecoPorCep("01001000")).thenReturn(addressResponse);
        Map<String, List<RegulatoryRegistrySnapshot.RegulatoryEntry>> map = entries.length == 0 ? Map.of() : Map.of(CNPJ, List.of(entries));
        when(registry.load()).thenReturn(new RegulatoryRegistrySnapshot("CVM", NOW.minus(Duration.ofHours(1)), NOW, map));
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        RegulatoryVerificationService verification = new RegulatoryVerificationService(registry, clock, Duration.ofDays(7),
                Set.of("CORRETORAS", "DISTRIBUIDORAS", "BANCOS DE INVESTIMENTOS", "BANCOS MÚLTIPLOS COM CARTEIRA DE INVESTIMENTO"));
        com.trabalho.gestao_acoes.repositories.UserAccountRepository userRepository = mock(com.trabalho.gestao_acoes.repositories.UserAccountRepository.class);

        SecurityUtils securityUtils = mock(SecurityUtils.class);
        com.trabalho.gestao_acoes.domains.UserAccount user = new com.trabalho.gestao_acoes.domains.UserAccount();
        user.setId(2L);
        when(securityUtils.currentOwnerId()).thenReturn(2L);
        when(securityUtils.currentUser()).thenReturn(user);
        CorretoraService service = new CorretoraService(repository, company, address, verification, clock, Duration.ofDays(7), securityUtils);


        return new Fixture(repository, company, address, registry, companyResponse, service);
    }

    private static RegulatoryRegistrySnapshot.RegulatoryEntry activeEntry(String category) { return entry(category, "EM FUNCIONAMENTO NORMAL"); }
    private static RegulatoryRegistrySnapshot.RegulatoryEntry entry(String category, String status) { return new RegulatoryRegistrySnapshot.RegulatoryEntry(category, status, "123"); }
    private static CorretoraDTO request(String cnpj, String cep) { CorretoraDTO dto = new CorretoraDTO(); dto.setCnpj(cnpj); dto.setCep(cep); dto.setNumero("10"); return dto; }
    private record Fixture(CorretoraRepository repository, CnpjClientPort company, CepClientPort address,
            RegulatoryRegistryPort registry, BrasilApiResponse companyResponse, CorretoraService service) {}
}
