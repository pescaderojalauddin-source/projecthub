package com.example.projecthub.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Универсальная обёртка над {@link Page} — без зависимости от Spring-внутренностей в JSON.
 */
@Schema(description = "Страница результата с пагинацией")
public record PageDto<T>(
        List<T> content,
        @Schema(example = "0") int page,
        @Schema(example = "20") int size,
        @Schema(example = "5") long totalElements,
        @Schema(example = "1") int totalPages
) {
    public static <T> PageDto<T> of(Page<T> page) {
        return new PageDto<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
