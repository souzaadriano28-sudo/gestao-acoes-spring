package com.trabalho.gestao_acoes.domains;

import com.trabalho.gestao_acoes.domains.enums.TipoTransacao;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "transacao")
@Check(constraints = "quantidade > 0 and preco_unitario > 0")
public class Transacao implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTransacao tipo;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @ManyToOne
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @ManyToOne
    @JoinColumn(name = "corretora_id", nullable = false)
    private Corretora corretora;

    public Transacao() {
    }

    public Transacao(Long id, TipoTransacao tipo, Integer quantidade, BigDecimal precoUnitario, LocalDateTime dataHora, Acao acao, Corretora corretora) {
        this.id = id;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.dataHora = dataHora;
        this.acao = acao;
        this.corretora = corretora;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoTransacao getTipo() { return tipo; }
    public void setTipo(TipoTransacao tipo) { this.tipo = tipo; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public Acao getAcao() { return acao; }
    public void setAcao(Acao acao) { this.acao = acao; }

    public Corretora getCorretora() { return corretora; }
    public void setCorretora(Corretora corretora) { this.corretora = corretora; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transacao transacao = (Transacao) o;
        return Objects.equals(id, transacao.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
