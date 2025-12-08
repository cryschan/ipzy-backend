package com.ipzy.domain.recommendation.controller;

import com.ipzy.global.common.ApiResponse;
import com.ipzy.domain.recommendation.client.PythonAiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 추천 API 컨트롤러
 */
@Tag(name = "Recommendation", description = "AI 코디 추천 API")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final PythonAiClient pythonAiClient;

    @Operation(
            summary = "Python 통신 테스트",
            description = """
                    Python AI 서비스와의 통신을 테스트합니다.

                    **사용 예시:**
                    ```
                    GET /api/recommendations/test?msg=안녕하세요
                    ```

                    **참고:** Python API 준비 전까지 Mock 응답을 반환합니다.
                    """)
    @GetMapping("/test")
    public ApiResponse<String> testConnection(@RequestParam String msg) {
        String result = pythonAiClient.testConnection(msg);
        return ApiResponse.success(result);
    }
}
