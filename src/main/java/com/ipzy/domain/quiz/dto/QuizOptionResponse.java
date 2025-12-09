package com.ipzy.domain.quiz.dto;

import com.ipzy.domain.quiz.entity.QuizOption;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuizOptionResponse {

    private Long optionId;
    private String text;
    private String value;
    private String imageUrl;
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
