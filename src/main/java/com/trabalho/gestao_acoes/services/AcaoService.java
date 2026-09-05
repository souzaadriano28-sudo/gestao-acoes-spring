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

    public AcaoService(AcaoRepository repository, CotacaoService cotacaoService) {
        this.repository = repository;
        this.cotacaoService = cotacaoService;
    }

    public AcaoDTO insert(AcaoDTO dto) {
        if (dto.getId() != null) throw new BusinessException("VALIDATION_ERROR", "O ID não deve ser informado no cadastro.");
        String ticker = Identifiers.ticker(dto.getTicker());
        String market = Identifiers.mercado(dto.getMercado());
        Identifiers.validateTickerForMarket(ticker, market);
        if (repository.findByTicker(ticker).isPresent()) throw new ConflictException("DUPLICATE_TICKER", "Ticker já cadastrado.");

        CotacaoBolsa quote = cotacaoService.buscar(ticker, market);
        log.info("Cotação validada para ticker [{}] no mercado [{}]", ticker, market);
        dto.setId(null);
        dto.setTicker(ticker);
        dto.setMercado(market);
        dto.setCotacaoAtual(quote.getPrecoAtual());
        dto.setMoeda(quote.getMoeda());
        dto.setDataHoraCotacao(LocalDateTime.now());
        return AcaoMapper.toDTO(repository.save(AcaoMapper.toEntity(dto)));
    }

    public List<AcaoDTO> findAll() { return repository.findAll().stream().map(AcaoMapper::toDTO).toList(); }

    public AcaoDTO findById(Long id) {
        return AcaoMapper.toDTO(repository.findById(id).orElseThrow(() -> new NotFoundException("Ação não encontrada.")));
    }

    public AcaoDTO findByTicker(String ticker) {
        String canonical = Identifiers.ticker(ticker);
        Identifiers.validateTickerSyntax(canonical);
        return AcaoMapper.toDTO(repository.findByTicker(canonical).orElseThrow(() -> new NotFoundException("Ação não encontrada.")));
    }

    public AcaoDTO atualizarCotacao(Long id) {
        Acao entity = repository.findById(id).orElseThrow(() -> new NotFoundException("Ação não encontrada."));
        CotacaoBolsa quote = cotacaoService.buscar(entity.getTicker(), entity.getMercado());
        entity.setCotacaoAtual(quote.getPrecoAtual());
        entity.setDataHoraCotacao(LocalDateTime.now());
        return AcaoMapper.toDTO(repository.save(entity));
    }
}
