# 작업 계획: 계정 탈퇴 서비스 구현

## 1. 작업 요약

| 항목 | 내용 |
|------|------|
| 핵심 목표 | 계정 탈퇴 시 OAuth 연결 끊기 + 연관 데이터 처리 + 개인정보 마스킹 |
| 구현 방식 | WithdrawalService 별도 생성 (책임 분리) |
| 상태 | 🔲 대기 중 |

---

## 2. 설계 결정

### 기존 vs 신규 비교

| 항목 | 기존 (UserService) | 신규 (WithdrawalService) |
|------|-------------------|-------------------------|
| 위치 | `UserService.deleteAccount()` | `WithdrawalService.withdraw()` |
| 책임 | User 상태만 변경 | 전체 탈퇴 프로세스 관리 |
| OAuth 연동 해제 | ❌ | ✅ |
| 연관 데이터 처리 | ❌ | ✅ |
| 개인정보 마스킹 | ❌ | ✅ |
| 세션 무효화 | ❌ | ✅ |

### 선택 이유

- **단일 책임 원칙**: 탈퇴는 복잡한 비즈니스 로직 → 별도 서비스로 분리
- **테스트 용이성**: 탈퇴 로직만 독립적으로 테스트 가능
- **확장성**: 유예 기간, 탈퇴 사유 저장 등 기능 추가 용이

---

## 3. 아키텍처

### 탈퇴 흐름도

```text
┌──────────┐     ┌──────────────────┐     ┌───────────────────┐
│ Frontend │────>│ UserController   │────>│ WithdrawalService │
│          │     │ DELETE /users/me │     │                   │
└──────────┘     └──────────────────┘     └───────────────────┘
                                                    │
                 ┌──────────────────────────────────┴──────────────────────────────────┐
                 │                                                                      │
                 ▼                                                                      │
    ┌────────────────────────┐                                                         │
    │ Step 1: 탈퇴 가능 확인  │                                                         │
    │  - 활성 구독 여부       │                                                         │
    │  - 미완료 결제 여부     │                                                         │
    │  - 관리자 계정 여부     │                                                         │
    └───────────┬────────────┘                                                         │
                │                                                                      │
                ▼                                                                      │
    ┌────────────────────────┐     ┌─────────────────────┐     ┌─────────────┐        │
    │ Step 2: OAuth 연결 끊기 │────>│ OAuth2Unlink        │────>│ Kakao API   │        │
    │                        │     │ StrategyFactory     │     │ /v1/unlink  │        │
    └───────────┬────────────┘     └─────────────────────┘     └─────────────┘        │
                │                                                                      │
                ▼                                                                      │
    ┌────────────────────────┐                                                         │
    │ Step 3: 연관 데이터 처리│                                                         │
    │  - SavedOutfit 삭제    │                                                         │
    │  - ActivityLog 삭제    │                                                         │
    │  - QuizSession FK null │                                                         │
    │  - Recommendation null │                                                         │
    └───────────┬────────────┘                                                         │
                │                                                                      │
                ▼                                                                      │
    ┌────────────────────────┐                                                         │
    │ Step 4: User 탈퇴 처리  │                                                         │
    │  - status = DELETED    │                                                         │
    │  - deletedAt = now()   │                                                         │
    │  - 개인정보 마스킹      │                                                         │
    └───────────┬────────────┘                                                         │
                │                                                                      │
                ▼                                                                      │
    ┌────────────────────────┐                                                         │
    │ Step 5: 세션 무효화     │                                                         │
    │  - SecurityContext 클리어                                                        │
    │  - HttpSession 무효화  │                                                         │
    └───────────┬────────────┘                                                         │
                │                                                                      │
                ▼                                                                      │
    ┌────────────────────────┐                                                         │
    │ 204 No Content 응답    │◀────────────────────────────────────────────────────────┘
    └────────────────────────┘
```

### 클래스 구조

```text
domain/user/
├── controller/
│   └── UserController.java           # DELETE /api/users/me 엔드포인트
├── service/
│   ├── UserService.java              # 기존 (프로필 CRUD)
│   └── WithdrawalService.java        # 신규 (탈퇴 전담)
├── dto/
│   └── WithdrawalRequest.java        # 탈퇴 요청 DTO (선택)
└── exception/
    ├── UserErrorCode.java            # 탈퇴 관련 에러 코드 추가
    └── UserException.java

domain/auth/
├── unlink/                           # ✅ 구현 완료
│   ├── OAuth2UnlinkStrategy.java
│   ├── OAuth2UnlinkStrategyFactory.java
│   └── strategy/
│       └── KakaoUnlinkStrategy.java
```

