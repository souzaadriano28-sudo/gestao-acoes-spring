package com.trabalho.gestao_acoes.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.trabalho.gestao_acoes.domains.Acao;
import com.trabalho.gestao_acoes.domains.dtos.AcaoDTO;
import com.trabalho.gestao_acoes.mappers.AcaoMapper;
import com.trabalho.gestao_acoes.repositories.AcaoRepository;
import com.trabalho.gestao_acoes.services.ports.CotacaoBolsa;
import com.trabalho.gestao_acoes.services.ports.CotacaoStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AcaoService {

    private static final Logger log = LoggerFactory.getLogger(AcaoService.class);

    @Autowired
    private AcaoRepository repository;

    // A MÁGICA DO STRATEGY: O Spring injeta TODAS as suas estratégias aqui automaticamente!
    @Autowired
    private List<CotacaoStrategy> estrategias;

    // RF07 e RF08: Cadastrar ação buscando cotação em tempo real
    public AcaoDTO insert(AcaoDTO dto) {
        String tickerFormatado = dto.getTicker().toUpperCase();
        log.info("Iniciando cadastro de nova ação. Ticker: [{}], Mercado: [{}]", tickerFormatado, dto.getMercado());

        // ==========================================
        // TRAVA 1: Validação Sintática do Ticker (Padrão Regex)
        // ==========================================
        if (dto.getMercado().equalsIgnoreCase("BRASIL")) {
            // Regex: Exatamente 4 letras, seguidas de 1 ou 2 números (PETR4, TAEE11, AAPL34)
            if (!tickerFormatado.matches("^[A-Z]{4}\\d{1,2}$")) {
                throw new RuntimeException("Formato inválido. Tickers nacionais possuem 4 letras e 1 ou 2 números (Ex: PETR4, TAEE11).");
            }
        } else if (dto.getMercado().equalsIgnoreCase("AMERICANO")) {
            // Regex: De 1 a 5 letras, NENHUM número (AAPL, TSLA, F)
            if (!tickerFormatado.matches("^[A-Z]{1,5}$")) {
                throw new RuntimeException("Formato inválido. Tickers americanos contêm apenas letras (Ex: AAPL, MSFT, TSLA).");
            }
        }

        // RF12: Impedir ticker duplicado
        Optional<Acao> existente = repository.findByTicker(tickerFormatado);
        if (existente.isPresent()) {
            throw new RuntimeException("O Ticker " + tickerFormatado + " já está cadastrado.");
        }

        CotacaoStrategy estrategiaCerta = estrategias.stream()
                .filter(e -> e.suportaMercado(dto.getMercado()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Mercado não suportado."));

        CotacaoBolsa cotacao = estrategiaCerta.buscarCotacao(tickerFormatado);

        // ==========================================
        // TRAVA 2: Validação de Moeda cruzada
        // ==========================================
        if (dto.getMercado().equalsIgnoreCase("BRASIL") && !cotacao.getMoeda().equalsIgnoreCase("BRL")) {
            throw new RuntimeException("O ativo " + tickerFormatado + " não é Nacional. A API retornou " + cotacao.getMoeda() + ".");
        }
        if (dto.getMercado().equalsIgnoreCase("AMERICANO") && !cotacao.getMoeda().equalsIgnoreCase("USD")) {
            throw new RuntimeException("O ativo " + tickerFormatado + " não é Internacional. A API retornou " + cotacao.getMoeda() + ".");
        }

        log.info("Cotação encontrada com sucesso! Preço: {} {}", cotacao.getPrecoAtual(), cotacao.getMoeda());

        dto.setTicker(tickerFormatado);
        dto.setCotacaoAtual(cotacao.getPrecoAtual());
        dto.setMoeda(cotacao.getMoeda());
        dto.setDataHoraCotacao(LocalDateTime.now());

        Acao entity = AcaoMapper.toEntity(dto);
        entity = repository.save(entity);

        return AcaoMapper.toDTO(entity);
    }

    // RF09: Listar ações cadastradas
    public List<AcaoDTO> findAll() {
        return repository.findAll().stream()
                .map(AcaoMapper::toDTO)
                .collect(Collectors.toList());
    }

    // GET: Buscar ação por ID (O método que faltava!)
    public AcaoDTO findById(Long id) {
        Acao entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ação não encontrada com o ID: " + id));
        return AcaoMapper.toDTO(entity);
    }

    // RF10: Buscar ação por ticker
    public AcaoDTO findByTicker(String ticker) {
        Acao entity = repository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Ação não encontrada com o Ticker: " + ticker));
        return AcaoMapper.toDTO(entity);
    }

    // RF11: Atualizar a cotação de uma ação já cadastrada
    public AcaoDTO atualizarCotacao(Long id) {
        Acao entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ação não encontrada com o ID: " + id));

        // Pega a estratégia correta baseada no mercado salvo no banco
        CotacaoStrategy estrategiaCerta = estrategias.stream()
                .filter(e -> e.suportaMercado(entity.getMercado()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Estratégia perdida para o mercado: " + entity.getMercado()));

        // Bate na API de novo para pegar o preço mais recente
        CotacaoBolsa novaCotacao = estrategiaCerta.buscarCotacao(entity.getTicker());

        // Atualiza os valores
        entity.setCotacaoAtual(novaCotacao.getPrecoAtual());
        entity.setDataHoraCotacao(LocalDateTime.now());

        // CORREÇÃO: Criamos uma nova variável em vez de reatribuir 'entity'
        Acao entityAtualizada = repository.save(entity);

        return AcaoMapper.toDTO(entityAtualizada);
    }
}