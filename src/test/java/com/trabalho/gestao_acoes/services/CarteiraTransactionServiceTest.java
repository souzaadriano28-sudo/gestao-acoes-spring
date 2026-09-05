package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.repositories.AcaoRepository;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.repositories.PosicaoCarteiraRepository;
import com.trabalho.gestao_acoes.repositories.TransacaoRepository;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.InvalidQuoteException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class CarteiraTransactionServiceTest {
    @Test
    void rejectsInvalidDirectCallsBeforeReadingOrWritingPersistence() {
        TransacaoRepository transactions = mock(TransacaoRepository.class);
        PosicaoCarteiraRepository positions = mock(PosicaoCarteiraRepository.class);
        AcaoRepository assets = mock(AcaoRepository.class);
        CorretoraRepository brokers = mock(CorretoraRepository.class);
        CarteiraTransactionService service = new CarteiraTransactionService(transactions, positions, assets, brokers);

        assertThatThrownBy(() -> service.comprar(1L, 1L, 0, BigDecimal.ONE)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.vender(1L, 1L, -1, BigDecimal.ONE)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.comprar(null, 1L, 1, BigDecimal.ONE)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.comprar(1L, 1L, 1, BigDecimal.ZERO)).isInstanceOf(InvalidQuoteException.class);
        assertThatThrownBy(() -> service.vender(1L, 1L, 1, null)).isInstanceOf(InvalidQuoteException.class);
        verifyNoInteractions(transactions, positions, assets, brokers);
    }
}
