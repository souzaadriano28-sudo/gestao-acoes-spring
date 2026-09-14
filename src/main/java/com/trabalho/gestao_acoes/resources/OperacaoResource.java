package com.trabalho.gestao_acoes.resources;

import com.trabalho.gestao_acoes.domains.dtos.*;
import com.trabalho.gestao_acoes.domains.enums.TipoTransacao;
import com.trabalho.gestao_acoes.services.OperationLedgerService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operacoes")
public class OperacaoResource {
  private final OperationLedgerService service;
  public OperacaoResource(OperationLedgerService service){this.service=service;}
  @PostMapping("/previa") public OperationPreviewDTO preview(@Valid @RequestBody OperationRequestDTO request){return service.preview(request);}
  @PostMapping("/{id}/previa") public OperationPreviewDTO previewUpdate(@PathVariable Long id,@Valid @RequestBody OperationRequestDTO request){return service.previewUpdate(id,request);}
  @PostMapping public ResponseEntity<OperationDTO> create(@Valid @RequestBody OperationRequestDTO request){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));}
  @GetMapping public List<OperationDTO> list(@RequestParam(required=false) TipoTransacao tipo,@RequestParam(required=false) String ticker,@RequestParam(required=false) Long corretoraId,@RequestParam(required=false) LocalDateTime de,@RequestParam(required=false) LocalDateTime ate){return service.list(tipo,ticker,corretoraId,de,ate);}
  @PutMapping("/{id}") public OperationDTO update(@PathVariable Long id,@Valid @RequestBody OperationRequestDTO request){return service.update(id,request);}
  @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id){service.delete(id);return ResponseEntity.noContent().build();}
}
