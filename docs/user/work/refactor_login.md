# 작업 완료: 로그아웃/로그인 흐름 개선

## 1. 작업 요약

| 항목 | 내용 |
|------|------|
| 핵심 목표 | 로그아웃 버튼 한 번으로 카카오 로그아웃까지 자동 완료 |
| 구현 방식 | 서버에서 카카오 REST API 직접 호출 후 메인페이지 리다이렉트 |
| 상태 | ✅ 완료 |

---

## 2. 변경 전/후 비교

### 로그아웃 흐름
```
변경 전: 로그아웃 → JSON 응답 {"success":true} (카카오 세션 유지됨)
변경 후: 로그아웃 → 카카오 API 호출 → 메인페이지 리다이렉트
```

### 상세 비교
| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 로그아웃 응답 | `200 JSON` | `302 리다이렉트` (메인페이지) |
| 카카오 로그아웃 | ❌ 미처리 | ✅ REST API 호출 |
| 로그인 성공 | `/auth/callback` 이동 | `/` (메인화면) 이동 |
| 비로그인 로그아웃 | `permitAll()` 허용 | 메인페이지 리다이렉트 |
| 로그아웃 핸들러 | SecurityConfig 인라인 | 별도 클래스 분리 |

---

## 3. 구현된 아키텍처

### 로그아웃 흐름도
```
┌──────────┐     ┌──────────────────┐     ┌─────────────────────┐     ┌────────┐
│ Frontend │────>│ SecurityConfig   │────>│ OAuth2Logout        │────>│ Kakao  │
│          │     │ /api/auth/logout │     │ SuccessHandler      │     │ Server │
└──────────┘     └──────────────────┘     └─────────────────────┘     └────────┘
                         │                          │                      │
                         │ 세션 무효화               │ Provider 조회         │
                         │ 쿠키 삭제                │ Token 조회            │
                         │                          │                      │
                         │                 ┌────────▼────────┐            │
                         │                 │ OAuth2Logout    │            │
                         │                 │ StrategyFactory │            │
                         │                 └────────┬────────┘            │
                         │                          │                      │
                         │                 ┌────────▼────────┐            │
                         │                 │ KakaoLogout     │───────────>│
                         │                 │ Strategy        │  API 호출   │
                         │                 └─────────────────┘            │
                         │                                                 │
                         │<────────────────── 302 리다이렉트 ───────────────│
```

### 클래스 구조
```
auth/
├── handler/
│   ├── OAuth2SuccessHandler.java      # 로그인 성공 시 토큰 세션 저장
│   ├── OAuth2FailureHandler.java      # 로그인 실패 처리
│   └── OAuth2LogoutSuccessHandler.java # 로그아웃 처리 (Strategy 호출)
├── logout/
│   ├── OAuth2LogoutStrategy.java       # 로그아웃 전략 인터페이스
│   ├── OAuth2LogoutStrategyFactory.java # Provider별 전략 조회
│   ├── OAuth2TokenSessionKey.java      # 세션 키 관리
│   └── strategy/
│       └── KakaoLogoutStrategy.java    # 카카오 로그아웃 API 호출
```

---

## 4. 구현 상세

### 4.1 OAuth2SuccessHandler - 로그인 성공 시 토큰 저장

```java
@Override
public void onAuthenticationSuccess(...) {
    // access_token 세션 저장 (로그아웃 시 사용)
    saveAccessTokenToSession(request, authentication);

    // 메인페이지로 리다이렉트
    response.sendRedirect(successRedirectUri);
}
```

### 4.2 OAuth2LogoutSuccessHandler - 로그아웃 처리

```java
@Override
public void onLogoutSuccess(...) {
    if (authentication == null) {
        // 비로그인 상태 → 메인으로 리다이렉트
        response.sendRedirect(logoutRedirectUri);
        return;
    }

    // Provider별 토큰 revoke (카카오 API 호출)
    revokeProviderToken(request);

    // 메인페이지로 리다이렉트
    response.sendRedirect(logoutRedirectUri);
}
```

### 4.3 KakaoLogoutStrategy - 카카오 API 호출

```java
@Override
public void revokeToken(String accessToken) {
    // POST https://kapi.kakao.com/v1/user/logout
    // Authorization: Bearer {accessToken}
    restTemplate.exchange(LOGOUT_URL, HttpMethod.POST, ...);
}
```

### 4.4 SecurityConfig - 로그아웃 설정

```java
.logout(logout -> logout
    .logoutUrl("/api/auth/logout")
    .invalidateHttpSession(true)
    .deleteCookies("JSESSIONID")
    .logoutSuccessHandler(oAuth2LogoutSuccessHandler)
)
```

---

## 5. 설정 (application.yml)

```yaml
app:
  oauth2:
    success-redirect-uri: ${FRONTEND_URL:http://localhost:5173}
    failure-redirect-uri: ${FRONTEND_URL:http://localhost:5173}/auth/callback?error=true
    logout-redirect-uri: ${FRONTEND_URL:http://localhost:5173}
```

---

## 6. 테스트 결과

- [x] 로그인 성공 → `/` 리다이렉트 확인
- [x] 로그아웃 성공 → 카카오 API 호출 + 메인페이지 리다이렉트 확인
- [x] 비로그인 로그아웃 → 메인페이지 리다이렉트 확인
- [x] 전체 테스트 통과 (`./gradlew test`)

---

## 7. 향후 확장

네이버/구글 로그인 추가 시:
1. `NaverLogoutStrategy`, `GoogleLogoutStrategy` 구현
2. `@Component`로 등록하면 `OAuth2LogoutStrategyFactory`가 자동 수집
3. 추가 코드 변경 없이 확장 가능 (Strategy 패턴)
