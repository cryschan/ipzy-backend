package com.ipzy.domain.quiz.dto;

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

}