---

## 4. 구현 상세

### 4.1 WithdrawalService

```java
@Service
@RequiredArgsConstructor
@Transactional
public class WithdrawalService {

    private final UserRepository userRepository;
    private final OAuth2UnlinkStrategyFactory unlinkStrategyFactory;

    // 연관 데이터 처리용 Repository (필요 시)
    // private final SavedOutfitRepository savedOutfitRepository;
    // private final ActivityLogRepository activityLogRepository;

    /**
     * 계정 탈퇴를 수행합니다.
     *
     * @param userId      탈퇴할 사용자 ID
     * @param provider    OAuth Provider (KAKAO, NAVER, GOOGLE)
     * @param accessToken OAuth access token (세션에서 조회)
     */
    public void withdraw(Long userId, String provider, String accessToken) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.notFound(userId));

        // Step 1: 탈퇴 가능 여부 확인
        validateWithdrawal(user);

        // Step 2: OAuth 연결 끊기
        unlinkOAuthProvider(provider, accessToken);

        // Step 3: 연관 데이터 처리
        processRelatedData(userId);

        // Step 4: User 탈퇴 처리 (상태 변경 + 개인정보 마스킹)
        user.withdraw();
    }

    private void validateWithdrawal(User user) {
        // 이미 탈퇴한 계정인지 확인
        if (user.getStatus() == UserStatus.DELETED) {
            throw UserException.alreadyWithdrawn();
        }

        // 관리자 계정인지 확인
        if (user.getRole() == UserRole.ADMIN) {
            throw UserException.adminCannotWithdraw();
        }

        // TODO: 활성 구독 확인
        // TODO: 미완료 결제 확인
    }

    private void unlinkOAuthProvider(String provider, String accessToken) {
        if (provider == null || accessToken == null) {
            log.warn("OAuth 정보가 없어 연결 끊기를 건너뜁니다");
            return;
        }

        boolean success = unlinkStrategyFactory.unlink(provider, accessToken);

        if (!success) {
            log.warn("OAuth 연결 끊기 실패 - provider: {}", provider);
            // 실패해도 탈퇴는 계속 진행
        }
    }

    private void processRelatedData(Long userId) {
        // TODO: 연관 데이터 처리 (Phase 2)
        // savedOutfitRepository.deleteByUserId(userId);
        // activityLogRepository.deleteByUserId(userId);
        // quizSessionRepository.nullifyUserId(userId);
        // recommendationRepository.nullifyUserId(userId);
    }
}
```

### 4.2 User.withdraw() 메서드

```java
// User.java에 추가
public void withdraw() {
    this.status = UserStatus.DELETED;
    this.deletedAt = LocalDateTime.now();

    // 개인정보 마스킹
    this.email = "deleted_" + this.id + "_" + System.currentTimeMillis() + "@ipzy.com";
    this.name = "탈퇴한 사용자";
    this.phone = null;
    this.profileImageUrl = null;
    this.providerId = "deleted_" + this.id;
    this.preferences = null;
    this.stylePreference = null;
}
```

### 4.3 UserController 엔드포인트

```java
@DeleteMapping("/me")
@Operation(summary = "계정 탈퇴", description = "현재 로그인한 사용자의 계정을 탈퇴합니다")
@ApiResponse(responseCode = "204", description = "탈퇴 성공")
public ResponseEntity<Void> withdraw(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        HttpServletRequest request) {

    // 세션에서 OAuth 정보 조회
    HttpSession session = request.getSession(false);
    String provider = session != null
            ? (String) session.getAttribute("OAUTH2_PROVIDER")
            : null;
    String accessToken = session != null
            ? (String) session.getAttribute(OAuth2TokenSessionKey.getSessionKey(provider))
            : null;

    // 탈퇴 수행
    withdrawalService.withdraw(principal.getUserId(), provider, accessToken);

    // 세션 무효화
    if (session != null) {
        session.invalidate();
    }
    SecurityContextHolder.clearContext();

    return ResponseEntity.noContent().build();
}
```

### 4.4 UserErrorCode 추가

