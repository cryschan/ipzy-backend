# Swagger API 문서화 가이드

## 개요

이 프로젝트는 OpenAPI 3.0 (Swagger)을 사용하여 API 문서를 자동 생성합니다.
모든 Controller는 아래 규칙에 따라 문서화해야 합니다.

---

## 1. 기본 구조

### 1.1 Controller 레벨 - `@Tag`

```java
@Tag(name = "Recommendation", description = "AI 코디 추천 API")
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    // ...
}
```

### 1.2 메서드 레벨 - `@Operation`

```java
@Operation(
        summary = "코디 추천 생성",           // 한 줄 요약
        description = """
                완료된 퀴즈 세션 기반으로 AI 코디 추천을 생성합니다.

                **인증:** 필수 (로그인 필요)

                **동작 흐름:**
                1. 퀴즈 세션 완료 여부 검증
                2. Python AI 서비스에 추천 요청
                3. 추천 결과 저장 및 반환
                """)                          // 상세 설명 (마크다운 지원)
```

---

## 2. 응답 문서화 - 성공/실패 분리

### 2.1 핵심 원칙

- **성공 응답**과 **에러 응답**을 `@ApiResponse`로 명확히 분리
- 각 응답에 `@ExampleObject`로 실제 JSON 예시 제공
- 에러 코드를 description이 아닌 개별 `@ApiResponse`로 표현

### 2.2 성공 응답 예시

```java
@io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "추천 생성 성공",
        content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                        {
                          "success": true,
                          "data": {
                            "recommendationId": 1,
                            "occasion": "데이트",
                            "style": "캐주얼"
                          }
                        }
                        """)
        )
)
```

### 2.3 에러 응답 예시

```java
@io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "퀴즈 미완료",
        content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                        {
                          "success": false,
                          "error": {
                            "code": "REC302",
                            "message": "퀴즈가 완료되지 않았습니다"
                          }
                        }
                        """)
        )
)
```

---

## 3. 프로젝트 응답 형식

### 3.1 ApiResponse 래퍼 클래스

> **중요**: 프로젝트 응답 래퍼와 Swagger 애노테이션의 이름 충돌 주의

| 클래스 | 용도 | import |
|--------|------|--------|
| `com.ipzy._global.common.ApiResponse` | **프로젝트 응답 래퍼** (Controller 반환 타입) | 사용 가능 |
| `io.swagger.v3.oas.annotations.responses.ApiResponse` | **Swagger 문서화** | **import 금지, FQCN 사용** |

```java
// 올바른 사용 예시
import com.ipzy._global.common.ApiResponse;  // 프로젝트 래퍼 - OK

// Swagger는 항상 FQCN으로 사용
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", ...)
public ApiResponse<RecommendationResponse> generate(...) { ... }
```

### 3.2 응답 JSON 구조

```java
// 성공 응답
{
  "success": true,
  "data": { ... }    // 실제 데이터
}

// 에러 응답
{
  "success": false,
  "error": {
    "code": "에러코드",
    "message": "에러 메시지"
  }
}
```

### 3.3 HTTP 상태 코드 매핑

| HTTP | 용도 | 에러 코드 예시 |
|------|------|---------------|
| 200 | 성공 | - |
| 400 | 잘못된 요청 | REC302 (퀴즈 미완료) |
| 401 | 인증 필요 | AUTH_001 |
| 403 | 권한 없음 | REC401 |
| 404 | 리소스 없음 | REC301 |
| 409 | 충돌 | REC303 (이미 존재) |
| 503 | 서비스 불가 | REC101 (AI 연결 실패) |
| 504 | 타임아웃 | REC102 |

---

## 4. 전체 예시

```java
@Operation(
        summary = "코디 추천 생성",
        description = """
                완료된 퀴즈 세션 기반으로 AI 코디 추천을 생성합니다.

                **인증:** 필수 (로그인 필요)
                """)
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "추천 생성 성공",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "data": [
                                    {
                                      "recommendationId": 1,
                                      "occasion": "데이트",
                                      "style": "캐주얼",
                                      "totalPrice": 237000
                                    }
                                  ]
                                }
                                """)
                )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "퀴즈 미완료",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "error": {
                                    "code": "REC302",
                                    "message": "퀴즈가 완료되지 않았습니다"
                                  }
                                }
                                """)
                )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "error": {
                                    "code": "AUTH_001",
                                    "message": "인증이 필요합니다"
                                  }
                                }
                                """)
                )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "세션 없음",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "error": {
                                    "code": "REC301",
                                    "message": "퀴즈 세션을 찾을 수 없습니다"
                                  }
                                }
                                """)
                )
        )
})
@PostMapping("/sessions/{sessionId}/generate")
public ApiResponse<List<RecommendationSummaryResponse>> generateRecommendation(
        @Parameter(description = "완료된 퀴즈 세션 ID") @PathVariable Long sessionId,
        @AuthenticationPrincipal CustomUserPrincipal principal) {
    // ...
}
```

---

## 5. 파라미터 문서화

### 5.1 Path Variable

```java
@Parameter(description = "퀴즈 세션 ID") @PathVariable Long sessionId
```

### 5.2 Query Parameter

```java
@Parameter(description = "테스트 메시지", example = "안녕하세요") @RequestParam String msg
```

### 5.3 Request Body

```java
@io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "추천 요청 데이터",
        required = true,
        content = @Content(schema = @Schema(implementation = RecommendationRequest.class))
)
@RequestBody RecommendationRequest request
```

---

## 6. 필수 import

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

// 주의: 프로젝트에 ApiResponse 응답 래퍼가 있으면 이름 충돌 발생
// Swagger 애노테이션은 FQCN 사용 권장:
// @io.swagger.v3.oas.annotations.responses.ApiResponse
```

---

## 7. 체크리스트

API 문서화 시 확인사항:

- [ ] `@Tag`로 Controller 설명 추가
- [ ] `@Operation`의 summary와 description 작성
- [ ] 성공 응답(200)에 `@ExampleObject` 추가
- [ ] 모든 에러 케이스를 개별 `@ApiResponse`로 분리
- [ ] 각 에러 응답에 실제 에러 JSON 예시 추가
- [ ] Path/Query 파라미터에 `@Parameter` 추가
- [ ] ExampleObject의 JSON이 실제 DTO 구조와 일치하는지 확인

---

## 8. Swagger UI 접속

- 로컬: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/v3/api-docs
