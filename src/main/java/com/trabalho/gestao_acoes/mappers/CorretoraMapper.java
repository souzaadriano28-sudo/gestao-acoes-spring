package com.trabalho.gestao_acoes.mappers;

import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.dtos.CorretoraDTO;
import com.trabalho.gestao_acoes.domains.dtos.broker.BusinessRegistrationDTO;
import com.trabalho.gestao_acoes.domains.dtos.broker.RegulatoryEvidenceDTO;

public class CorretoraMapper {

    public static CorretoraDTO toDTO(Corretora entity) {
        return toDTO(entity, java.time.Instant.now(), java.time.Duration.ofDays(2));
    }

    public static CorretoraDTO toDTO(Corretora entity, java.time.Instant now, java.time.Duration freshness) {
        if (entity == null) {
            return null;
        }
        CorretoraDTO dto = new CorretoraDTO();
        dto.setId(entity.getId());
        dto.setCnpj(entity.getCnpj());
        dto.setRazaoSocial(entity.getRazaoSocial());
        dto.setNomeFantasia(entity.getNomeFantasia());
        dto.setEmail(entity.getEmail());
        dto.setTelefone(entity.getTelefone());
        dto.setCep(entity.getCep());
        dto.setLogradouro(entity.getLogradouro());
        dto.setNumero(entity.getNumero());
        dto.setComplemento(entity.getComplemento());
        dto.setBairro(entity.getBairro());
        dto.setCidade(entity.getCidade());
        dto.setUf(entity.getUf());
        dto.setSituacaoCadastral(entity.getSituacaoCadastral());
        dto.setValidadaNaCvm(entity.getValidadaNaCvm());
        dto.setDataCadastro(entity.getDataCadastro());
        dto.setBusinessRegistration(new BusinessRegistrationDTO("BRASIL_API",
                entity.getSituacaoCadastral(), entity.getSituacaoCadastral() == null ? "BUSINESS_STATUS_UNAVAILABLE" : null));
        var status = entity.getRegulatoryStatus();
        String reason = entity.getRegulatoryReason();
        if ((status == com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus.VERIFIED
                || status == com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus.STALE)
                && (entity.getRegulatoryCategory() == null || entity.getRegulatorySource() == null
                || entity.getRegulatoryEvidenceId() == null || entity.getRegulatoryReferenceAt() == null
                || entity.getRegulatoryCheckedAt() == null)) {
            status = com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus.UNAVAILABLE;
            reason = "CVM_EVIDENCE_PROVENANCE_INCOMPLETE";
        } else if (status == com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus.VERIFIED && now != null && freshness != null
                && entity.getRegulatoryReferenceAt() != null
                && entity.getRegulatoryReferenceAt().plus(freshness).isBefore(now)) {
            status = com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus.STALE;
            reason = "CVM_REGISTRY_FRESHNESS_EXCEEDED";
        }
        dto.setRegulatoryEvidence(new RegulatoryEvidenceDTO(status, entity.getRegulatoryCategory(),
                entity.getRegulatorySource(), entity.getRegulatoryEvidenceId(), entity.getRegulatoryReferenceAt(),
                entity.getRegulatoryCheckedAt(), reason));
        return dto;
    }

    public static Corretora toEntity(CorretoraDTO dto) {
        if (dto == null) {
            return null;
        }
        Corretora entity = new Corretora();
        entity.setCnpj(dto.getCnpj());
        entity.setRazaoSocial(dto.getRazaoSocial());
        entity.setNomeFantasia(dto.getNomeFantasia());
        entity.setEmail(dto.getEmail());
        entity.setTelefone(dto.getTelefone());
        entity.setCep(dto.getCep());
        entity.setLogradouro(dto.getLogradouro());
        entity.setNumero(dto.getNumero());
        entity.setComplemento(dto.getComplemento());
        entity.setBairro(dto.getBairro());
        entity.setCidade(dto.getCidade());
        entity.setUf(dto.getUf());
        entity.setSituacaoCadastral(dto.getSituacaoCadastral());
        entity.setValidadaNaCvm(dto.getValidadaNaCvm());
        entity.setDataCadastro(dto.getDataCadastro());
        entity.setRegulatoryStatus(com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus.NOT_CHECKED);
        return entity;
    }
}
