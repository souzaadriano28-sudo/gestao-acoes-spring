package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Acao;
import com.trabalho.gestao_acoes.domains.PosicaoCarteira;
import com.trabalho.gestao_acoes.domains.Transacao;
import com.trabalho.gestao_acoes.domains.dtos.portfolio.*;
import com.trabalho.gestao_acoes.domains.enums.Availability;
import com.trabalho.gestao_acoes.domains.enums.TipoTransacao;
import com.trabalho.gestao_acoes.repositories.PosicaoCarteiraRepository;
import com.trabalho.gestao_acoes.repositories.TransacaoRepository;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.ports.ExchangeRate;
import com.trabalho.gestao_acoes.services.ports.ExchangeRatePort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PortfolioReadService {
    private static final String PRESENTATION_CURRENCY = "BRL";
    private static final Sort POSITION_SORT = Sort.by("acao.ticker").ascending().and(Sort.by("corretora.razaoSocial").ascending()).and(Sort.by("id").ascending());
    private static final Sort MOVEMENT_SORT = Sort.by("dataHora").descending().and(Sort.by("id").descending());
    private final PosicaoCarteiraRepository positions;
    private final TransacaoRepository transactions;
    private final ExchangeRatePort exchangeRates;
    private final Clock clock;
    private final Duration quoteFreshness;
    private final Duration exchangeFreshness;
    private final ZoneId legacyZone;

    public PortfolioReadService(PosicaoCarteiraRepository positions, TransacaoRepository transactions,
            ExchangeRatePort exchangeRates, Clock clock,
            @Value("${app.portfolio.quote-freshness:PT30M}") Duration quoteFreshness,
            @Value("${app.portfolio.exchange-freshness:PT36H}") Duration exchangeFreshness,
            @Value("${app.portfolio.legacy-server-zone:America/Sao_Paulo}") String legacyZone) {
        this.positions = positions;
        this.transactions = transactions;
        this.exchangeRates = exchangeRates;
        this.clock = clock;
        this.quoteFreshness = positive(quoteFreshness, "quote freshness");
        this.exchangeFreshness = positive(exchangeFreshness, "exchange freshness");
        this.legacyZone = ZoneId.of(legacyZone);
    }

    public PageDTO<DetailedPositionDTO> detailedPositions(int page, int size, String market, Long brokerId) {
        validatePage(page, size);
        String canonicalMarket = market == null || market.isBlank() ? null : Identifiers.mercado(market);
        if (brokerId != null && brokerId <= 0) throw validation("Corretora deve ser positiva.");
        Page<DetailedPositionDTO> result = positions.findDetailed(canonicalMarket, brokerId, PageRequest.of(page, size, POSITION_SORT)).map(this::position);
        return PageDTO.from(result);
    }

    public PageDTO<MovementDTO> movements(int page, int size, String type, String ticker, Long brokerId,
            OffsetDateTime from, OffsetDateTime to) {
        validatePage(page, size);
        TipoTransacao parsedType = parseType(type);
        String canonicalTicker = ticker == null || ticker.isBlank() ? null : Identifiers.ticker(ticker);
        if (brokerId != null && brokerId <= 0) throw validation("Corretora deve ser positiva.");
        if (from != null && to != null && from.isAfter(to)) throw validation("Período inicial deve anteceder o final.");
        LocalDateTime localFrom = from == null ? null : from.atZoneSameInstant(legacyZone).toLocalDateTime();
        LocalDateTime localTo = to == null ? null : to.atZoneSameInstant(legacyZone).toLocalDateTime();
        return PageDTO.from(transactions.findMovements(parsedType, canonicalTicker, brokerId, localFrom, localTo,
                PageRequest.of(page, size, MOVEMENT_SORT)).map(this::movement));
    }

    public DashboardDTO dashboard() {
        Instant asOf = clock.instant();
        List<PosicaoCarteira> entities = positions.findAllDetailed();
        List<DetailedPositionDTO> details = entities.stream().map(this::position).toList();
        List<MovementDTO> recent = transactions.findMovements(null, null, null, null, null,
                PageRequest.of(0, 5, MOVEMENT_SORT)).map(this::movement).getContent();
        if (entities.isEmpty()) {
            MoneyMetricDTO zero = MoneyMetricDTO.available(new BigDecimal("0.00"), PRESENTATION_CURRENCY);
            return new DashboardDTO(asOf, PRESENTATION_CURRENCY, 0, zero, zero, zero,
                    PercentageMetricDTO.unavailable("RESULT_PERCENTAGE_NOT_APPLICABLE_TO_EMPTY_PORTFOLIO"),
                    details, recent, List.of(), unavailableExchange("NOT_REQUIRED_FOR_EMPTY_PORTFOLIO"));
        }

        boolean needsUsd = entities.stream().anyMatch(p -> "USD".equals(p.getAcao().getMoeda()));
        ExchangeState exchange = exchangeState(needsUsd);
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalMarket = BigDecimal.ZERO;
        boolean costComplete = true;
        boolean marketComplete = true;
        for (int i = 0; i < entities.size(); i++) {
            PosicaoCarteira entity = entities.get(i);
            DetailedPositionDTO detail = details.get(i);
            BigDecimal rate = "BRL".equals(entity.getAcao().getMoeda()) ? BigDecimal.ONE : exchange.usableRate();
            if (rate == null) {
                costComplete = false;
                marketComplete = false;
                continue;
            }
            BigDecimal nativeCost = entity.getPrecoMedio().multiply(BigDecimal.valueOf(entity.getQuantidadeTotal()));
            totalCost = totalCost.add(nativeCost.multiply(rate));
            if (detail.marketValue().availability() != Availability.AVAILABLE) marketComplete = false;
            else {
                BigDecimal nativeMarket = entity.getAcao().getCotacaoAtual().multiply(BigDecimal.valueOf(entity.getQuantidadeTotal()));
                totalMarket = totalMarket.add(nativeMarket.multiply(rate));
            }
        }
        MoneyMetricDTO cost = costComplete ? MoneyMetricDTO.available(MoneyPolicy.total(totalCost), PRESENTATION_CURRENCY)
                : MoneyMetricDTO.unavailable(PRESENTATION_CURRENCY, "EXCHANGE_RATE_UNAVAILABLE");
        MoneyMetricDTO patrimony = marketComplete ? MoneyMetricDTO.available(MoneyPolicy.total(totalMarket), PRESENTATION_CURRENCY)
                : MoneyMetricDTO.unavailable(PRESENTATION_CURRENCY, "QUOTE_OR_EXCHANGE_UNAVAILABLE");
        MoneyMetricDTO result = costComplete && marketComplete
                ? MoneyMetricDTO.available(MoneyPolicy.total(totalMarket.subtract(totalCost)), PRESENTATION_CURRENCY)
                : MoneyMetricDTO.unavailable(PRESENTATION_CURRENCY, "DEPENDENT_TOTAL_UNAVAILABLE");
        PercentageMetricDTO percentage = result.availability() == Availability.AVAILABLE && totalCost.signum() > 0
                ? PercentageMetricDTO.available(totalMarket.subtract(totalCost).multiply(new BigDecimal("100"))
                        .divide(totalCost, 4, RoundingMode.HALF_UP))
                : PercentageMetricDTO.unavailable("DEPENDENT_TOTAL_UNAVAILABLE");
        List<QuoteProvenanceDTO> sources = details.stream().map(DetailedPositionDTO::quoteProvenance).distinct().toList();
        return new DashboardDTO(asOf, PRESENTATION_CURRENCY, entities.size(), patrimony, cost, result,
                percentage, details, recent, sources, exchange.dto());
    }

    private DetailedPositionDTO position(PosicaoCarteira p) {
        String currency = requireCurrency(p.getAcao().getMoeda());
        BigDecimal average = p.getPrecoMedio();
        BigDecimal costValue = MoneyPolicy.total(average.multiply(BigDecimal.valueOf(p.getQuantidadeTotal())));
        QuoteProvenanceDTO provenance = quoteProvenance(p.getAcao());
        MoneyMetricDTO currentQuote;
        MoneyMetricDTO market;
        MoneyMetricDTO result;
        if (provenance.availability() == Availability.UNAVAILABLE) {
            currentQuote = MoneyMetricDTO.unavailable(currency, provenance.reason());
            market = MoneyMetricDTO.unavailable(currency, provenance.reason());
            result = MoneyMetricDTO.unavailable(currency, provenance.reason());
        } else {
            BigDecimal quote = p.getAcao().getCotacaoAtual();
            BigDecimal marketValue = MoneyPolicy.total(quote.multiply(BigDecimal.valueOf(p.getQuantidadeTotal())));
            if (provenance.availability() == Availability.STALE) {
                currentQuote = MoneyMetricDTO.stale(quote, currency, provenance.reason());
                market = MoneyMetricDTO.stale(marketValue, currency, provenance.reason());
                result = MoneyMetricDTO.stale(MoneyPolicy.total(marketValue.subtract(costValue)), currency, provenance.reason());
            } else {
                currentQuote = MoneyMetricDTO.available(quote, currency);
                market = MoneyMetricDTO.available(marketValue, currency);
                result = MoneyMetricDTO.available(MoneyPolicy.total(marketValue.subtract(costValue)), currency);
            }
        }
        return new DetailedPositionDTO(p.getId(), p.getAcao().getId(), p.getAcao().getTicker(), p.getAcao().getMercado(),
                p.getCorretora().getId(), p.getCorretora().getRazaoSocial(), p.getQuantidadeTotal(), currency,
                MoneyMetricDTO.available(average, currency), MoneyMetricDTO.available(costValue, currency),
                currentQuote, market, result, provenance);
    }

    private QuoteProvenanceDTO quoteProvenance(Acao asset) {
        String currency = requireCurrency(asset.getMoeda());
        if (asset.getCotacaoAtual() == null || asset.getCotacaoAtual().signum() <= 0 || asset.getQuoteProvider() == null
                || asset.getQuoteSourceType() == null || asset.getQuoteReferenceAt() == null || asset.getQuoteFetchedAt() == null
                || asset.getQuoteReferenceKind() == null || asset.getQuoteReferenceAt().isAfter(asset.getQuoteFetchedAt())
                || asset.getQuoteFetchedAt().isAfter(clock.instant())) {
            return new QuoteProvenanceDTO(Availability.UNAVAILABLE, asset.getQuoteSourceType(), asset.getQuoteProvider(),
                    asset.getQuoteReferenceAt(), asset.getQuoteFetchedAt(), asset.getQuoteReferenceKind(), currency,
                    "QUOTE_PROVENANCE_UNAVAILABLE");
        }
        boolean stale = asset.getQuoteReferenceAt().plus(quoteFreshness).isBefore(clock.instant());
        return new QuoteProvenanceDTO(stale ? Availability.STALE : Availability.AVAILABLE,
                asset.getQuoteSourceType(), asset.getQuoteProvider(), asset.getQuoteReferenceAt(), asset.getQuoteFetchedAt(),
                asset.getQuoteReferenceKind(), currency, stale ? "QUOTE_FRESHNESS_EXCEEDED" : null);
    }

    private MovementDTO movement(Transacao t) {
        String currency = requireCurrency(t.getAcao().getMoeda());
        QuoteProvenanceDTO absentHistory = new QuoteProvenanceDTO(Availability.UNAVAILABLE, null, null, null, null,
                null, currency, "HISTORICAL_QUOTE_PROVENANCE_NOT_RECORDED");
        return new MovementDTO(t.getId(), t.getTipo().name(), t.getAcao().getId(), t.getAcao().getTicker(),
                t.getAcao().getMercado(), t.getCorretora().getId(), t.getCorretora().getRazaoSocial(), t.getQuantidade(),
                MoneyMetricDTO.available(t.getPrecoUnitario(), currency),
                t.getDataHora().atZone(legacyZone).toOffsetDateTime(), "LEGACY_SERVER_ZONE:" + legacyZone, absentHistory);
    }

    private ExchangeState exchangeState(boolean required) {
        if (!required) return new ExchangeState(null, unavailableExchange("NOT_REQUIRED_FOR_SINGLE_CURRENCY_PORTFOLIO"));
        Optional<ExchangeRate> found;
        try { found = exchangeRates.find("USD", "BRL"); }
        catch (RuntimeException ex) { return new ExchangeState(null, unavailableExchange("EXCHANGE_PROVIDER_FAILURE")); }
        if (found.isEmpty()) return new ExchangeState(null, unavailableExchange("EXCHANGE_RATE_UNAVAILABLE"));
        ExchangeRate rate = found.get();
        boolean invalid = !"USD".equals(rate.baseCurrency()) || !"BRL".equals(rate.quoteCurrency())
                || rate.fetchedAt().isAfter(clock.instant());
        if (invalid) return new ExchangeState(null, unavailableExchange("EXCHANGE_RATE_INVALID"));
        boolean stale = rate.referenceAt().plus(exchangeFreshness).isBefore(clock.instant());
        Availability availability = stale ? Availability.STALE : Availability.AVAILABLE;
        ExchangeProvenanceDTO dto = new ExchangeProvenanceDTO(availability, rate.baseCurrency(), rate.quoteCurrency(),
                rate.rate(), rate.sourceType(), rate.provider(), rate.referenceAt(), rate.fetchedAt(), rate.referenceKind(),
                stale ? "EXCHANGE_FRESHNESS_EXCEEDED" : null);
        return new ExchangeState(stale ? null : rate.rate(), dto);
    }

    private static ExchangeProvenanceDTO unavailableExchange(String reason) {
        return new ExchangeProvenanceDTO(Availability.UNAVAILABLE, "USD", "BRL", null, null, null, null, null, null, reason);
    }
    private static Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }
    private static String requireCurrency(String value) {
        try { return Currency.getInstance(value).getCurrencyCode(); }
        catch (RuntimeException ex) { throw validation("Moeda ISO inválida no dado persistido."); }
    }
    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw validation("Página deve ser >= 0 e tamanho entre 1 e 100.");
    }
    private static TipoTransacao parseType(String value) {
        if (value == null || value.isBlank()) return null;
        try { return TipoTransacao.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw validation("Tipo deve ser COMPRA ou VENDA."); }
    }
    private static BusinessException validation(String message) { return new BusinessException("VALIDATION_ERROR", message); }
    private record ExchangeState(BigDecimal usableRate, ExchangeProvenanceDTO dto) {}
}
