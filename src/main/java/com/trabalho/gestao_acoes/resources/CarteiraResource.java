package com.trabalho.gestao_acoes.resources;

import com.trabalho.gestao_acoes.domains.dtos.PosicaoDTO;
import com.trabalho.gestao_acoes.domains.dtos.TransacaoRequestDTO;
import com.trabalho.gestao_acoes.services.CarteiraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/carteira")
public class CarteiraResource {

    private final CarteiraService service;

    public CarteiraResource(CarteiraService service) {
        this.service = service;
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
        return ResponseEntity.ok(service.calcularSaldoTotal());
    }

    @GetMapping("/posicoes")
    public ResponseEntity<List<PosicaoDTO>> listarPosicoes() {
        return ResponseEntity.ok(service.listarPosicoes());
    }
}
