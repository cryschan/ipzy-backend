package com.ipzy.domain.user.service;

import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.subscription.service.SubscriptionService;
import com.ipzy.domain.user.dto.UserProfileResponse;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.exception.UserException;
import com.ipzy.domain.user.repository.UserRepository;
import com.ipzy.domain.user.vo.UserStylePreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    // ========== 외부용 (API) - DTO 반환 ==========

    public UserProfileResponse getMyProfile(Long userId) {
        User user = findActiveUser(userId);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, String name, String phone, String profileImageUrl) {
        User user = findActiveUser(userId);
        user.updateProfile(name, phone, profileImageUrl);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updatePreferences(Long userId, Map<String, Object> preferences) {
        User user = findActiveUser(userId);
        user.updatePreferences(preferences);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void deleteAccount(Long userId) {

        User user = findActiveUser(userId);
        user.delete();
    }

    /**
     * 사용자를 물리적으로 삭제합니다. (테스트 전용)
     *
     * <p>주의: 프로덕션에서 사용 금지. 연관 데이터 FK 제약으로 실패할 수 있습니다.
     *
     * @param userId 삭제할 사용자 ID
     */
    @Transactional
    public void hardDelete(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.notFound(userId));

        userRepository.delete(user);
    }

    @Transactional
    public UserProfileResponse updateStylePreference(Long userId, UserStylePreference stylePreference) {
        User user = findActiveUser(userId);
        user.updateStylePreference(stylePreference);
        return UserProfileResponse.from(user);
    }

    // ========== 내부용 (다른 도메인) - Entity 반환 ==========

    /**
     * 활성 사용자 조회 (삭제되지 않은 사용자)
     *
     * @param userId 사용자 ID
     * @return 활성 상태의 사용자
     * @throws UserException 사용자가 없거나 삭제된 경우
     */
    public User findActiveUser(Long userId) {
        return userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED)
                .orElseThrow(() -> UserException.notFound(userId));
    }

    /**
     * OAuth 로그인 시 사용자 조회 또는 생성
     * <p>
     * 조회 우선순위:
     * 1. provider + providerId로 조회
     * 2. email로 조회 (다른 Provider로 가입한 경우 소셜 연동)
     * 3. 없으면 새 사용자 생성
     *
     * @param provider     OAuth 제공자 (KAKAO, NAVER, GOOGLE 등)
     * @param providerId   제공자별 사용자 고유 ID
     * @param email        사용자 이메일
     * @param name         사용자 이름 (없으면 이메일 앞부분으로 대체됨)
     * @param profileImage 프로필 이미지 URL
     * @return 조회되거나 생성된 사용자
     */
    @Transactional
    public User findOrCreateByOAuth(String provider, String providerId,
                                    String email, String name, String profileImage) {

        // 이름이 없으면 이메일 앞부분으로 대체
        String finalName = (name == null || name.isBlank())
                ? email.split("@")[0]
                : name;

        if (!finalName.equals(name)) {
            log.info("{} 사용자 이름이 없어 이메일로 대체: {}", provider, finalName);
        }

        // 1. provider+providerId로 조회 → 2. email로 조회 → 3. 새 사용자 생성
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .or(() -> userRepository.findByEmail(email))
                .map(existingUser -> updateExistingUser(existingUser, provider, providerId, finalName, profileImage))
                .orElseGet(() -> createNewUser(provider, providerId, email, finalName, profileImage));

        user.updateLastLoginAt();

        return user;
    }

    /**
     * 기존 사용자 정보 업데이트 (소셜 연동 포함)
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
     * 새 사용자 생성 + FREE 구독 자동 생성
     */
    private User createNewUser(String provider, String providerId,
                               String email, String name, String profileImage) {

        log.info("새 사용자 생성: provider={}, email={}", provider, email);

        User user = userRepository.save(User.builder()
                .email(email)
                .name(name)
                .profileImageUrl(profileImage)
                .provider(provider)
                .providerId(providerId)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

        // ⭐ 신규 사용자에게 FREE 구독 자동 생성
        subscriptionService.ensureDefaultSubscription(user.getId());
        log.info("FREE 구독 생성 완료: userId={}, email={}", user.getId(), email);

        return user;
    }
}
