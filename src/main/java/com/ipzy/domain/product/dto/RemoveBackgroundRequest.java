package com.ipzy.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 파이썬 서버 누끼 제거 API 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveBackgroundRequest {

    /**
     * 누끼를 제거할 이미지 URL 리스트
     */
    @JsonProperty("image_urls")
    private List<String> imageUrls;
}
