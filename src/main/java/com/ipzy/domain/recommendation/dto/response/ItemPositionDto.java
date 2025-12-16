package com.ipzy.domain.recommendation.dto.response;

/**
 * 아이템 위치 정보 DTO - Python FastAPI 응답용
 */
public record ItemPositionDto(
        Integer x,
        Integer y,
        Integer width,
        Integer height
) {
}
