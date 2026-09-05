package com.trabalho.gestao_acoes.domains;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "posicao_carteira", uniqueConstraints = @UniqueConstraint(name = "uk_posicao_acao_corretora", columnNames = {"acao_id", "corretora_id"}))
@Check(constraints = "quantidade_total > 0 and preco_medio > 0")
public class PosicaoCarteira implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantidadeTotal;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal precoMedio;

    @ManyToOne
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @ManyToOne
    @JoinColumn(name = "corretora_id", nullable = false)
    private Corretora corretora;

    public PosicaoCarteira() {
    }

    public PosicaoCarteira(Long id, Integer quantidadeTotal, BigDecimal precoMedio, Acao acao, Corretora corretora) {
        this.id = id;
        this.quantidadeTotal = quantidadeTotal;
        this.precoMedio = precoMedio;
        this.acao = acao;
        this.corretora = corretora;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getQuantidadeTotal() { return quantidadeTotal; }
    public void setQuantidadeTotal(Integer quantidadeTotal) { this.quantidadeTotal = quantidadeTotal; }

    public BigDecimal getPrecoMedio() { return precoMedio; }
    public void setPrecoMedio(BigDecimal precoMedio) { this.precoMedio = precoMedio; }

    public Acao getAcao() { return acao; }
    public void setAcao(Acao acao) { this.acao = acao; }

    public Corretora getCorretora() { return corretora; }
    public void setCorretora(Corretora corretora) { this.corretora = corretora; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PosicaoCarteira that = (PosicaoCarteira) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
