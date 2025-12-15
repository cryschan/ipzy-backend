package com.ipzy.domain.recommendation.client;

import com.ipzy.domain.recommendation.dto.request.QuizAnswerDto;
import com.ipzy.domain.recommendation.dto.request.RecommendationRequest;
import com.ipzy.domain.recommendation.dto.response.RecommendationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockAiClient 테스트")
class MockAiClientTest {

    private MockAiClient mockAiClient;

    @BeforeEach
    void setUp() {
        mockAiClient = new MockAiClient();
    }

    @Nested
    @DisplayName("testConnection")
    class TestConnection {

        @Test
        @DisplayName("메시지 전송 시 Mock 응답 반환")
        void testConnection_returnsMockResponse() {
            // given
            String message = "안녕하세요";

            // when
            String result = mockAiClient.testConnection(message);

            // then
            assertThat(result).isNotNull();
            assertThat(result).contains("[Mock] Python 응답");
            assertThat(result).contains(message);
        }

        @Test
        @DisplayName("빈 메시지도 처리 가능")
        void testConnection_withEmptyMessage() {
            // given
            String message = "";

            // when
            String result = mockAiClient.testConnection(message);

            // then
            assertThat(result).isNotNull();
            assertThat(result).contains("[Mock] Python 응답");
        }

        @Test
        @DisplayName("특수문자 포함 메시지 처리")
        void testConnection_withSpecialCharacters() {
            // given
            String message = "테스트!@#$%^&*()";

            // when
            String result = mockAiClient.testConnection(message);

            // then
            assertThat(result).isNotNull();
            assertThat(result).contains(message);
        }
    }

    @Nested
    @DisplayName("requestRecommendation")
    class RequestRecommendation {

        @Test
        @DisplayName("성공 - Mock 추천 응답 반환")
        void success() {
            // Given
            RecommendationRequest request = new RecommendationRequest(
                    1L,
                    List.of(
                            new QuizAnswerDto(1L, "선호하는 스타일은?", List.of("캐주얼")),
                            new QuizAnswerDto(2L, "선호하는 색상은?", List.of("블랙", "화이트"))
                    )
            );

            // When
            RecommendationResponse response = mockAiClient.requestRecommendation(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.recommendedOutfits()).isNotEmpty();
            assertThat(response.recommendedOutfits()).hasSize(2);
            assertThat(response.recommendedOutfits().get(0).style()).isNotNull();
            assertThat(response.recommendedOutfits().get(0).items()).isNotEmpty();
        }

        @Test
        @DisplayName("성공 - 빈 답변도 정상 처리")
        void success_emptyAnswers() {
            // Given
            RecommendationRequest request = new RecommendationRequest(2L, List.of());

            // When
            RecommendationResponse response = mockAiClient.requestRecommendation(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.recommendedOutfits()).isNotEmpty();
        }

        @Test
        @DisplayName("Mock 응답에 캐주얼/비즈니스 캐주얼 스타일 포함")
        void success_containsExpectedStyles() {
            // Given
            RecommendationRequest request = new RecommendationRequest(1L, List.of());

            // When
            RecommendationResponse response = mockAiClient.requestRecommendation(request);

            // Then
            assertThat(response.recommendedOutfits())
                    .extracting("style")
                    .containsExactlyInAnyOrder("캐주얼", "비즈니스 캐주얼");
        }
    }
}
