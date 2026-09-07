package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.dtos.CorretoraDTO;
import com.trabalho.gestao_acoes.domains.dtos.broker.BrokerAddressPreviewDTO;
import com.trabalho.gestao_acoes.domains.dtos.broker.BrokerCnpjPreviewDTO;
import com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus;
import com.trabalho.gestao_acoes.integrations.brasilapi.BrasilApiResponse;
import com.trabalho.gestao_acoes.integrations.viacep.ViaCepResponse;
import com.trabalho.gestao_acoes.mappers.CorretoraMapper;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.services.RegulatoryVerificationService.Decision;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.ConflictException;
import com.trabalho.gestao_acoes.services.exceptions.NotFoundException;
import com.trabalho.gestao_acoes.services.exceptions.UpstreamInvalidResponseException;
import com.trabalho.gestao_acoes.services.exceptions.UpstreamNotFoundException;
import com.trabalho.gestao_acoes.services.ports.CepClientPort;
import com.trabalho.gestao_acoes.services.ports.CnpjClientPort;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CorretoraService {
    private static final Set<Integer> COMPLEMENTARY_CNAES = Set.of(6612601, 6612602, 6431900);
    private final CorretoraRepository repository;
    private final CnpjClientPort cnpjClient;
    private final CepClientPort cepClient;
    private final RegulatoryVerificationService regulatoryVerification;
    private final Clock clock;
    private final Duration regulatoryFreshness;

    @Autowired
    public CorretoraService(CorretoraRepository repository, CnpjClientPort cnpjClient, CepClientPort cepClient,
            RegulatoryVerificationService regulatoryVerification, Clock clock,
            @Value("${app.regulatory.cvm.freshness:P7D}") Duration regulatoryFreshness) {
        this.repository = repository;
        this.cnpjClient = cnpjClient;
        this.cepClient = cepClient;
        this.regulatoryVerification = regulatoryVerification;
        this.clock = clock;
        this.regulatoryFreshness = regulatoryFreshness;
    }

    public BrokerCnpjPreviewDTO consultCnpj(String input) {
        String cnpj = Identifiers.cnpjFromBody(input);
        rejectDuplicate(cnpj);
        BrasilApiResponse company = cnpjClient.buscarDadosPorCnpj(cnpj);
        validateCompany(company);
        Decision decision = regulatoryVerification.verify(cnpj);
        return new BrokerCnpjPreviewDTO(cnpj, company.getRazaoSocial(), fantasyName(company),
                company.getDescricaoSituacaoCadastral(), normalizedSuggestion(company.getCep()),
                COMPLEMENTARY_CNAES.contains(company.getCnaeFiscal()), decision.authorized(),
                cvmStatusLabel(decision.status()), decision.category(), decision.checkedAt(), decision.reason());
    }

    public BrokerAddressPreviewDTO consultCep(String input) {
        String cep = Identifiers.cepFromBody(input);
        ViaCepResponse address = cepClient.buscarEnderecoPorCep(cep);
        validateAddress(address);
        return new BrokerAddressPreviewDTO(cep, required(address.getLogradouro(), "logradouro"),
                required(address.getBairro(), "bairro"), required(address.getLocalidade(), "cidade"),
                required(address.getUf(), "UF"));
    }

    public CorretoraDTO insert(CorretoraDTO dto) {
        if (dto.getId() != null) throw new BusinessException("VALIDATION_ERROR", "O ID não deve ser informado no cadastro.");
        String cnpj = Identifiers.cnpjFromBody(dto.getCnpj());
        String cep = Identifiers.cepFromBody(dto.getCep());
        rejectDuplicate(cnpj);
        BrasilApiResponse company = cnpjClient.buscarDadosPorCnpj(cnpj);
        validateCompany(company);
        Decision decision = regulatoryVerification.verify(cnpj);
        if (!decision.authorized()) throw regulatoryBlock(decision);
        ViaCepResponse address = cepClient.buscarEnderecoPorCep(cep);
        validateAddress(address);

        dto.setId(null);
        dto.setCnpj(cnpj);
        dto.setCep(cep);
        dto.setRazaoSocial(company.getRazaoSocial());
        dto.setNomeFantasia(fantasyName(company));
        dto.setSituacaoCadastral(company.getDescricaoSituacaoCadastral());
        dto.setValidadaNaCvm(true);
        dto.setLogradouro(required(address.getLogradouro(), "logradouro"));
        dto.setBairro(required(address.getBairro(), "bairro"));
        dto.setCidade(required(address.getLocalidade(), "cidade"));
        dto.setUf(required(address.getUf(), "UF"));
        Corretora entity = CorretoraMapper.toEntity(dto);
        entity.setDataCadastro(LocalDateTime.now(clock));
        applyDecision(entity, decision);
        return dto(repository.save(entity));
    }

    public List<CorretoraDTO> findAll() { return repository.findAll().stream().map(this::dto).toList(); }
    public CorretoraDTO findById(Long id) { return dto(repository.findById(id).orElseThrow(() -> new NotFoundException("Corretora não encontrada."))); }
    public CorretoraDTO findByCnpj(String cnpj) {
        String canonical = Identifiers.cnpjFromPath(cnpj);
        return dto(repository.findByCnpj(canonical).orElseThrow(() -> new NotFoundException("Corretora não encontrada.")));
    }

    static void applyDecision(Corretora entity, Decision decision) {
        entity.setRegulatoryStatus(decision.status());
        entity.setRegulatoryCategory(decision.category());
        entity.setRegulatorySource(decision.source());
        entity.setRegulatoryEvidenceId(decision.evidenceId());
        entity.setRegulatoryReferenceAt(decision.referenceAt());
        entity.setRegulatoryCheckedAt(decision.checkedAt());
        entity.setRegulatoryReason(decision.reason());
        entity.setValidadaNaCvm(decision.authorized());
    }

    private void rejectDuplicate(String cnpj) {
        if (repository.findByCnpj(cnpj).isPresent()) throw new ConflictException("DUPLICATE_CNPJ", "CNPJ já cadastrado.");
    }

    private static void validateCompany(BrasilApiResponse company) {
        if (company == null || blank(company.getRazaoSocial()) || blank(company.getDescricaoSituacaoCadastral())) {
            throw new UpstreamInvalidResponseException("A consulta do CNPJ retornou dados empresariais inconclusivos.");
        }
        if (!"ATIVA".equalsIgnoreCase(company.getDescricaoSituacaoCadastral().trim())) {
            throw new BusinessException("BUSINESS_INACTIVE", "A empresa não está em situação cadastral ativa.", "cnpj");
        }
    }

    private static void validateAddress(ViaCepResponse address) {
        if (address == null) throw new UpstreamInvalidResponseException("A consulta do CEP retornou resposta vazia.");
        if (Boolean.TRUE.equals(address.getErro())) throw new UpstreamNotFoundException("CEP não encontrado.");
        required(address.getLogradouro(), "logradouro"); required(address.getBairro(), "bairro");
        required(address.getLocalidade(), "cidade"); required(address.getUf(), "UF");
    }

    private static String required(String value, String field) {
        if (blank(value)) throw new UpstreamInvalidResponseException("A consulta do CEP não informou " + field + ".");
        return value.trim();
    }

    private static BusinessException regulatoryBlock(Decision decision) {
        return switch (decision.status()) {
            case NOT_FOUND -> new BusinessException("CVM_NOT_FOUND", "CNPJ não localizado no cadastro oficial da CVM.", "cnpj");
            case INACTIVE -> new BusinessException("CVM_INACTIVE", "O registro na CVM não está em funcionamento normal.", "cnpj");
            case INCOMPATIBLE -> new BusinessException("CVM_CATEGORY_INCOMPATIBLE", "A categoria encontrada na CVM não permite este cadastro.", "cnpj");
            default -> new BusinessException("CVM_INCONCLUSIVE", "Não foi possível comprovar a autorização na CVM.", "cnpj");
        };
    }

    private static String cvmStatusLabel(RegulatoryStatus status) {
        return switch (status) {
            case VERIFIED -> "Em funcionamento normal";
            case NOT_FOUND -> "Não localizada";
            case INACTIVE -> "Fora de funcionamento normal";
            case INCOMPATIBLE -> "Categoria não compatível";
            default -> "Consulta inconclusiva";
        };
    }

    private static String normalizedSuggestion(String cep) {
        if (cep == null) return null;
        String digits = cep.replaceAll("\\D", "");
        return digits.length() == 8 ? digits : null;
    }

    private static String fantasyName(BrasilApiResponse company) {
        return blank(company.getNomeFantasia()) ? company.getRazaoSocial() : company.getNomeFantasia().trim();
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private CorretoraDTO dto(Corretora entity) { return CorretoraMapper.toDTO(entity, clock.instant(), regulatoryFreshness); }
}
