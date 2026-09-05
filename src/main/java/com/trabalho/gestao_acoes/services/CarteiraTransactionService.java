package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Acao;
import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.PosicaoCarteira;
import com.trabalho.gestao_acoes.domains.Transacao;
import com.trabalho.gestao_acoes.domains.enums.TipoTransacao;
import com.trabalho.gestao_acoes.repositories.AcaoRepository;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.repositories.PosicaoCarteiraRepository;
import com.trabalho.gestao_acoes.repositories.TransacaoRepository;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CarteiraTransactionService {
    private final TransacaoRepository transactions;
    private final PosicaoCarteiraRepository positions;
    private final AcaoRepository assets;
    private final CorretoraRepository brokers;

    public CarteiraTransactionService(TransacaoRepository transactions, PosicaoCarteiraRepository positions,
                                      AcaoRepository assets, CorretoraRepository brokers) {
        this.transactions = transactions;
        this.positions = positions;
        this.assets = assets;
        this.brokers = brokers;
    }

    @Transactional
    public void comprar(Long assetId, Long brokerId, int quantity, BigDecimal price) {
        validateMutation(assetId, brokerId, quantity);
        price = MoneyPolicy.quote(price);
        Corretora broker = lockBroker(brokerId);
        Acao asset = assets.findById(assetId).orElseThrow(() -> new NotFoundException("Ação não encontrada."));
        PosicaoCarteira position = positions.findByAcaoIdAndCorretoraId(assetId, brokerId)
                .orElse(new PosicaoCarteira(null, 0, BigDecimal.ZERO.setScale(MoneyPolicy.PRICE_SCALE), asset, broker));
        int total;
        try {
            total = Math.addExact(position.getQuantidadeTotal(), quantity);
        } catch (ArithmeticException ex) {
            throw new BusinessException("NUMERIC_LIMIT_EXCEEDED", "A quantidade total excede o limite suportado.");
        }
        position.setPrecoMedio(MoneyPolicy.average(position.getPrecoMedio(), position.getQuantidadeTotal(), price, quantity, total));
        position.setQuantidadeTotal(total);
        transactions.save(new Transacao(null, TipoTransacao.COMPRA, quantity, price, LocalDateTime.now(), asset, broker));
        positions.save(position);
    }

    @Transactional
    public void vender(Long assetId, Long brokerId, int quantity, BigDecimal price) {
        validateMutation(assetId, brokerId, quantity);
        price = MoneyPolicy.quote(price);
        Corretora broker = lockBroker(brokerId);
        Acao asset = assets.findById(assetId).orElseThrow(() -> new NotFoundException("Ação não encontrada."));
        PosicaoCarteira position = positions.findByAcaoIdAndCorretoraId(assetId, brokerId)
                .orElseThrow(() -> new BusinessException("INSUFFICIENT_POSITION", "Você não possui este ativo nesta corretora."));
        if (position.getQuantidadeTotal() < quantity) {
            throw new BusinessException("INSUFFICIENT_POSITION", "Quantidade disponível insuficiente para a venda.");
        }
        int remaining = position.getQuantidadeTotal() - quantity;
        transactions.save(new Transacao(null, TipoTransacao.VENDA, quantity, price, LocalDateTime.now(), asset, broker));
        if (remaining == 0) positions.delete(position);
        else {
            position.setQuantidadeTotal(remaining);
            positions.save(position);
        }
    }

    private Corretora lockBroker(Long id) {
        return brokers.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Corretora não encontrada."));
    }

    private void validateMutation(Long assetId, Long brokerId, int quantity) {
        if (assetId == null || assetId <= 0 || brokerId == null || brokerId <= 0 || quantity <= 0) {
            throw new BusinessException("VALIDATION_ERROR", "Ativo, corretora e quantidade devem ser positivos.");
        }
    }
}
