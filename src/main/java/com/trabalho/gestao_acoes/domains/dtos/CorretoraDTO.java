package com.trabalho.gestao_acoes.domains.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.trabalho.gestao_acoes.domains.dtos.broker.BusinessRegistrationDTO;
import com.trabalho.gestao_acoes.domains.dtos.broker.RegulatoryEvidenceDTO;

public class CorretoraDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Null(message = "O ID não deve ser informado no cadastro")
    private Long id;

    @NotBlank(message = "O CNPJ é obrigatório")
    private String cnpj;

    private String razaoSocial;
    private String nomeFantasia;
    private String email;
    private String telefone;

    @NotBlank(message = "O CEP é obrigatório")
    @Size(min = 8, max = 9, message = "O CEP deve ter 8 ou 9 caracteres")
    private String cep;

    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private String situacaoCadastral;
    private Boolean validadaNaCvm;
    private LocalDateTime dataCadastro;
    private BusinessRegistrationDTO businessRegistration;
    private RegulatoryEvidenceDTO regulatoryEvidence;

    public CorretoraDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public String getNomeFantasia() { return nomeFantasia; }
    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public String getSituacaoCadastral() { return situacaoCadastral; }
    public void setSituacaoCadastral(String situacaoCadastral) { this.situacaoCadastral = situacaoCadastral; }

    public Boolean getValidadaNaCvm() { return validadaNaCvm; }
    public void setValidadaNaCvm(Boolean validadaNaCvm) { this.validadaNaCvm = validadaNaCvm; }

    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }
    public BusinessRegistrationDTO getBusinessRegistration() { return businessRegistration; }
    public void setBusinessRegistration(BusinessRegistrationDTO value) { this.businessRegistration = value; }
    public RegulatoryEvidenceDTO getRegulatoryEvidence() { return regulatoryEvidence; }
    public void setRegulatoryEvidence(RegulatoryEvidenceDTO value) { this.regulatoryEvidence = value; }
}
