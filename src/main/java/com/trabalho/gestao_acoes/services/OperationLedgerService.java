package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.*;
import com.trabalho.gestao_acoes.domains.dtos.*;
import com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus;
import com.trabalho.gestao_acoes.domains.enums.TipoTransacao;
import com.trabalho.gestao_acoes.repositories.*;
import com.trabalho.gestao_acoes.services.exceptions.*;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Historical ledger: every position is replayed by date/time then database id. */
@Service
public class OperationLedgerService {
  private final TransacaoRepository transactions; private final PosicaoCarteiraRepository positions;
  private final AcaoRepository assets; private final CorretoraRepository brokers; private final PortfolioRepository portfolios; private final SecurityUtils security;
  public OperationLedgerService(TransacaoRepository t, PosicaoCarteiraRepository p, AcaoRepository a, CorretoraRepository b, PortfolioRepository ps, SecurityUtils s) { transactions=t;positions=p;assets=a;brokers=b;portfolios=ps;security=s; }
  private Portfolio portfolio() { return portfolios.findFirstByOwnerIdOrderByIdAsc(security.currentOwnerId()).orElseThrow(() -> new BusinessException("PORTFOLIO_NOT_FOUND", "Carteira padrão não encontrada.")); }

  private Portfolio lockPortfolio() { return portfolios.findFirstByOwnerIdForUpdate(security.currentOwnerId()).orElseThrow(() -> new BusinessException("PORTFOLIO_NOT_FOUND", "Default portfolio was not found.")); }

  @Transactional(readOnly = true) public OperationPreviewDTO preview(OperationRequestDTO request) { Context c=context(request, false); return simulate(c, request, null); }
  @Transactional(readOnly = true) public OperationPreviewDTO previewUpdate(Long id, OperationRequestDTO request) { Portfolio p=portfolio(); Transacao current=transactions.findByIdAndPortfolioId(id,p.getId()).orElseThrow(()->new NotFoundException("Operação não encontrada.")); Context c=context(request,false); if(!current.getAcao().getId().equals(c.asset.getId())||!current.getCorretora().getId().equals(c.broker.getId())||!current.getMoeda().equals(c.currency)) throw new BusinessException("VALIDATION_ERROR","Ativo, corretora e moeda não podem mudar ao editar uma operação."); return simulate(c,request,id); }
  @Transactional public OperationDTO create(OperationRequestDTO request) {
    // The idempotency key is scoped to the portfolio: lock it before the broker.
    Portfolio p=lockPortfolio(); Context c=context(request, true);
    if (request.idempotencyKey()!=null && !request.idempotencyKey().isBlank()) {
      Optional<Transacao> prior=transactions.findByPortfolioIdAndIdempotencyKey(p.getId(), request.idempotencyKey());
      if(prior.isPresent()) { if (same(prior.get(),request)) return dto(prior.get()); throw new ConflictException("IDEMPOTENCY_CONFLICT", "A chave de idempotência já foi usada com outro registro."); }
    }
    // Validate the full chronological replay before inserting anything, including later movements.
    simulate(c, request, null);
    Transacao t=new Transacao(); apply(t,request,c); t.setPortfolio(p); transactions.saveAndFlush(t); replay(c); return dto(t);
  }
  @Transactional public OperationDTO update(Long id, OperationRequestDTO request) {
    lockPortfolio();
    Portfolio p=portfolio(); Transacao t=transactions.findByIdAndPortfolioId(id,p.getId()).orElseThrow(() -> new NotFoundException("Operação não encontrada."));
    Context c=context(request,true); if(!t.getAcao().getId().equals(c.asset.getId()) || !t.getCorretora().getId().equals(c.broker.getId()) || !t.getMoeda().equals(c.currency)) throw new BusinessException("VALIDATION_ERROR", "Ativo, corretora e moeda não podem mudar ao editar uma operação.");
    simulate(c, request, id);
    apply(t,request,c); transactions.saveAndFlush(t); replay(c); return dto(t);
  }
  @Transactional public void delete(Long id) { Portfolio p=portfolio(); Transacao t=transactions.findByIdAndPortfolioId(id,p.getId()).orElseThrow(() -> new NotFoundException("Operação não encontrada.")); Context c=new Context(p,t.getAcao(),lockBroker(t.getCorretora().getId()),t.getMoeda()); validateRemoval(c,id); transactions.delete(t); transactions.flush(); replay(c); }
  @Transactional(readOnly=true) public List<OperationDTO> list(TipoTransacao type,String ticker,Long brokerId,LocalDateTime from,LocalDateTime to) {
    Portfolio p=portfolio(); return transactions.findMovements(p.getId(),type,ticker==null||ticker.isBlank()?null:Identifiers.ticker(ticker),brokerId,from,to,org.springframework.data.domain.Pageable.unpaged()).getContent().stream().map(this::dto).toList(); }

