# Phase 3: User 생성 시 Subscription 생성 개선

## 🎯 목표
- User 생성과 Subscription 생성을 한 곳에서 처리
- 코드 가독성 및 유지보수성 향상
- TODO 주석 해결

---

## 📝 작업 체크리스트

- [ ] CustomOAuth2UserService 리팩토링
- [ ] 사용자 조회/생성 로직 메서드 분리
- [ ] 신규 사용자 + 구독 생성 메서드 추가
- [ ] SubscriptionService에 createDefaultSubscription 메서드 추가
- [ ] 테스트 작성

---

## 🔧 구현 코드

### 1. SubscriptionService에 메서드 추가

```java
package com.ipzy.domain.subscription.service;

// ... imports

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class SubscriptionService {

    // ... 기존 필드

    /**
     * 기본 구독 생성 (체크 없이 무조건 생성)
     * User 생성 시점에 호출됨
     *
     * @param userId 사용자 ID
     * @return 생성된 FREE 구독
     */
    @Transactional
    public Subscription createDefaultSubscription(Long userId) {
        User user = findUserById(userId);
        return startFreeSubscription(user);
    }

    // ... 기존 메서드들
}
```

---

### 2. CustomOAuth2UserService 리팩토링

```java
package com.ipzy.domain.auth.service;

// ... imports

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final OAuth2UserInfoFactory oAuth2UserInfoFactory;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = fetchOAuth2User(request);

        String registrationId = request.getClientRegistration().getRegistrationId();
        String provider = registrationId.toUpperCase();

        OAuth2UserInfo userInfo = oAuth2UserInfoFactory.create(
                registrationId,
                oauth2User.getAttributes()
        );

        validateUserInfo(userInfo, provider);

        String providerId = userInfo.getProviderId();
        String email = userInfo.getEmail();
        String name = userInfo.getName();
        String profileImage = userInfo.getProfileImageUrl();

        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
            log.info("{} 사용자 이름이 없어 이메일로 대체: {}", provider, name);
        }

        // ⭐ 사용자 조회 또는 생성 (메서드 분리)
        User user = getOrCreateUser(provider, providerId, email, name, profileImage);

        user.updateLastLoginAt();

        return CustomUserPrincipal.from(user, oauth2User.getAttributes());
    }

    /**
     * 사용자 조회 또는 생성
     *
     * @return 사용자 엔티티
     */
    private User getOrCreateUser(String provider, String providerId,
                                 String email, String name, String profileImage) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .or(() -> userRepository.findByEmail(email))
                .map(existingUser -> updateExistingUser(existingUser, provider, providerId, name, profileImage))
                .orElseGet(() -> createNewUserWithSubscription(provider, providerId, email, name, profileImage));
    }

    /**
     * 기존 사용자 업데이트
     */
    private User updateExistingUser(User existingUser, String provider, String providerId,
                                    String name, String profileImage) {
        // 같은 이메일이지만 다른 Provider → 소셜 연동
        if (!existingUser.getProvider().equals(provider)) {
            existingUser.linkSocialAccount(provider, providerId);
            log.info("기존 계정에 {} 소셜 연동: userId={}", provider, existingUser.getId());
        }

        existingUser.updateOAuthInfo(name, profileImage);
        return existingUser;
    }

    /**
     * ⭐ 신규 사용자 + 구독 생성
     *
     * User 생성 직후 FREE 플랜 구독을 자동으로 생성합니다.
     */
    private User createNewUserWithSubscription(String provider, String providerId,
                                               String email, String name, String profileImage) {
        // 1. User 생성
        User newUser = userRepository.save(User.builder()
                .email(email)
                .name(name)
                .profileImageUrl(profileImage)
                .provider(provider)
                .providerId(providerId)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

        log.info("신규 사용자 생성: userId={}, provider={}, email={}",
                newUser.getId(), provider, email);

        // 2. ⭐ FREE 구독 생성
        subscriptionService.createDefaultSubscription(newUser.getId());

        log.info("신규 사용자 구독 생성 완료: userId={}", newUser.getId());

        return newUser;
    }

    // ... 기타 메서드 (validateUserInfo, fetchOAuth2User 등)
}
```

