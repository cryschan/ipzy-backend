# TODO: 미사용 AuthException 메서드 처리

## 현황

다음 메서드들이 현재 코드베이스에서 **사용되지 않음**:

```java
// AuthException.java
public static AuthException oauthFailed(String reason)
public static AuthException oauthInvalidResponse(String reason)
```

## 선택지

### 옵션 A: 삭제

**장점:**
- 코드 간결화
- 미사용 코드 제거로 유지보수성 향상

**단점:**
- 향후 필요 시 다시 작성해야 함

**작업:**
```java
// 삭제 대상 (AuthException.java:26-28, 54-56)
public static AuthException oauthFailed(String reason) { ... }
public static AuthException oauthInvalidResponse(String reason) { ... }
```

---

### 옵션 B: 유지

**장점:**
- 향후 OAuth 상세 에러 처리 시 즉시 사용 가능
- 확장성 유지

**단점:**
- 미사용 코드 존재

**예상 사용 케이스:**
- OAuth 토큰 만료: `oauthFailed("token expired")`
- OAuth 응답 파싱 실패: `oauthInvalidResponse("missing email field")`

---

## 결정

- [ ] 옵션 A: 삭제
- [ ] 옵션 B: 유지

## 관련 파일

- `src/main/java/com/ipzy/domain/auth/exception/AuthException.java`
