package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.dtos.CorretoraDTO;
import com.trabalho.gestao_acoes.integrations.brasilapi.BrasilApiResponse;
import com.trabalho.gestao_acoes.integrations.viacep.ViaCepResponse;
import com.trabalho.gestao_acoes.mappers.CorretoraMapper;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.ConflictException;
import com.trabalho.gestao_acoes.services.exceptions.NotFoundException;
import com.trabalho.gestao_acoes.services.exceptions.UpstreamInvalidResponseException;
import com.trabalho.gestao_acoes.services.exceptions.UpstreamNotFoundException;
import com.trabalho.gestao_acoes.services.ports.CepClientPort;
import com.trabalho.gestao_acoes.services.ports.CnpjClientPort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class CorretoraService {
    private static final Set<Integer> CNAES_VALIDOS = Set.of(6612601, 6612602, 6431900);
    private final CorretoraRepository repository;
    private final CnpjClientPort cnpjClient;
    private final CepClientPort cepClient;

    public CorretoraService(CorretoraRepository repository, CnpjClientPort cnpjClient, CepClientPort cepClient) {
        this.repository = repository;
        this.cnpjClient = cnpjClient;
        this.cepClient = cepClient;
    }

    public CorretoraDTO insert(CorretoraDTO dto) {
        if (dto.getId() != null) throw new BusinessException("VALIDATION_ERROR", "O ID não deve ser informado no cadastro.");
        String cnpj = Identifiers.cnpjFromBody(dto.getCnpj());
        if (repository.findByCnpj(cnpj).isPresent()) throw new ConflictException("DUPLICATE_CNPJ", "CNPJ já cadastrado.");

        BrasilApiResponse company = cnpjClient.buscarDadosPorCnpj(cnpj);
        if (company == null || company.getRazaoSocial() == null || company.getRazaoSocial().isBlank()) {
            throw new UpstreamInvalidResponseException("O serviço de CNPJ retornou dados incompletos.");
        }
        if (company.getCnaeFiscal() == null || !CNAES_VALIDOS.contains(company.getCnaeFiscal())) {
            throw new BusinessException("VALIDATION_ERROR", "O CNPJ não pertence a uma instituição financeira aceita.");
        }

        ViaCepResponse address = cepClient.buscarEnderecoPorCep(dto.getCep());
        if (address == null) throw new UpstreamInvalidResponseException("O serviço de CEP retornou resposta vazia.");
        if (Boolean.TRUE.equals(address.getErro())) throw new UpstreamNotFoundException("CEP não encontrado.");

        dto.setId(null);
        dto.setCnpj(cnpj);
        dto.setRazaoSocial(company.getRazaoSocial());
        dto.setNomeFantasia(company.getNomeFantasia() == null || company.getNomeFantasia().isBlank() ? company.getRazaoSocial() : company.getNomeFantasia());
        dto.setSituacaoCadastral(company.getDescricaoSituacaoCadastral());
        dto.setValidadaNaCvm(true);
        dto.setLogradouro(address.getLogradouro());
        dto.setBairro(address.getBairro());
        dto.setCidade(address.getLocalidade());
        dto.setUf(address.getUf());
        Corretora entity = CorretoraMapper.toEntity(dto);
        entity.setDataCadastro(LocalDateTime.now());
        return CorretoraMapper.toDTO(repository.save(entity));
    }

    public List<CorretoraDTO> findAll() { return repository.findAll().stream().map(CorretoraMapper::toDTO).toList(); }

    public CorretoraDTO findById(Long id) {
        return CorretoraMapper.toDTO(repository.findById(id).orElseThrow(() -> new NotFoundException("Corretora não encontrada.")));
    }

    public CorretoraDTO findByCnpj(String cnpj) {
        String canonical = Identifiers.cnpjFromPath(cnpj);
        return CorretoraMapper.toDTO(repository.findByCnpj(canonical).orElseThrow(() -> new NotFoundException("Corretora não encontrada.")));
    }
}
