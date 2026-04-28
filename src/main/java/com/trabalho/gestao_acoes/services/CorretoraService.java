package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.dtos.CorretoraDTO;
import com.trabalho.gestao_acoes.integrations.brasilapi.BrasilApiResponse;
import com.trabalho.gestao_acoes.integrations.viacep.ViaCepResponse;
import com.trabalho.gestao_acoes.mappers.CorretoraMapper;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.services.ports.CepClientPort;
import com.trabalho.gestao_acoes.services.ports.CnpjClientPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CorretoraService {

    @Autowired
    private CorretoraRepository repository;

    @Autowired
    private CnpjClientPort cnpjClient;

    @Autowired
    private CepClientPort cepClient;

    // RF01: Cadastrar corretora
    public CorretoraDTO insert(CorretoraDTO dto) {

        // 1. RF12: Impedir cadastro duplicado por CNPJ
        Optional<Corretora> existente = repository.findByCnpj(dto.getCnpj());
        if (existente.isPresent()) {
            throw new RuntimeException("CNPJ já cadastrado no sistema.");
        }

        // 2. RF02 e RN01: Consultar CNPJ na API Externa
        BrasilApiResponse dadosCnpj = cnpjClient.buscarDadosPorCnpj(dto.getCnpj());
        if (dadosCnpj == null || dadosCnpj.getRazaoSocial() == null) {
            throw new RuntimeException("CNPJ inválido ou não encontrado na base de dados.");
        }

        // 3. RN02: Registrar os dados vindos da consulta (bloqueia preenchimento manual)
        dto.setRazaoSocial(dadosCnpj.getRazaoSocial());
        dto.setNomeFantasia(dadosCnpj.getNomeFantasia() != null ? dadosCnpj.getNomeFantasia() : dadosCnpj.getRazaoSocial());
        dto.setSituacaoCadastral(dadosCnpj.getDescricaoSituacaoCadastral());

        // 4. RN03 e RF03: Validação na "CVM" pelo CNAE
        // 6612601: Corretoras de títulos e valores mobiliários (CTVM)
        // 6612602: Distribuidoras de títulos e valores mobiliários (DTVM)
        // 6431900: Bancos de investimento (BTG, Itaú BBA, etc)
        java.util.List<Integer> cnaesValidos = java.util.Arrays.asList(6612601, 6612602, 6431900);

        if (dadosCnpj.getCnaeFiscal() != null && cnaesValidos.contains(dadosCnpj.getCnaeFiscal())) {
            dto.setValidadaNaCvm(true);
        } else {
            dto.setValidadaNaCvm(false); // Marca como não validada se for padaria, posto de gasolina, etc.
        }

        // 5. RF04 e RN04: Consultar e validar CEP
        ViaCepResponse dadosCep = cepClient.buscarEnderecoPorCep(dto.getCep());
        if (dadosCep == null || (dadosCep.getErro() != null && dadosCep.getErro())) {
            throw new RuntimeException("CEP inválido ou inexistente.");
        }

        // Preenche o endereço com os dados oficiais do ViaCEP
        dto.setLogradouro(dadosCep.getLogradouro());
        dto.setBairro(dadosCep.getBairro());
        dto.setCidade(dadosCep.getLocalidade());
        dto.setUf(dadosCep.getUf());
        // Obs: O número e o complemento o usuário já enviou no DTO original

        // 6. Converter para Entidade, setar a data e salvar no banco
        Corretora entity = CorretoraMapper.toEntity(dto);
        entity.setDataCadastro(LocalDateTime.now());

        entity = repository.save(entity);

        // 7. Retornar os dados consolidados e salvos
        return CorretoraMapper.toDTO(entity);
    }

    // RF05: Listar corretoras cadastradas
    public List<CorretoraDTO> findAll() {
        return repository.findAll().stream()
                .map(CorretoraMapper::toDTO)
                .collect(Collectors.toList());
    }

    // RF06: Buscar corretora por ID
    public CorretoraDTO findById(Long id) {
        Corretora entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Corretora não encontrada com o ID: " + id));
        return CorretoraMapper.toDTO(entity);
    }

    // RF06: Buscar corretora por CNPJ
    public CorretoraDTO findByCnpj(String cnpj) {
        Corretora entity = repository.findByCnpj(cnpj)
                .orElseThrow(() -> new RuntimeException("Corretora não encontrada com o CNPJ: " + cnpj));
        return CorretoraMapper.toDTO(entity);
    }
}