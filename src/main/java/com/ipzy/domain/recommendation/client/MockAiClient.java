package com.ipzy.domain.recommendation.client;

import com.ipzy.domain.recommendation.dto.request.RecommendationRequest;
import com.ipzy.domain.recommendation.dto.response.OutfitRecommendationDto;
import com.ipzy.domain.recommendation.dto.response.RecommendationResponse;
import com.ipzy.domain.recommendation.dto.response.RecommendedItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 추천 서비스 Mock 클라이언트
 * <p>
 * Python FastAPI가 준비되지 않은 환경에서 사용 (prod 제외 모든 환경)
 * <p>
 * 활성화 조건: prod 프로파일이 아닌 모든 경우
 */
@Slf4j
@Component
@Profile("!prod")
public class MockAiClient implements AiRecommendationClient {

    @Override
    public RecommendationResponse requestRecommendation(RecommendationRequest request) {
        log.info("==== [Mock] AI 추천 요청 시작 ====");
        log.info("세션 ID: {}, 답변 수: {}", request.sessionId(), request.answers().size());

        RecommendationResponse mockResponse = createMockResponse();

        log.info("[Mock] 추천 응답 생성: {} 개 코디", mockResponse.recommendedOutfits().size());
        log.info("==== [Mock] AI 추천 요청 완료 ====");

        return mockResponse;
    }

    @Override
    public String testConnection(String msg) {
        log.info("==== [Mock] 통신 테스트 ====");
        log.info("보내는 메시지: {}", msg);

        String mockResponse = "[Mock] Python 응답: " + msg;

        log.info("받은 응답: {}", mockResponse);
        return mockResponse;
    }

    /**
     * Mock 추천 응답 생성
     */
    private RecommendationResponse createMockResponse() {
        return new RecommendationResponse(
                List.of(
                        createCasualOutfit(),
                        createBusinessCasualOutfit()
                )
        );
    }

    private OutfitRecommendationDto createCasualOutfit() {
        return new OutfitRecommendationDto(
                1,
                "데이트",
                "봄",
                "캐주얼",
                "밝은 색감의 캐주얼 룩으로 데이트에 적합합니다.",
                237000,
                "https://example.com/style_board1.jpg",
                List.of(
                        new RecommendedItemDto(1L, "TOP", "오버핏 옥스포드 셔츠", "무신사 스탠다드", 59000, "https://example.com/image1.jpg", "https://example.com/product1"),
                        new RecommendedItemDto(2L, "BOTTOM", "와이드 슬랙스 팬츠", "커버낫", 79000, "https://example.com/image2.jpg", "https://example.com/product2"),
                        new RecommendedItemDto(3L, "SHOES", "화이트 스니커즈", "나이키", 99000, "https://example.com/image3.jpg", "https://example.com/product3")
                )
        );
    }

    private OutfitRecommendationDto createBusinessCasualOutfit() {
        return new OutfitRecommendationDto(
                2,
                "출근",
                "봄",
                "비즈니스 캐주얼",
                "깔끔한 비즈니스 캐주얼 룩입니다.",
                245000,
                "https://example.com/style_board2.jpg",
                List.of(
                        new RecommendedItemDto(4L, "TOP", "슬림핏 셔츠", "유니클로", 49000, "https://example.com/image4.jpg", "https://example.com/product4"),
                        new RecommendedItemDto(5L, "BOTTOM", "슬랙스", "지오다노", 69000, "https://example.com/image5.jpg", "https://example.com/product5"),
                        new RecommendedItemDto(6L, "SHOES", "로퍼", "탠디", 127000, "https://example.com/image6.jpg", "https://example.com/product6")
                )
        );
    }
}
