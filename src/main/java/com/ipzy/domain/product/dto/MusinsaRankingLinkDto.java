package com.ipzy.domain.product.dto;

/**
 * 무신사 랭킹에서 가져온 상품 링크 정보
 */
public record MusinsaRankingLinkDto(
        int rank,
        String url
) {
}