  private Context context(OperationRequestDTO r, boolean mutation) { Portfolio p=portfolio(); String currency=r.moeda().trim().toUpperCase(Locale.ROOT); Acao a=assets.findByIdAndOwnerId(r.ativoId(),security.currentOwnerId()).orElseThrow(()->new NotFoundException("Ativo não encontrado.")); if(!a.getMoeda().equals(currency)) throw new BusinessException("CURRENCY_MISMATCH","A moeda deve corresponder à moeda do ativo.","moeda"); Corretora b=mutation?lockBroker(r.corretoraId()):brokers.findByIdAndOwnerId(r.corretoraId(),security.currentOwnerId()).orElseThrow(()->new NotFoundException("Corretora não encontrada.")); return new Context(p,a,b,currency); }
  private Corretora lockBroker(Long id) { Corretora b=brokers.findByIdAndOwnerIdForUpdate(id,security.currentOwnerId()).orElseThrow(()->new NotFoundException("Corretora não encontrada.")); if(b.getRegulatoryStatus()!=RegulatoryStatus.VERIFIED) throw new BusinessException("BROKER_UNAVAILABLE","A corretora não está autorizada para novos registros."); return b; }
  private void apply(Transacao t,OperationRequestDTO r,Context c) { BigDecimal price=MoneyPolicy.quote(r.precoUnitario()), brokerage=MoneyPolicy.amount(r.corretagem(),"corretagem"), fees=MoneyPolicy.amount(r.taxas(),"taxas"), taxes=MoneyPolicy.amount(r.impostos(),"impostos"), other=MoneyPolicy.amount(r.outrosCustos(),"outrosCustos"); BigDecimal gross=price.multiply(BigDecimal.valueOf(r.quantidade())).setScale(MoneyPolicy.PRICE_SCALE,MoneyPolicy.ROUNDING), costs=brokerage.add(fees).add(taxes).add(other), total=MoneyPolicy.operationTotal(BigDecimal.valueOf(r.quantidade()),price,costs,r.tipo()==TipoTransacao.COMPRA); t.setTipo(r.tipo());t.setAcao(c.asset);t.setCorretora(c.broker);t.setQuantidade(r.quantidade());t.setMoeda(c.currency);t.setPrecoUnitario(price);t.setDataHora(r.dataHora());t.setCorretagem(brokerage);t.setTaxas(fees);t.setImpostos(taxes);t.setOutrosCustos(other);t.setValorBruto(gross);t.setValorTotal(total);t.setObservacao(normalizedObservation(r.observacao()));t.setIdempotencyKey(blankToNull(r.idempotencyKey())); }
  private OperationPreviewDTO simulate(Context c,OperationRequestDTO proposed,Long exclude) { List<Transacao> all=new ArrayList<>(transactions.findByPortfolioIdAndAcaoIdAndCorretoraIdAndMoedaOrderByDataHoraAscIdAsc(c.portfolio.getId(),c.asset.getId(),c.broker.getId(),c.currency)); if(exclude!=null) all.removeIf(t->t.getId().equals(exclude)); Transacao virtual=new Transacao(); apply(virtual,proposed,c); int insertion=0; while(insertion<all.size() && (all.get(insertion).getDataHora().isBefore(virtual.getDataHora()) || (all.get(insertion).getDataHora().equals(virtual.getDataHora()) && (exclude==null || all.get(insertion).getId()<exclude)))) insertion++; State s=new State(); for(int i=0;i<insertion;i++) applyState(s,all.get(i)); int before=s.quantity; BigDecimal pmBefore=s.average; BigDecimal costBefore=s.average.multiply(BigDecimal.valueOf(s.quantity)); applyState(s,virtual); int after=s.quantity; BigDecimal pmAfter=s.average; BigDecimal realizedAfter=virtual.getResultadoRealizado(); for(int i=insertion;i<all.size();i++) applyState(s,all.get(i)); BigDecimal costs=virtual.getCorretagem().add(virtual.getTaxas()).add(virtual.getImpostos()).add(virtual.getOutrosCustos()); return new OperationPreviewDTO(before,pmBefore,costBefore,virtual.getValorBruto(),costs,virtual.getValorTotal(),after,pmAfter,pmAfter.multiply(BigDecimal.valueOf(after)),realizedAfter,c.currency); }
  private void replay(Context c) { List<Transacao> all=transactions.findByPortfolioIdAndAcaoIdAndCorretoraIdAndMoedaOrderByDataHoraAscIdAsc(c.portfolio.getId(),c.asset.getId(),c.broker.getId(),c.currency); State s=new State(); for(Transacao t:all) applyState(s,t); Optional<PosicaoCarteira> existing=positions.findByAcaoIdAndCorretoraIdAndPortfolioId(c.asset.getId(),c.broker.getId(),c.portfolio.getId()); if(s.quantity==0) existing.ifPresent(positions::delete); else { PosicaoCarteira p=existing.orElseGet(PosicaoCarteira::new); p.setAcao(c.asset);p.setCorretora(c.broker);p.setPortfolio(c.portfolio);p.setQuantidadeTotal(s.quantity);p.setPrecoMedio(s.average);p.setResultadoRealizado(s.realized);positions.save(p); } }
  private void validateRemoval(Context c, Long id) { State s=new State(); for(Transacao t:transactions.findByPortfolioIdAndAcaoIdAndCorretoraIdAndMoedaOrderByDataHoraAscIdAsc(c.portfolio.getId(),c.asset.getId(),c.broker.getId(),c.currency)) if(!t.getId().equals(id)) applyState(s,t); }
  private void applyState(State s,Transacao t) { if(t.getTipo()==TipoTransacao.COMPRA) { BigDecimal cost=t.getValorTotal(); int next=Math.addExact(s.quantity,t.getQuantidade()); s.average=s.quantity==0?cost.divide(BigDecimal.valueOf(next),MoneyPolicy.PRICE_SCALE,MoneyPolicy.ROUNDING):s.average.multiply(BigDecimal.valueOf(s.quantity)).add(cost).divide(BigDecimal.valueOf(next),MoneyPolicy.PRICE_SCALE,MoneyPolicy.ROUNDING); s.quantity=next; t.setResultadoRealizado(BigDecimal.ZERO.setScale(MoneyPolicy.PRICE_SCALE)); } else { if(t.getQuantidade()>s.quantity) throw new BusinessException("INSUFFICIENT_POSITION","A operação produz posição negativa na ordem cronológica."); BigDecimal result=t.getValorTotal().subtract(s.average.multiply(BigDecimal.valueOf(t.getQuantidade()))).setScale(MoneyPolicy.PRICE_SCALE,MoneyPolicy.ROUNDING); t.setResultadoRealizado(result); s.realized=s.realized.add(result); s.quantity-=t.getQuantidade(); if(s.quantity==0) s.average=BigDecimal.ZERO.setScale(MoneyPolicy.PRICE_SCALE); } }
  private boolean same(Transacao t,OperationRequestDTO r) { BigDecimal c=MoneyPolicy.amount(r.corretagem(),"corretagem"),f=MoneyPolicy.amount(r.taxas(),"taxas"),i=MoneyPolicy.amount(r.impostos(),"impostos"),o=MoneyPolicy.amount(r.outrosCustos(),"outrosCustos"); return t.getTipo()==r.tipo()&&t.getAcao().getId().equals(r.ativoId())&&t.getCorretora().getId().equals(r.corretoraId())&&t.getQuantidade().equals(r.quantidade())&&t.getMoeda().equals(r.moeda().trim().toUpperCase(Locale.ROOT))&&t.getPrecoUnitario().compareTo(MoneyPolicy.quote(r.precoUnitario()))==0&&t.getDataHora().equals(r.dataHora())&&t.getCorretagem().compareTo(c)==0&&t.getTaxas().compareTo(f)==0&&t.getImpostos().compareTo(i)==0&&t.getOutrosCustos().compareTo(o)==0&&Objects.equals(normalizedObservation(t.getObservacao()),normalizedObservation(r.observacao())); }
  private OperationDTO dto(Transacao t) { return new OperationDTO(t.getId(),t.getTipo().name(),t.getAcao().getId(),t.getAcao().getTicker(),t.getCorretora().getId(),t.getCorretora().getRazaoSocial(),t.getQuantidade(),t.getMoeda(),t.getPrecoUnitario(),t.getCorretagem(),t.getTaxas(),t.getImpostos(),t.getOutrosCustos(),t.getValorBruto(),t.getValorTotal(),t.getResultadoRealizado(),t.getDataHora(),t.getObservacao()); }
  private static String blankToNull(String s){ if(s==null) return null; String normalized=s.trim(); return normalized.isEmpty()?null:normalized; }
  private static String normalizedObservation(String observation) { return blankToNull(observation); }
  private static class State { int quantity; BigDecimal average=BigDecimal.ZERO.setScale(MoneyPolicy.PRICE_SCALE); BigDecimal realized=BigDecimal.ZERO.setScale(MoneyPolicy.PRICE_SCALE); } private record Context(Portfolio portfolio,Acao asset,Corretora broker,String currency) {}
}