---

## 📊 Before & After 비교

### Before (기존)

```java
// loadUser 메서드 안에 모든 로직이 섞여있음
User user = userRepository.findByProviderAndProviderId(...)
        .or(() -> userRepository.findByEmail(email))
        .map(existingUser -> {
            // 기존 사용자 처리 (10줄)
        })
        .orElseGet(() -> {
            // 신규 사용자 생성 (5줄)
        });

user.updateLastLoginAt();
subscriptionService.ensureDefaultSubscription(user.getId());  // ⚠️ 신규/기존 구분 없이 항상 호출
```

**문제점:**
- 신규/기존 사용자 구분 불명확
- `ensureDefaultSubscription`이 모든 경우에 호출됨
- 가독성 낮음
- 테스트 어려움

---

### After (개선)

```java
// 명확한 메서드 분리
User user = getOrCreateUser(provider, providerId, email, name, profileImage);
user.updateLastLoginAt();
// ✅ Subscription은 신규 사용자 생성 시에만 createNewUserWithSubscription에서 생성
```

**개선점:**
- ✅ 책임 분리 (SRP)
- ✅ 신규 사용자만 구독 생성
- ✅ 가독성 향상
- ✅ 테스트 용이
- ✅ TODO 주석 해결

---

## ✅ 테스트 시나리오

### 1. 신규 사용자 생성 테스트

```java
@SpringBootTest
class CustomOAuth2UserServiceTest {

    @Autowired
    private CustomOAuth2UserService oAuth2UserService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    @DisplayName("신규 사용자 생성 시 FREE 구독 자동 생성")
    void newUserAutoSubscription() {
        // Given
        OAuth2UserRequest request = createKakaoUserRequest();

        // When
        OAuth2User result = oAuth2UserService.loadUser(request);

        // Then
        CustomUserPrincipal principal = (CustomUserPrincipal) result;
        Long userId = principal.getUserId();

        // User 생성 확인
        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getEmail()).isEqualTo("test@kakao.com");

        // Subscription 자동 생성 확인
        Subscription subscription = subscriptionRepository
            .findValidSubscription(user)
            .orElseThrow();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getPlan().getName()).isEqualTo("FREE");
        assertThat(subscription.getEndDate()).isAfter(LocalDateTime.now());
    }
}
```

### 2. 기존 사용자 재로그인 테스트

```java
@Test
@DisplayName("기존 사용자 재로그인 시 구독 생성 안 함")
void existingUserNoNewSubscription() {
    // Given: 기존 사용자 + FREE 구독
    User existingUser = createUserWithSubscription();
    Long originalSubscriptionId = existingUser.getSubscription().getId();

    // When: 재로그인
    OAuth2UserRequest request = createKakaoUserRequestForExistingUser();
    OAuth2User result = oAuth2UserService.loadUser(request);

    // Then: 구독이 새로 생성되지 않아야 함
    CustomUserPrincipal principal = (CustomUserPrincipal) result;
    User user = userRepository.findById(principal.getUserId()).orElseThrow();

    Subscription subscription = subscriptionRepository
        .findValidSubscription(user)
        .orElseThrow();

    // 같은 구독 ID
    assertThat(subscription.getId()).isEqualTo(originalSubscriptionId);

    // 구독 개수 확인 (1개만)
    long count = subscriptionRepository.countValidSubscriptions(user);
    assertThat(count).isEqualTo(1);
}
```

---

## 🎯 완료 기준

- [ ] CustomOAuth2UserService 리팩토링 완료
- [ ] 메서드 3개로 분리 (getOrCreateUser, updateExistingUser, createNewUserWithSubscription)
- [ ] 신규 사용자만 구독 생성 확인
- [ ] 기존 사용자는 구독 생성 안 함 확인
- [ ] TODO 주석 제거
- [ ] 테스트 통과

---

**Estimated Time**: 1-2시간
**Priority**: 🟡 Medium (리팩토링, 기능 개선)
**Dependencies**: Phase 2 완료 후 진행 권장