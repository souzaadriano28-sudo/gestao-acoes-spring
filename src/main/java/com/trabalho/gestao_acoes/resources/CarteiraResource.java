package com.trabalho.gestao_acoes.resources;

import com.trabalho.gestao_acoes.domains.dtos.PosicaoDTO;
import com.trabalho.gestao_acoes.domains.dtos.TransacaoRequestDTO;
import com.trabalho.gestao_acoes.services.CarteiraService;
import com.trabalho.gestao_acoes.services.PortfolioReadService;
import com.trabalho.gestao_acoes.domains.dtos.portfolio.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/carteira")
public class CarteiraResource {

    private final CarteiraService service;
    private final PortfolioReadService reads;

    public CarteiraResource(CarteiraService service, PortfolioReadService reads) {
        this.service = service;
        this.reads = reads;
    }

    @PostMapping("/comprar")
    public ResponseEntity<Void> comprar(@Valid @RequestBody TransacaoRequestDTO dto) {
        service.comprar(dto.getTicker(), dto.getMercado(), dto.getQtd(), dto.getCorretoraId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/vender")
    public ResponseEntity<Void> vender(@Valid @RequestBody TransacaoRequestDTO dto) {
        service.vender(dto.getTicker(), dto.getMercado(), dto.getQtd(), dto.getCorretoraId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/saldo-total")
    public ResponseEntity<java.math.BigDecimal> getSaldoTotal() {
        return ResponseEntity.ok()
                .header("Deprecation", "true")
                .header("Link", "</carteira/dashboard>; rel=\"successor-version\"")
                .body(service.calcularSaldoTotal());
    }

    @GetMapping("/posicoes")
    public ResponseEntity<List<PosicaoDTO>> listarPosicoes() {
        return ResponseEntity.ok()
                .header("Deprecation", "true")
                .header("Link", "</carteira/posicoes/detalhadas>; rel=\"successor-version\"")
                .body(service.listarPosicoes());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> dashboard() { return ResponseEntity.ok(reads.dashboard()); }

    @GetMapping("/posicoes/detalhadas")
    public ResponseEntity<PageDTO<DetailedPositionDTO>> detailedPositions(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String market, @RequestParam(required = false) Long brokerId) {
        return ResponseEntity.ok(reads.detailedPositions(page, size, market, brokerId));
    }

    @GetMapping("/movimentacoes")
    public ResponseEntity<PageDTO<MovementDTO>> movements(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type, @RequestParam(required = false) String ticker,
            @RequestParam(required = false) Long brokerId, @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {
        return ResponseEntity.ok(reads.movements(page, size, type, ticker, brokerId, from, to));
    }
}
