package com.trabalho.gestao_acoes.resources;

import com.trabalho.gestao_acoes.domains.dtos.CorretoraDTO;
import com.trabalho.gestao_acoes.services.CorretoraService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/corretoras") // O endereço base de todas as requisições desta classe
public class CorretoraResource {

    @Autowired
    private CorretoraService service;

    // 1. POST /corretoras (Cadastrar)
    @PostMapping
    public ResponseEntity<CorretoraDTO> insert(@Valid @RequestBody CorretoraDTO dto) {
        CorretoraDTO newDto = service.insert(dto);

        // Boa prática: Retornar o código 201 (Created) e o endereço (URI) do novo recurso criado
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(newDto.getId()).toUri();

        return ResponseEntity.created(uri).body(newDto);
    }

    // 2. GET /corretoras (Listar todas)
    @GetMapping
    public ResponseEntity<List<CorretoraDTO>> findAll() {
        List<CorretoraDTO> list = service.findAll();
        return ResponseEntity.ok().body(list); // Retorna 200 OK
    }

    // 3. GET /corretoras/{id} (Buscar por ID)
    @GetMapping("/{id}")
    public ResponseEntity<CorretoraDTO> findById(@PathVariable Long id) {
        CorretoraDTO dto = service.findById(id);
        return ResponseEntity.ok().body(dto);
    }

    // 4. GET /corretoras/cnpj/{cnpj} (Buscar por CNPJ)
    @GetMapping("/cnpj/{cnpj}")
    public ResponseEntity<CorretoraDTO> findByCnpj(@PathVariable String cnpj) {
        CorretoraDTO dto = service.findByCnpj(cnpj);
        return ResponseEntity.ok().body(dto);
    }
}