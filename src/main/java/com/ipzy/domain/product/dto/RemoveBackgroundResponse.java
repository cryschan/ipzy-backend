package com.ipzy.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 파이썬 서버 누끼 제거 API 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveBackgroundResponse {

    /**
     * 누끼 제거 결과 리스트
     */
    private List<ImageResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageResult {
        /**
         * 원본 이미지 URL
         */
        private String originalUrl;

        /**
         * 누끼 제거된 이미지 URL
         */
        private String removedBackgroundUrl;

        /**
         * 처리 성공 여부
         */
        private Boolean success;

        /**
         * 실패 시 에러 메시지
         */
        private String errorMessage;
    }
}
