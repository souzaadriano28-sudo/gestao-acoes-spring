package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Acao;
import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.PosicaoCarteira;
import com.trabalho.gestao_acoes.domains.Transacao;
import com.trabalho.gestao_acoes.domains.dtos.PosicaoDTO;
import com.trabalho.gestao_acoes.domains.enums.TipoTransacao;
import com.trabalho.gestao_acoes.repositories.AcaoRepository;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.repositories.PosicaoCarteiraRepository;
import com.trabalho.gestao_acoes.repositories.TransacaoRepository;
import com.trabalho.gestao_acoes.services.ports.CotacaoStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CarteiraService {

    private final TransacaoRepository transacaoRepository;
    private final PosicaoCarteiraRepository posicaoRepository;
    private final AcaoRepository acaoRepository;
    private final CorretoraRepository corretoraRepository;
    private final List<CotacaoStrategy> estrategias;

    public CarteiraService(TransacaoRepository transacaoRepository,
                           PosicaoCarteiraRepository posicaoRepository,
                           AcaoRepository acaoRepository,
                           CorretoraRepository corretoraRepository,
                           List<CotacaoStrategy> estrategias) {
        this.transacaoRepository = transacaoRepository;
        this.posicaoRepository = posicaoRepository;
        this.acaoRepository = acaoRepository;
        this.corretoraRepository = corretoraRepository;
        this.estrategias = estrategias;
    }

    @Transactional
    public void comprar(String ticker, String mercado, Integer qtd, Long corretoraId) {
        Corretora corretora = corretoraRepository.findById(corretoraId)
                .orElseThrow(() -> new RuntimeException("Corretora não encontrada"));

        Acao acao = acaoRepository.findByTicker(ticker)
                .orElseThrow(() -> new RuntimeException("Ação não cadastrada no sistema"));

        CotacaoStrategy estrategia = estrategias.stream()
                .filter(e -> e.suportaMercado(mercado))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Mercado não suportado."));

        Double precoAtual = estrategia.buscarCotacao(ticker).getPrecoAtual();

        Transacao t = new Transacao(null, TipoTransacao.COMPRA, qtd, precoAtual, LocalDateTime.now(), acao, corretora);
        transacaoRepository.save(t);

        PosicaoCarteira posicao = posicaoRepository.findByAcaoTickerAndCorretoraId(ticker, corretoraId)
                .orElse(new PosicaoCarteira(null, 0, 0.0, acao, corretora));

        double custoTotalAtual = posicao.getQuantidadeTotal() * posicao.getPrecoMedio();
        double custoNovaCompra = qtd * precoAtual;
        int novaQtdTotal = posicao.getQuantidadeTotal() + qtd;

        posicao.setPrecoMedio((custoTotalAtual + custoNovaCompra) / novaQtdTotal);
        posicao.setQuantidadeTotal(novaQtdTotal);

        posicaoRepository.save(posicao);
    }

    @Transactional
    public void vender(String ticker, String mercado, Integer qtd, Long corretoraId) {
        PosicaoCarteira posicao = posicaoRepository.findByAcaoTickerAndCorretoraId(ticker, corretoraId)
                .orElseThrow(() -> new RuntimeException("Você não possui esta ação nesta corretora"));

        if (posicao.getQuantidadeTotal() < qtd) {
            throw new RuntimeException("Saldo insuficiente para venda");
        }

        CotacaoStrategy estrategia = estrategias.stream()
                .filter(e -> e.suportaMercado(mercado))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Mercado não suportado."));

        Double precoVenda = estrategia.buscarCotacao(ticker).getPrecoAtual();

        Transacao t = new Transacao(null, TipoTransacao.VENDA, qtd, precoVenda, LocalDateTime.now(), posicao.getAcao(), posicao.getCorretora());
        transacaoRepository.save(t);

        posicao.setQuantidadeTotal(posicao.getQuantidadeTotal() - qtd);
        if (posicao.getQuantidadeTotal() == 0) {
            posicaoRepository.delete(posicao);
        } else {
            posicaoRepository.save(posicao);
        }
    }

    public Double calcularSaldoTotal() {
        double saldoTotal = 0.0;
        double taxaCambioFixa = 5.30; // <-- Câmbio fixado para o cálculo do patrimônio

        List<PosicaoCarteira> posicoes = posicaoRepository.findAll();

        for (PosicaoCarteira p : posicoes) {
            CotacaoStrategy est = estrategias.stream()
                    .filter(e -> e.suportaMercado(p.getAcao().getMercado()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Mercado não suportado."));

            Double precoAtual = est.buscarCotacao(p.getAcao().getTicker()).getPrecoAtual();

            // ==========================================
            // CONVERSÃO DE MOEDA PARA O SALDO TOTAL
            // ==========================================
            if (p.getAcao().getMoeda().equalsIgnoreCase("USD")) {
                precoAtual = precoAtual * taxaCambioFixa;
            }

            saldoTotal += (p.getQuantidadeTotal() * precoAtual);
        }

        return saldoTotal;
    }

    public List<PosicaoDTO> listarPosicoes() {
        return posicaoRepository.findAll().stream()
                .map(p -> new PosicaoDTO(
                        p.getAcao().getTicker(),
                        p.getCorretora().getRazaoSocial(),
                        p.getQuantidadeTotal(),
                        p.getPrecoMedio(),
                        p.getAcao().getMoeda() // Passando a moeda para o Front!
                ))
                .collect(Collectors.toList());
    }
}