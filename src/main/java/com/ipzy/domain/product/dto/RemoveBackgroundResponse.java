package com.ipzy.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 파이썬 서버 배치 누끼 제거 API 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveBackgroundResponse {

    /**
     * 전체 배치 처리 성공 여부
     */
    private Boolean success;

    /**
     * 누끼 제거 결과 리스트
     */
    private List<ImageResult> results;

    /**
     * 전체 이미지 개수
     */
    @JsonProperty("total_count")
    private Integer totalCount;

    /**
     * 성공한 이미지 개수
     */
    @JsonProperty("success_count")
    private Integer successCount;

    /**
     * 실패한 이미지 개수
     */
    @JsonProperty("failed_count")
    private Integer failedCount;

    /**
     * 총 처리 시간(초)
     */
    @JsonProperty("processing_time")
    private Double processingTime;

    /**
     * 상태 메시지
     */
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageResult {
        /**
         * 원본 이미지 URL
         */
        @JsonProperty("original_url")
        private String originalUrl;

        /**
         * 누끼 제거된 이미지 URL
         */
        @JsonProperty("nobg_image_url")
        private String removedBackgroundUrl;

        /**
         * 처리 성공 여부
         */
        private boolean success;

        /**
         * 실패 시 에러 메시지
         */
        private String error;
    }
}
