package com.ipzy.global.common;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 래퍼 - 목록 데이터와 페이지네이션 정보 포함
 */
@Getter
public class PageResponse<T> {

    private final List<T> items;
    private final PaginationInfo pagination;

    private PageResponse(List<T> items, PaginationInfo pagination) {
        this.items = items;
        this.pagination = pagination;
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                PaginationInfo.from(page)
        );
    }

    @Getter
    public static class PaginationInfo {
        private final int currentPage;
        private final int totalPages;
        private final long totalItems;
        private final int itemsPerPage;
        private final boolean hasNext;
        private final boolean hasPrev;

        private PaginationInfo(int currentPage, int totalPages, long totalItems,
                               int itemsPerPage, boolean hasNext, boolean hasPrev) {
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.totalItems = totalItems;
            this.itemsPerPage = itemsPerPage;
            this.hasNext = hasNext;
            this.hasPrev = hasPrev;
        }

        public static PaginationInfo from(Page<?> page) {
            return new PaginationInfo(
                    page.getNumber() + 1,  // 0-based → 1-based
                    page.getTotalPages(),
                    page.getTotalElements(),
                    page.getSize(),
                    page.hasNext(),
                    page.hasPrevious()
            );
        }
    }
}
