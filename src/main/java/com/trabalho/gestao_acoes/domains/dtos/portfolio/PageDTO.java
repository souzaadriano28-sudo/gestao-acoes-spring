package com.trabalho.gestao_acoes.domains.dtos.portfolio;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageDTO<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
    public static <T> PageDTO<T> from(Page<T> page) {
        return new PageDTO<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
