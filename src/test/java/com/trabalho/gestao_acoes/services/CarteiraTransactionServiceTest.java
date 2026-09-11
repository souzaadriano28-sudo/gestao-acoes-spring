package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.repositories.AcaoRepository;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.repositories.PosicaoCarteiraRepository;
import com.trabalho.gestao_acoes.repositories.TransacaoRepository;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.InvalidQuoteException;
import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CarteiraTransactionServiceTest {
    @Test
    void rejectsInvalidDirectCallsBeforeReadingOrWritingPersistence() {
        TransacaoRepository transactions = mock(TransacaoRepository.class);
        PosicaoCarteiraRepository positions = mock(PosicaoCarteiraRepository.class);
        AcaoRepository assets = mock(AcaoRepository.class);
        CorretoraRepository brokers = mock(CorretoraRepository.class);
        com.trabalho.gestao_acoes.repositories.PortfolioRepository portfolios = mock(com.trabalho.gestao_acoes.repositories.PortfolioRepository.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        CarteiraTransactionService service = new CarteiraTransactionService(transactions, positions, assets, brokers, portfolios, securityUtils);

        assertThatThrownBy(() -> service.comprar(1L, 1L, 0, BigDecimal.ONE)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.vender(1L, 1L, -1, BigDecimal.ONE)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.comprar(null, 1L, 1, BigDecimal.ONE)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.comprar(1L, 1L, 1, BigDecimal.ZERO)).isInstanceOf(InvalidQuoteException.class);
        assertThatThrownBy(() -> service.vender(1L, 1L, 1, null)).isInstanceOf(InvalidQuoteException.class);
        verifyNoInteractions(transactions, positions, assets, brokers);
    }

    @Test
    void blocksNewOperationsWhenHistoricalBrokerLosesAuthorization() {
        TransacaoRepository transactions = mock(TransacaoRepository.class);
        PosicaoCarteiraRepository positions = mock(PosicaoCarteiraRepository.class);
        AcaoRepository assets = mock(AcaoRepository.class);
        CorretoraRepository brokers = mock(CorretoraRepository.class);
        com.trabalho.gestao_acoes.repositories.PortfolioRepository portfolios = mock(com.trabalho.gestao_acoes.repositories.PortfolioRepository.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.currentOwnerId()).thenReturn(2L);
        com.trabalho.gestao_acoes.domains.Portfolio port = new com.trabalho.gestao_acoes.domains.Portfolio(); port.setId(3L);
        when(portfolios.findFirstByOwnerIdOrderByIdAsc(2L)).thenReturn(java.util.Optional.of(port));

        Corretora historical = new Corretora(); historical.setId(1L); historical.setRegulatoryStatus(RegulatoryStatus.INACTIVE);
        when(brokers.findByIdAndOwnerIdForUpdate(1L, 2L)).thenReturn(java.util.Optional.of(historical));
        CarteiraTransactionService service = new CarteiraTransactionService(transactions, positions, assets, brokers, portfolios, securityUtils);


        assertThatThrownBy(() -> service.comprar(1L, 1L, 1, BigDecimal.ONE))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("BROKER_UNAVAILABLE");
        verifyNoInteractions(transactions, positions, assets);
    }
}
