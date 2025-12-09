package com.ipzy.domain.user.controller;

import com.ipzy._global.common.ApiResponse;
import com.ipzy.domain.auth.dto.CustomUserPrincipal;
import com.ipzy.domain.auth.exception.AuthException;
import com.ipzy.domain.auth.logout.OAuth2TokenSessionKey;
import com.ipzy.domain.user.dto.UpdatePreferencesRequest;
import com.ipzy.domain.user.dto.UpdateProfileRequest;
import com.ipzy.domain.user.dto.UpdateStylePreferenceRequest;
import com.ipzy.domain.user.dto.UserProfileResponse;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.service.UserService;
import com.ipzy.domain.user.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 API")
@RequiredArgsConstructor
@RequestMapping("/api/users")
@RestController
public class UserController {

    private final UserService userService;
    private final WithdrawalService withdrawalService;

    @Operation(
            summary = "내 프로필 조회",
            description = "로그인한 사용자의 상세 프로필 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않음 (AUTH_001)"
            )
    })
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        validatePrincipal(principal);

        User user = userService.getMyProfile(principal.getUserId());
        return ApiResponse.success(UserProfileResponse.from(user));
    }

    @Operation(
            summary = "프로필 수정",
            description = "이름, 전화번호, 프로필 이미지를 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않음 (AUTH_001)"
            )
    })
    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        validatePrincipal(principal);

        User user = userService.updateProfile(
                principal.getUserId(),
                request.name(),
                request.phone(),
                request.profileImageUrl()
        );
        return ApiResponse.success(UserProfileResponse.from(user));
    }

    @Operation(
            summary = "환경설정 수정",
            description = "다크모드, 알림 등 앱 환경설정을 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않음 (AUTH_001)"
            )
    })
    @PutMapping("/me/preferences")
    public ApiResponse<UserProfileResponse> updatePreferences(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody UpdatePreferencesRequest request) {

        validatePrincipal(principal);

        User user = userService.updatePreferences(principal.getUserId(), request.preferences());
        return ApiResponse.success(UserProfileResponse.from(user));
    }

    @Operation(
            summary = "스타일 선호도 수정",
            description = "추천에 사용되는 스타일 선호도(색상, 나이, 성별, 스타일)를 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않음 (AUTH_001)"
            )
    })
    @PutMapping("/me/style-preference")
    public ApiResponse<UserProfileResponse> updateStylePreference(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody UpdateStylePreferenceRequest request) {

        validatePrincipal(principal);

        User user = userService.updateStylePreference(principal.getUserId(), request.toVo());

        return ApiResponse.success(UserProfileResponse.from(user));
    }

    @Operation(
            summary = "계정 탈퇴",
            description = "계정을 탈퇴합니다. OAuth 연결을 끊고, 개인정보를 마스킹 처리합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "탈퇴 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이미 탈퇴한 계정 (USER_003) 또는 관리자 계정 (USER_004)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않음 (AUTH_001)"
            )
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest request) {

        validatePrincipal(principal);

        // 세션에서 OAuth 정보 조회
        HttpSession session = request.getSession(false);
        String provider = null;
        String accessToken = null;

        if (session != null) {
            provider = (String) session.getAttribute("OAUTH2_PROVIDER");
            if (provider != null) {
                String sessionKey = OAuth2TokenSessionKey.getSessionKey(provider);
                accessToken = (String) session.getAttribute(sessionKey);
            }
        }

        // 탈퇴 수행
        withdrawalService.withdraw(principal.getUserId(), provider, accessToken);

        // 세션 무효화
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build();
    }

    private void validatePrincipal(CustomUserPrincipal principal) {
        if (principal == null) {
            throw AuthException.unauthorized();
        }
    }

    // ========== 테스트 전용 API ==========

    @Operation(
            summary = "[테스트] 계정 하드 딜리트",
            description = "사용자를 물리적으로 삭제합니다. 테스트 환경에서만 사용하세요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음"
            )
    })
    @DeleteMapping("/test/{userId}/hard-delete")
    public ResponseEntity<Void> hardDelete(@PathVariable Long userId) {

        userService.hardDelete(userId);
        return ResponseEntity.noContent().build();
    }
}
