package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Acao;
import com.trabalho.gestao_acoes.domains.dtos.AcaoDTO;
import com.trabalho.gestao_acoes.mappers.AcaoMapper;
import com.trabalho.gestao_acoes.repositories.AcaoRepository;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.ConflictException;
import com.trabalho.gestao_acoes.services.exceptions.NotFoundException;
import com.trabalho.gestao_acoes.services.ports.CotacaoBolsa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AcaoService {
    private static final Logger log = LoggerFactory.getLogger(AcaoService.class);
    private final AcaoRepository repository;
    private final CotacaoService cotacaoService;
    private final SecurityUtils securityUtils;

    public AcaoService(AcaoRepository repository, CotacaoService cotacaoService, SecurityUtils securityUtils) {
        this.repository = repository;
        this.cotacaoService = cotacaoService;
        this.securityUtils = securityUtils;
    }

    public AcaoDTO insert(AcaoDTO dto) {
        if (dto.getId() != null) throw new BusinessException("VALIDATION_ERROR", "O ID não deve ser informado no cadastro.");
        String ticker = Identifiers.ticker(dto.getTicker());
        String market = Identifiers.mercado(dto.getMercado());
        Identifiers.validateTickerForMarket(ticker, market);
        Long ownerId = securityUtils.currentOwnerId();
        if (repository.findByTickerAndMercadoAndOwnerId(ticker, market, ownerId).isPresent()) throw new ConflictException("DUPLICATE_TICKER", "Este ticker já está cadastrado neste mercado.");

        CotacaoBolsa quote = cotacaoService.buscar(ticker, market);
        log.info("Cotação validada para ticker [{}] no mercado [{}]", ticker, market);
        dto.setId(null);
        dto.setNomeEmpresa(requiredCompanyName(quote));
        dto.setTicker(ticker);
        dto.setMercado(market);
        dto.setCotacaoAtual(quote.getPrecoAtual());
        dto.setMoeda(quote.getMoeda());
        dto.setDataHoraCotacao(LocalDateTime.now());
        Acao entity = AcaoMapper.toEntity(dto);
        entity.setOwner(securityUtils.currentUser());
        applyProvenance(entity, quote);
        return AcaoMapper.toDTO(repository.save(entity));
    }

    public List<AcaoDTO> findAll() { return repository.findAllByOwnerId(securityUtils.currentOwnerId()).stream().map(AcaoMapper::toDTO).toList(); }

    /** Provider validation for the review screen. It is deliberately read-only. */
    public AcaoDTO consultar(AcaoDTO input) {
        String ticker = Identifiers.ticker(input.getTicker());
        String market = Identifiers.mercado(input.getMercado());
        Identifiers.validateTickerForMarket(ticker, market);
        CotacaoBolsa quote = cotacaoService.buscar(ticker, market);
        AcaoDTO result = new AcaoDTO();
        result.setTicker(ticker);
        result.setMercado(market);
        result.setNomeEmpresa(requiredCompanyName(quote));
        result.setMoeda(quote.getMoeda());
        result.setCotacaoAtual(quote.getPrecoAtual());
        result.setDataHoraCotacao(java.time.LocalDateTime.ofInstant(quote.getReferenceAt(), java.time.ZoneId.of("America/Sao_Paulo")));
        applyProvenance(result, quote);
        return result;
    }

    public AcaoDTO findById(Long id) {
        return AcaoMapper.toDTO(repository.findByIdAndOwnerId(id, securityUtils.currentOwnerId()).orElseThrow(() -> new NotFoundException("Ação não encontrada.")));
    }

    public AcaoDTO findByTicker(String ticker) {
        String canonical = Identifiers.ticker(ticker);
        Identifiers.validateTickerSyntax(canonical);
        return AcaoMapper.toDTO(repository.findAllByOwnerId(securityUtils.currentOwnerId()).stream().filter(acao -> canonical.equals(acao.getTicker())).findFirst().orElseThrow(() -> new NotFoundException("Ação não encontrada.")));
    }

    public AcaoDTO atualizarCotacao(Long id) {
        Acao entity = repository.findByIdAndOwnerId(id, securityUtils.currentOwnerId()).orElseThrow(() -> new NotFoundException("Ação não encontrada."));
        CotacaoBolsa quote = cotacaoService.buscar(entity.getTicker(), entity.getMercado());
        entity.setCotacaoAtual(quote.getPrecoAtual());
        entity.setNomeEmpresa(requiredCompanyName(quote));
        entity.setDataHoraCotacao(LocalDateTime.now());
        applyProvenance(entity, quote);
        return AcaoMapper.toDTO(repository.save(entity));
    }


    private static void applyProvenance(Acao entity, CotacaoBolsa quote) {
        entity.setQuoteSourceType(quote.getSourceType());
        entity.setQuoteProvider(quote.getProvider());
        entity.setQuoteReferenceAt(quote.getReferenceAt());
        entity.setQuoteFetchedAt(quote.getFetchedAt());
        entity.setQuoteReferenceKind(quote.getReferenceKind());
    }
    private static void applyProvenance(AcaoDTO dto, CotacaoBolsa quote) {
        dto.setQuoteSourceType(quote.getSourceType());
        dto.setQuoteProvider(quote.getProvider());
        dto.setQuoteReferenceAt(quote.getReferenceAt());
        dto.setQuoteFetchedAt(quote.getFetchedAt());
        dto.setQuoteReferenceKind(quote.getReferenceKind());
    }
    private static String requiredCompanyName(CotacaoBolsa quote) {
        if (quote.getNomeEmpresa() == null || quote.getNomeEmpresa().isBlank()) throw new BusinessException("INVALID_QUOTE", "O provedor não informou o nome da empresa.");
        return quote.getNomeEmpresa().trim();
    }
}
