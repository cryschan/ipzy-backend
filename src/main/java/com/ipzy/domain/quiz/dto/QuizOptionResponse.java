package com.ipzy.domain.quiz.dto;

import com.ipzy.domain.quiz.entity.QuizOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuizOptionResponse {

    @Schema(description = "옵션 ID", example = "101")
    private Long optionId;

    @Schema(description = "옵션 표시 텍스트", example = "깔끔하게")
    private String text;

    @Schema(description = "옵션 값(value). 내부 로직에서 사용되는 식별용 문자열", example = "clean")
    private String value;

    @Schema(description = "옵션 이미지 URL (없을 경우 null)", example = "https://cdn.ipzy.com/options/clean.png")
    private String imageUrl;

    @Schema(description = "옵션 노출 순서", example = "1")
    private Integer displayOrder;


    public static QuizOptionResponse from(QuizOption option) {
        return new QuizOptionResponse(
                option.getId(),
                option.getText(),
                option.getValue(),
                option.getImageUrl(),
                option.getDisplayOrder()
        );
    }

}
