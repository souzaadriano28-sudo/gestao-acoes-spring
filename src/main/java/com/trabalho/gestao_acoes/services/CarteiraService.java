package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Acao;
import com.trabalho.gestao_acoes.domains.PosicaoCarteira;
import com.trabalho.gestao_acoes.domains.dtos.PosicaoDTO;
import com.trabalho.gestao_acoes.repositories.AcaoRepository;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.repositories.PosicaoCarteiraRepository;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.NotFoundException;
import com.trabalho.gestao_acoes.services.ports.CotacaoBolsa;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CarteiraService {
    private static final BigDecimal FIXED_EXCHANGE_RATE = new BigDecimal("5.30");
    private final PosicaoCarteiraRepository positions;
    private final AcaoRepository assets;
    private final CorretoraRepository brokers;
    private final CotacaoService quotes;
    private final CarteiraTransactionService transactionService;

    public CarteiraService(PosicaoCarteiraRepository positions, AcaoRepository assets,
                           CorretoraRepository brokers, CotacaoService quotes,
                           CarteiraTransactionService transactionService) {
        this.positions = positions;
        this.assets = assets;
        this.brokers = brokers;
        this.quotes = quotes;
        this.transactionService = transactionService;
    }

    public void comprar(String tickerValue, String marketValue, Integer quantity, Long brokerId) {
        OperationData data = validate(tickerValue, marketValue, quantity, brokerId);
        CotacaoBolsa quote = quotes.buscar(data.asset().getTicker(), data.asset().getMercado());
        transactionService.comprar(data.asset().getId(), brokerId, quantity, quote.getPrecoAtual());
    }

    public void vender(String tickerValue, String marketValue, Integer quantity, Long brokerId) {
        OperationData data = validate(tickerValue, marketValue, quantity, brokerId);
        CotacaoBolsa quote = quotes.buscar(data.asset().getTicker(), data.asset().getMercado());
        transactionService.vender(data.asset().getId(), brokerId, quantity, quote.getPrecoAtual());
    }

    public BigDecimal calcularSaldoTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (PosicaoCarteira position : positions.findAll()) {
            CotacaoBolsa quote = quotes.buscar(position.getAcao().getTicker(), position.getAcao().getMercado());
            BigDecimal value = quote.getPrecoAtual().multiply(BigDecimal.valueOf(position.getQuantidadeTotal()));
            if ("USD".equals(position.getAcao().getMoeda())) value = value.multiply(FIXED_EXCHANGE_RATE);
            total = total.add(value);
        }
        return MoneyPolicy.total(total);
    }

    public List<PosicaoDTO> listarPosicoes() {
        return positions.findAll().stream().map(p -> new PosicaoDTO(p.getAcao().getTicker(),
                p.getCorretora().getRazaoSocial(), p.getQuantidadeTotal(), p.getPrecoMedio(), p.getAcao().getMoeda())).toList();
    }

    private OperationData validate(String tickerValue, String marketValue, Integer quantity, Long brokerId) {
        String ticker = Identifiers.ticker(tickerValue);
        String market = Identifiers.mercado(marketValue);
        Identifiers.validateTickerForMarket(ticker, market);
        if (quantity == null || quantity <= 0 || brokerId == null || brokerId <= 0) {
            throw new BusinessException("VALIDATION_ERROR", "Quantidade e corretora devem ser positivas.");
        }
        Acao asset = assets.findByTicker(ticker).orElseThrow(() -> new NotFoundException("Ação não encontrada."));
        if (!brokers.existsById(brokerId)) throw new NotFoundException("Corretora não encontrada.");
        if (!asset.getMercado().equals(market)) throw new BusinessException("MARKET_MISMATCH", "O mercado informado não corresponde ao ativo.");
        return new OperationData(asset);
    }

    private record OperationData(Acao asset) {}
}
