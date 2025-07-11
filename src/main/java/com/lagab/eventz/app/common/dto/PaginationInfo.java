package com.lagab.eventz.app.common.dto;

//@Schema(description = "Pagination information")
public record PaginationInfo(
        //@Schema(description = "Current page number (0-based)", example = "0")
        int page,
        //@Schema(description = "Number of items per page", example = "10")
        int size,
        //@Schema(description = "Total number of items", example = "25")
        long totalElements,
        //@Schema(description = "Total number of pages", example = "3")
        int totalPages,
        //@Schema(description = "Whether this is the first page")
        boolean first,
        //@Schema(description = "Whether this is the last page")
        boolean last,
        //@Schema(description = "Whether there is a next page")
        boolean hasNext,
        //@Schema(description = "Whether there is a previous page")
        boolean hasPrevious) {
}
