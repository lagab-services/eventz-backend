package com.lagab.eventz.app.common.dto;

import java.util.List;

import org.springframework.data.domain.Page;

//@Schema(description = "Paginated response wrapper")
public record PageResponse<T>(
        //@Schema(description = "List of items for current page")
        List<T> data,
        //@Schema(description = "Pagination metadata")
        PaginationInfo pagination
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        PaginationInfo paginationInfo = new PaginationInfo(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );

        return new PageResponse<>(
                page.getContent(),
                paginationInfo
        );
    }
}
