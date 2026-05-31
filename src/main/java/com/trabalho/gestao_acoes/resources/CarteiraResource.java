package com.trabalho.gestao_acoes.resources;

import com.trabalho.gestao_acoes.domains.dtos.PosicaoDTO;
import com.trabalho.gestao_acoes.domains.dtos.TransacaoRequestDTO;
import com.trabalho.gestao_acoes.services.CarteiraService;
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
    public ResponseEntity<Void> comprar(@RequestBody TransacaoRequestDTO dto) {
        service.comprar(dto.getTicker(), dto.getMercado(), dto.getQtd(), dto.getCorretoraId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/vender")
    public ResponseEntity<Void> vender(@RequestBody TransacaoRequestDTO dto) {
        service.vender(dto.getTicker(), dto.getMercado(), dto.getQtd(), dto.getCorretoraId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/saldo-total")
    public ResponseEntity<Double> getSaldoTotal() {
        return ResponseEntity.ok(service.calcularSaldoTotal());
    }

    @GetMapping("/posicoes")
    public ResponseEntity<List<PosicaoDTO>> listarPosicoes() {
        return ResponseEntity.ok(service.listarPosicoes());
    }
}