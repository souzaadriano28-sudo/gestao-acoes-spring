package com.trabalho.gestao_acoes.domains;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.Instant;
import com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus;
import java.util.Objects;

@Entity
@Table(name = "corretora")
public class Corretora implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(nullable = false)
    private String razaoSocial;

    private String nomeFantasia;

    private String email;

    private String telefone;

    @Column(nullable = false, length = 9)
    private String cep;

    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;

    @Column(length = 2)
    private String uf;

    private String situacaoCadastral;

    private Boolean validadaNaCvm;

    private LocalDateTime dataCadastro;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RegulatoryStatus regulatoryStatus = RegulatoryStatus.NOT_CHECKED;
    @Column(length = 120) private String regulatoryCategory;
    @Column(length = 160) private String regulatorySource;
    @Column(length = 80) private String regulatoryEvidenceId;
    private Instant regulatoryReferenceAt;
    private Instant regulatoryCheckedAt;
    @Column(length = 120) private String regulatoryReason;

    public Corretora() {
    }

    public Corretora(Long id, String cnpj, String razaoSocial, String nomeFantasia, String email, String telefone, String cep, String logradouro, String numero, String complemento, String bairro, String cidade, String uf, String situacaoCadastral, Boolean validadaNaCvm, LocalDateTime dataCadastro) {
        this.id = id;
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.nomeFantasia = nomeFantasia;
        this.email = email;
        this.telefone = telefone;
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
        this.situacaoCadastral = situacaoCadastral;
        this.validadaNaCvm = validadaNaCvm;
        this.dataCadastro = dataCadastro;
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
    public RegulatoryStatus getRegulatoryStatus() { return regulatoryStatus; }
    public void setRegulatoryStatus(RegulatoryStatus value) { this.regulatoryStatus = value; }
    public String getRegulatoryCategory() { return regulatoryCategory; }
    public void setRegulatoryCategory(String value) { this.regulatoryCategory = value; }
    public String getRegulatorySource() { return regulatorySource; }
    public void setRegulatorySource(String value) { this.regulatorySource = value; }
    public String getRegulatoryEvidenceId() { return regulatoryEvidenceId; }
    public void setRegulatoryEvidenceId(String value) { this.regulatoryEvidenceId = value; }
    public Instant getRegulatoryReferenceAt() { return regulatoryReferenceAt; }
    public void setRegulatoryReferenceAt(Instant value) { this.regulatoryReferenceAt = value; }
    public Instant getRegulatoryCheckedAt() { return regulatoryCheckedAt; }
    public void setRegulatoryCheckedAt(Instant value) { this.regulatoryCheckedAt = value; }
    public String getRegulatoryReason() { return regulatoryReason; }
    public void setRegulatoryReason(String value) { this.regulatoryReason = value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Corretora corretora = (Corretora) o;
        return Objects.equals(id, corretora.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