```java
// UserErrorCode.java에 추가
ALREADY_WITHDRAWN(400, "U003", "이미 탈퇴한 계정입니다"),
ADMIN_CANNOT_WITHDRAW(400, "U004", "관리자는 일반 탈퇴할 수 없습니다"),
ACTIVE_SUBSCRIPTION_EXISTS(400, "U005", "활성 구독이 있어 탈퇴할 수 없습니다"),
PENDING_PAYMENT_EXISTS(400, "U006", "처리 중인 결제가 있어 탈퇴할 수 없습니다");
```

---

## 5. 의존성 관계

```text
┌─────────────────────────────────────────────────────────────────┐
│                      UserController                              │
│                    DELETE /api/users/me                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     WithdrawalService                            │
│                                                                  │
│  ┌─────────────────┐  ┌─────────────────────────────────────┐   │
│  │ UserRepository  │  │ OAuth2UnlinkStrategyFactory         │   │
│  │                 │  │   └─ KakaoUnlinkStrategy (✅ 완료)   │   │
│  └─────────────────┘  └─────────────────────────────────────┘   │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ (Phase 2) 연관 Repository                                │    │
│  │  - SavedOutfitRepository                                 │    │
│  │  - ActivityLogRepository                                 │    │
│  │  - QuizSessionRepository                                 │    │
│  │  - RecommendationRepository                              │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. 구현 체크리스트

### Phase 1: MVP (OAuth 연결 끊기 + 기본 탈퇴)

| 순서 | 작업 | 파일 | 상태 |
|:----:|------|------|:----:|
| 1 | OAuth2UnlinkStrategy 인터페이스 | `auth/unlink/OAuth2UnlinkStrategy.java` | ✅ |
| 2 | KakaoUnlinkStrategy 구현 | `auth/unlink/strategy/KakaoUnlinkStrategy.java` | ✅ |
| 3 | OAuth2UnlinkStrategyFactory | `auth/unlink/OAuth2UnlinkStrategyFactory.java` | ✅ |
| 4 | UserErrorCode 추가 | `user/exception/UserErrorCode.java` | 🔲 |
| 5 | User.withdraw() 메서드 | `user/entity/User.java` | 🔲 |
| 6 | WithdrawalService 생성 | `user/service/WithdrawalService.java` | 🔲 |
| 7 | UserController 엔드포인트 | `user/controller/UserController.java` | 🔲 |
| 8 | WithdrawalServiceTest | `user/service/WithdrawalServiceTest.java` | 🔲 |

### Phase 2: 연관 데이터 처리 (선택)

| 순서 | 작업 | 파일 | 상태 |
|:----:|------|------|:----:|
| 1 | SavedOutfit 삭제 쿼리 | `outfit/repository/SavedOutfitRepository.java` | 🔲 |
| 2 | ActivityLog 삭제 쿼리 | `activity/repository/ActivityLogRepository.java` | 🔲 |
| 3 | QuizSession FK null 처리 | `quiz/repository/QuizSessionRepository.java` | 🔲 |
| 4 | Recommendation FK null 처리 | `recommendation/repository/RecommendationRepository.java` | 🔲 |

### Phase 3: 고급 기능 (선택)

| 순서 | 작업 | 설명 | 상태 |
|:----:|------|------|:----:|
| 1 | 유예 기간 | 30일 후 완전 삭제 스케줄러 | 🔲 |
| 2 | 탈퇴 사유 저장 | WithdrawalRequest DTO + 저장 | 🔲 |
| 3 | 재가입 제한 | providerId 해시 보관 | 🔲 |

---

## 7. API 명세

### DELETE /api/users/me

```http
DELETE /api/users/me
Authorization: Required (Session)

Request Body: (선택)
{
    "reason": "서비스 불만족",
    "feedback": "추가 의견"
}

Response:
- 204 No Content: 탈퇴 성공
- 400 Bad Request: 탈퇴 불가 (활성 구독, 미완료 결제 등)
- 401 Unauthorized: 미인증
```

---

## 8. 테스트 시나리오

```java
@Test
void 정상_탈퇴_성공() { ... }

@Test
void 이미_탈퇴한_계정은_예외() { ... }

@Test
void 관리자_계정은_탈퇴_불가() { ... }

@Test
void OAuth_연결_끊기_실패해도_탈퇴는_진행() { ... }

@Test
void 탈퇴_후_개인정보_마스킹_확인() { ... }
```

---

## 9. 다음 단계

Phase 1 구현 승인 시:
1. UserErrorCode 추가
2. User.withdraw() 메서드 추가
3. WithdrawalService 생성
4. UserController 엔드포인트 추가
5. 테스트 작성 및 검증
