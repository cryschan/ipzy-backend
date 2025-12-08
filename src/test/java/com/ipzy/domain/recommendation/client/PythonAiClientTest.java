package com.ipzy.domain.recommendation.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("PythonAiClient 단위 테스트")
class PythonAiClientTest {

    @InjectMocks
    private PythonAiClient pythonAiClient;

    @Mock
    private RestClient pythonAiRestClient;

    @Nested
    @DisplayName("testConnection")
    class TestConnection {

        @Test
        @DisplayName("메시지 전송 시 Mock 응답 반환")
        void testConnection_returnsMockResponse() {
            // given
            String message = "안녕하세요";

            // when
            String result = pythonAiClient.testConnection(message);

            // then
            assertThat(result).isNotNull();
            assertThat(result).contains("Python 응답 (Mock)");
            assertThat(result).contains(message);
        }

        @Test
        @DisplayName("빈 메시지도 처리 가능")
        void testConnection_withEmptyMessage() {
            // given
            String message = "";

            // when
            String result = pythonAiClient.testConnection(message);

            // then
            assertThat(result).isNotNull();
            assertThat(result).contains("Python 응답 (Mock)");
        }

        @Test
        @DisplayName("특수문자 포함 메시지 처리")
        void testConnection_withSpecialCharacters() {
            // given
            String message = "테스트!@#$%^&*()";

            // when
            String result = pythonAiClient.testConnection(message);

            // then
            assertThat(result).isNotNull();
            assertThat(result).contains(message);
        }
    }
}
