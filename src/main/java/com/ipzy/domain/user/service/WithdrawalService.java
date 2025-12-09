package com.ipzy.domain.user.service;

import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.auth.unlink.OAuth2UnlinkStrategyFactory;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.exception.UserException;
import com.ipzy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정 탈퇴 전담 서비스.
 *
 * <p>탈퇴 프로세스:
 * <ol>
 *   <li>Step 1: 탈퇴 가능 여부 확인 (이미 탈퇴, 관리자 등)</li>
 *   <li>Step 2: OAuth Provider 연결 끊기</li>
 *   <li>Step 3: 연관 데이터 처리 (TODO: Phase 2)</li>
 *   <li>Step 4: User 상태 변경 + 개인정보 마스킹</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final UserRepository userRepository;
    private final OAuth2UnlinkStrategyFactory unlinkStrategyFactory;

    /**
     * 계정 탈퇴를 수행합니다.
     *
     * @param userId      탈퇴할 사용자 ID
     * @param provider    OAuth Provider (KAKAO, NAVER, GOOGLE)
     * @param accessToken OAuth access token (세션에서 조회)
     */
    @Transactional
    public void withdraw(Long userId, String provider, String accessToken) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.notFound(userId));

        // Step 1: 탈퇴 가능 여부 확인
        validateWithdrawal(user);

        // Step 2: OAuth 연결 끊기
        unlinkOAuthProvider(provider, accessToken);

        // Step 3: 연관 데이터 처리 (TODO: Phase 2)
        // processRelatedData(userId);

        // Step 4: User 탈퇴 처리 (상태 변경 + 개인정보 마스킹)
        user.withdraw();

        log.info("계정 탈퇴 완료 - userId: {}, provider: {}", userId, provider);
    }

    /**
     * 탈퇴 가능 여부를 검증합니다.
     */
    private void validateWithdrawal(User user) {

        // 이미 탈퇴한 계정인지 확인
        if (user.getStatus() == UserStatus.DELETED) {
            throw UserException.alreadyDeleted();
        }

        // 관리자 계정인지 확인
        if (user.getRole() == UserRole.ADMIN) {
            throw UserException.adminCannotWithdraw();
        }

        // TODO: Phase 2 - 활성 구독 확인
        // TODO: Phase 2 - 미완료 결제 확인
    }

    /**
     * OAuth Provider와의 연결을 끊습니다.
     *
     * <p>연결 끊기에 실패해도 탈퇴는 계속 진행됩니다.
     */
    private void unlinkOAuthProvider(String provider, String accessToken) {

        if (provider == null || accessToken == null) {
            log.warn("OAuth 정보가 없어 연결 끊기를 건너뜁니다");
            return;
        }

        boolean success = unlinkStrategyFactory.unlink(provider, accessToken);

        if (success) {
            log.info("OAuth 연결 끊기 성공 - provider: {}", provider);
        } else {
            log.warn("OAuth 연결 끊기 실패 - provider: {} (탈퇴는 계속 진행)", provider);
        }
    }
}