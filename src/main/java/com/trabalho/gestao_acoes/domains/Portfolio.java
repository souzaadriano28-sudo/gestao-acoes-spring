package com.trabalho.gestao_acoes.domains;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "portfolio")
public class Portfolio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Portfolio() {}
    public void setId(Long id) { this.id = id; }


    public Portfolio(String name, UserAccount owner, Instant now) {
        this.name = name;
        this.owner = owner;
        this.createdAt = now;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public UserAccount getOwner() { return owner; }
    public Instant getCreatedAt() { return createdAt; }
}
