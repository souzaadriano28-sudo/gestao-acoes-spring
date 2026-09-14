package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.*;
import com.trabalho.gestao_acoes.repositories.*;
import com.trabalho.gestao_acoes.domains.dtos.*;
import com.trabalho.gestao_acoes.services.CotacaoService;
import com.trabalho.gestao_acoes.services.ports.CotacaoBolsa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class UserIsolationIntegrationTest {

    @MockitoBean private CotacaoService cotacaoService;
    @Autowired private CorretoraService corretoraService;
    @Autowired private AcaoService acaoService;
    @Autowired private CarteiraService carteiraService;
    @Autowired private CorretoraRepository corretoras;
    @Autowired private AcaoRepository acoes;
    @Autowired private TransacaoRepository transacoes;
    @Autowired private PosicaoCarteiraRepository posicoes;
    @Autowired private PortfolioRepository portfolios;
    @Autowired private UserAccountRepository users;

    private UserAccount alice;
    private UserAccount bob;

    @BeforeEach
    void setUp() {
        transacoes.deleteAll();
        posicoes.deleteAll();
        acoes.deleteAll();
        corretoras.deleteAll();
        portfolios.deleteAll();
        users.deleteAll();

        alice = users.save(new UserAccount("alice", "alice@test.com", "hash", Instant.now()));
        bob = users.save(new UserAccount("bob", "bob@test.com", "hash", Instant.now()));
        portfolios.save(new Portfolio("alice", alice, Instant.now()));
        portfolios.save(new Portfolio("bob", bob, Instant.now()));
    }

    @Test
    @WithMockUser(username = "alice")
    void aliceCreatesResources() {
        Corretora corretora = new Corretora(null, "11111111111111", "Alice Broker", "Alice Fantasia", null, null, "12345678", "Rua A", null, null, null, null, null, null, null, LocalDateTime.now());
        corretora.setOwner(alice);
        corretora.setRegulatoryStatus(com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus.VERIFIED);
        corretora = corretoras.save(corretora);

        Acao acao = new Acao(null, "ALIC4", "Alice Corp", "BRASIL", "BRL", new BigDecimal("10.00"), LocalDateTime.now());
        acao.setOwner(alice);
        acao = acoes.save(acao);

        when(cotacaoService.buscar(any(), any())).thenReturn(new CotacaoBolsa(new BigDecimal("15.00"), "BRL", "Mock", "Mock", Instant.now(), Instant.now(), "Mock"));

        carteiraService.comprar("ALIC4", "BRASIL", 100, corretora.getId());

        assertThat(corretoraService.findAll()).hasSize(1);
        assertThat(acaoService.findAll()).hasSize(1);
        assertThat(carteiraService.listarPosicoes()).hasSize(1);
    }

    @Test
    @WithMockUser(username = "bob")
    void bobCannotSeeAliceResources() {
        // Alice setup (using repository directly so it ignores security context of Bob)
        Corretora aliceCorretora = new Corretora(null, "11111111111111", "Alice Broker", "Alice Fantasia", null, null, "12345678", "Rua A", null, null, null, null, null, null, null, LocalDateTime.now());
        aliceCorretora.setOwner(alice);
        corretoras.save(aliceCorretora);

        Acao aliceAcao = new Acao(null, "ALIC4", "Alice Corp", "BRASIL", "BRL", new BigDecimal("10.00"), LocalDateTime.now());
        aliceAcao.setOwner(alice);
        acoes.save(aliceAcao);

        // Bob should not see them
        assertThat(corretoraService.findAll()).isEmpty();
        assertThat(acaoService.findAll()).isEmpty();
        assertThat(carteiraService.listarPosicoes()).isEmpty();
        assertThat(carteiraService.calcularSaldoTotal()).isEqualByComparingTo("0.00");

        // Bob cannot find by ID
        Optional<Corretora> fetched = corretoras.findByIdAndOwnerId(aliceCorretora.getId(), bob.getId());
        assertThat(fetched).isEmpty();
    }

    @Test
    void brokerCnpjIsUniquePerOwner() {
        String cnpj = "02332886000104";
        corretoras.saveAndFlush(broker(cnpj, alice));
        corretoras.saveAndFlush(broker(cnpj, bob));

        assertThatThrownBy(() -> corretoras.saveAndFlush(broker(cnpj, alice)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private Corretora broker(String cnpj, UserAccount owner) {
        Corretora broker = new Corretora(null, cnpj, "Broker Test", "Broker Test", null, null,
                "12345678", "Rua A", null, null, null, null, null, null, null, LocalDateTime.now());
        broker.setOwner(owner);
        return broker;
    }
}
