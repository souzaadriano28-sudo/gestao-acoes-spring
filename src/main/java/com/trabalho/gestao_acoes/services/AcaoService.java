package com.trabalho.gestao_acoes.services;

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

    @Autowired
    private AcaoRepository repository;

    // A MÁGICA DO STRATEGY: O Spring injeta TODAS as suas estratégias aqui automaticamente!
    @Autowired
    private List<CotacaoStrategy> estrategias;

    // RF07 e RF08: Cadastrar ação buscando cotação em tempo real
    public AcaoDTO insert(AcaoDTO dto) {

        // RF12: Impedir ticker duplicado
        String tickerFormatado = dto.getTicker().toUpperCase();
        Optional<Acao> existente = repository.findByTicker(tickerFormatado);
        if (existente.isPresent()) {
            throw new RuntimeException("O Ticker " + tickerFormatado + " já está cadastrado.");
        }

        // 1. O Padrão Strategy em Ação: Acha a API correta sem usar IF/ELSE
        CotacaoStrategy estrategiaCerta = estrategias.stream()
                .filter(e -> e.suportaMercado(dto.getMercado()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Mercado não suportado. Digite NACIONAL ou INTERNACIONAL no JSON."));

        // 2. Busca o preço real na API que "levantou a mão"
        CotacaoBolsa cotacao = estrategiaCerta.buscarCotacao(tickerFormatado);

        // 3. Atualiza os dados para salvar
        dto.setTicker(tickerFormatado);
        dto.setCotacaoAtual(cotacao.getPrecoAtual());
        dto.setMoeda(cotacao.getMoeda());
        dto.setDataHoraCotacao(LocalDateTime.now());

        // 4. Salva no banco e retorna
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