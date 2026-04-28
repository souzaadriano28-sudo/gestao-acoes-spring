package com.trabalho.gestao_acoes.resources;

import com.trabalho.gestao_acoes.domains.dtos.AcaoDTO;
import com.trabalho.gestao_acoes.services.AcaoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/acoes")
public class AcaoResource {

    @Autowired
    private AcaoService service;

    // 1. POST /acoes (Cadastrar)
    @PostMapping
    public ResponseEntity<AcaoDTO> insert(@Valid @RequestBody AcaoDTO dto) {
        AcaoDTO newDto = service.insert(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(newDto.getId()).toUri();
        return ResponseEntity.created(uri).body(newDto);
    }

    // 2. GET /acoes (Listar todas)
    @GetMapping
    public ResponseEntity<List<AcaoDTO>> findAll() {
        List<AcaoDTO> list = service.findAll();
        return ResponseEntity.ok().body(list);
    }

    // 3. GET /acoes/{id} (Buscar por ID)
    @GetMapping("/{id}")
    public ResponseEntity<AcaoDTO> findById(@PathVariable Long id) {
        AcaoDTO dto = service.findById(id);
        return ResponseEntity.ok().body(dto);
    }

    // 4. GET /acoes/ticker/{ticker} (Buscar por Ticker)
    @GetMapping("/ticker/{ticker}")
    public ResponseEntity<AcaoDTO> findByTicker(@PathVariable String ticker) {
        AcaoDTO dto = service.findByTicker(ticker);
        return ResponseEntity.ok().body(dto);
    }

    // 5. PUT /acoes/{id}/atualizar-cotacao (Atualizar Preço)
    @PutMapping("/{id}/atualizar-cotacao")
    public ResponseEntity<AcaoDTO> atualizarCotacao(@PathVariable Long id) {
        AcaoDTO dto = service.atualizarCotacao(id);
        return ResponseEntity.ok().body(dto);
    }
}