package com.trabalho.gestao_acoes.integrations.brasilapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BrasilApiResponse {

    @JsonProperty("razao_social")
    private String razaoSocial;

    @JsonProperty("nome_fantasia")
    private String nomeFantasia;

    @JsonProperty("descricao_situacao_cadastral")
    private String descricaoSituacaoCadastral;

    @JsonProperty("cnae_fiscal")
    private Integer cnaeFiscal; // Usaremos isso para a regra da CVM!

    public BrasilApiResponse() {
    }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public String getNomeFantasia() { return nomeFantasia; }
    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia; }

    public String getDescricaoSituacaoCadastral() { return descricaoSituacaoCadastral; }
    public void setDescricaoSituacaoCadastral(String descricaoSituacaoCadastral) { this.descricaoSituacaoCadastral = descricaoSituacaoCadastral; }

    public Integer getCnaeFiscal() { return cnaeFiscal; }
    public void setCnaeFiscal(Integer cnaeFiscal) { this.cnaeFiscal = cnaeFiscal; }
}