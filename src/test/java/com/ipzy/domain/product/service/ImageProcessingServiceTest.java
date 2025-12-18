package com.ipzy.domain.product.service;

import com.ipzy.domain.product.dto.RemoveBackgroundRequest;
import com.ipzy.domain.product.dto.RemoveBackgroundResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * ImageProcessingService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class ImageProcessingServiceTest {

    @Mock
    private RestClient pythonAiRestClient;

    @InjectMocks
    private ImageProcessingService imageProcessingService;

    @Nested
    @DisplayName("removeBackgroundBatch 메서드는")
    class RemoveBackgroundBatch {

        @Test
        @DisplayName("이미지 URL 리스트를 전달하면 누끼 제거된 URL 맵을 반환한다")
        void success_whenValidUrls() {
            // given
            List<String> imageUrls = List.of(
                    "https://example.com/image1.jpg",
                    "https://example.com/image2.jpg"
            );

            RemoveBackgroundResponse.ImageResult result1 = RemoveBackgroundResponse.ImageResult.builder()
                    .originalUrl("https://example.com/image1.jpg")
                    .removedBackgroundUrl("https://s3.amazonaws.com/nobg1.jpg")
                    .success(true)
                    .build();

            RemoveBackgroundResponse.ImageResult result2 = RemoveBackgroundResponse.ImageResult.builder()
                    .originalUrl("https://example.com/image2.jpg")
                    .removedBackgroundUrl("https://s3.amazonaws.com/nobg2.jpg")
                    .success(true)
                    .build();

            RemoveBackgroundResponse response = RemoveBackgroundResponse.builder()
                    .results(List.of(result1, result2))
                    .build();

            RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            given(pythonAiRestClient.post()).willReturn(requestBodyUriSpec);
            given(requestBodyUriSpec.uri("/api/image/remove-background/batch")).willReturn(requestBodySpec);
            given(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).willReturn(requestBodySpec);
            given(requestBodySpec.body(any(RemoveBackgroundRequest.class))).willReturn(requestBodySpec);
            given(requestBodySpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.body(RemoveBackgroundResponse.class)).willReturn(response);

            // when
            Map<String, String> result = imageProcessingService.removeBackgroundBatch(imageUrls);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get("https://example.com/image1.jpg")).isEqualTo("https://s3.amazonaws.com/nobg1.jpg");
            assertThat(result.get("https://example.com/image2.jpg")).isEqualTo("https://s3.amazonaws.com/nobg2.jpg");
        }

        @Test
        @DisplayName("빈 리스트를 전달하면 빈 맵을 반환한다")
        void success_whenEmptyList() {
            // given
            List<String> imageUrls = List.of();

            // when
            Map<String, String> result = imageProcessingService.removeBackgroundBatch(imageUrls);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("null 리스트를 전달하면 빈 맵을 반환한다")
        void success_whenNullList() {
            // when
            Map<String, String> result = imageProcessingService.removeBackgroundBatch(null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("일부 이미지 처리가 실패해도 성공한 이미지만 반환한다")
        void success_whenPartialFailure() {
            // given
            List<String> imageUrls = List.of(
                    "https://example.com/image1.jpg",
                    "https://example.com/image2.jpg"
            );

            RemoveBackgroundResponse.ImageResult result1 = RemoveBackgroundResponse.ImageResult.builder()
                    .originalUrl("https://example.com/image1.jpg")
                    .removedBackgroundUrl("https://s3.amazonaws.com/nobg1.jpg")
                    .success(true)
                    .build();

            RemoveBackgroundResponse.ImageResult result2 = RemoveBackgroundResponse.ImageResult.builder()
                    .originalUrl("https://example.com/image2.jpg")
                    .removedBackgroundUrl(null)
                    .success(false)
                    .error("Processing failed")
                    .build();

            RemoveBackgroundResponse response = RemoveBackgroundResponse.builder()
                    .results(List.of(result1, result2))
                    .build();

            RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            given(pythonAiRestClient.post()).willReturn(requestBodyUriSpec);
            given(requestBodyUriSpec.uri("/api/image/remove-background/batch")).willReturn(requestBodySpec);
            given(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).willReturn(requestBodySpec);
            given(requestBodySpec.body(any(RemoveBackgroundRequest.class))).willReturn(requestBodySpec);
            given(requestBodySpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.body(RemoveBackgroundResponse.class)).willReturn(response);

            // when
            Map<String, String> result = imageProcessingService.removeBackgroundBatch(imageUrls);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get("https://example.com/image1.jpg")).isEqualTo("https://s3.amazonaws.com/nobg1.jpg");
            assertThat(result.get("https://example.com/image2.jpg")).isNull();
        }

        @Test
        @DisplayName("20개 이상의 이미지는 청크로 나눠서 처리한다")
        void success_whenLargeList() {
            // given - 25개 이미지 (20 + 5로 분할될 것)
            List<String> imageUrls = List.of(
                    "https://example.com/image1.jpg",
                    "https://example.com/image2.jpg",
                    "https://example.com/image3.jpg",
                    "https://example.com/image4.jpg",
                    "https://example.com/image5.jpg",
                    "https://example.com/image6.jpg",
                    "https://example.com/image7.jpg",
                    "https://example.com/image8.jpg",
                    "https://example.com/image9.jpg",
                    "https://example.com/image10.jpg",
                    "https://example.com/image11.jpg",
                    "https://example.com/image12.jpg",
                    "https://example.com/image13.jpg",
                    "https://example.com/image14.jpg",
                    "https://example.com/image15.jpg",
                    "https://example.com/image16.jpg",
                    "https://example.com/image17.jpg",
                    "https://example.com/image18.jpg",
                    "https://example.com/image19.jpg",
                    "https://example.com/image20.jpg",
                    "https://example.com/image21.jpg",
                    "https://example.com/image22.jpg",
                    "https://example.com/image23.jpg",
                    "https://example.com/image24.jpg",
                    "https://example.com/image25.jpg"
            );

            RemoveBackgroundResponse response1 = createMockResponseWithUrls(imageUrls.subList(0, 20));
            RemoveBackgroundResponse response2 = createMockResponseWithUrls(imageUrls.subList(20, 25));

            RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            given(pythonAiRestClient.post()).willReturn(requestBodyUriSpec);
            given(requestBodyUriSpec.uri("/api/image/remove-background/batch")).willReturn(requestBodySpec);
            given(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).willReturn(requestBodySpec);
            given(requestBodySpec.body(any(RemoveBackgroundRequest.class))).willReturn(requestBodySpec);
            given(requestBodySpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.body(RemoveBackgroundResponse.class))
                    .willReturn(response1, response2);

            // when
            Map<String, String> result = imageProcessingService.removeBackgroundBatch(imageUrls);

            // then
            assertThat(result).hasSize(25);
        }

        private RemoveBackgroundResponse createMockResponseWithUrls(List<String> originalUrls) {
            List<RemoveBackgroundResponse.ImageResult> results = new java.util.ArrayList<>(originalUrls.size());
            for (String originalUrl : originalUrls) {
                results.add(RemoveBackgroundResponse.ImageResult.builder()
                        .originalUrl(originalUrl)
                        .removedBackgroundUrl(originalUrl.replace("https://example.com/", "https://s3.amazonaws.com/nobg/"))
                        .success(true)
                        .build());
            }
            return RemoveBackgroundResponse.builder().results(results).build();
        }
    }
}
