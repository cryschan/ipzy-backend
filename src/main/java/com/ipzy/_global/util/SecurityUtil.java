package com.ipzy._global.util;

import com.ipzy.domain.auth.dto.CustomUserPrincipal;
import com.ipzy.domain.auth.exception.AuthException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Spring Security 관련 유틸리티 클래스
 * OAuth2 세션 기반 인증에서 사용자 정보 조회 기능 제공
 */
public final class SecurityUtil {

    private SecurityUtil() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    /**
     * 현재 인증된 Authentication 객체를 반환
     */
    public static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * 현재 인증된 사용자의 CustomUserPrincipal을 반환
     */
    public static Optional<CustomUserPrincipal> getCurrentPrincipal() {
        return getAuthentication()
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof CustomUserPrincipal)
                .map(principal -> (CustomUserPrincipal) principal);
    }

    /**
     * 현재 인증된 사용자의 ID를 반환
     */
    public static Optional<Long> getCurrentUserId() {
        return getCurrentPrincipal()
                .map(CustomUserPrincipal::getUserId);
    }

    /**
     * 현재 인증된 사용자의 ID를 반환 (없으면 AuthException 발생)
     *
     * @throws AuthException 인증되지 않은 경우
     */
    public static Long getCurrentUserIdOrThrow() {
        return getCurrentUserId()
                .orElseThrow(AuthException::unauthorized);
    }

    /**
     * 현재 인증된 사용자의 ID를 반환 (없으면 null)
     */
    public static Long getCurrentUserIdOrNull() {
        return getCurrentUserId().orElse(null);
    }

    /**
     * 사용자가 인증되었는지 확인
     */
    public static boolean isAuthenticated() {
        return getCurrentPrincipal().isPresent();
    }
}
