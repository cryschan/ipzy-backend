package com.ipzy.domain.admin.service;

import com.ipzy.domain.admin.dto.AdminLoginRequest;
import com.ipzy.domain.admin.dto.AdminLoginResponse;
import com.ipzy.domain.admin.exception.AdminException;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 인증 서비스
 */
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class AdminAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_SESSION_KEY = "ADMIN_USER";

    /**
     * 관리자 로그인
     */
    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request, HttpSession session) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(AdminException::invalidCredentials);

        // 2. 관리자 여부 확인
        if (!user.isAdmin()) {
            log.warn("관리자 로그인 시도 실패 - 권한 없음: {}", request.email());
            throw AdminException.notAdminUser();
        }

        // 3. 계정 상태 확인
        if (!user.isActive()) {
            log.warn("관리자 로그인 시도 실패 - 정지된 계정: {}", request.email());
            throw AdminException.accountSuspended();
        }

        // 4. 비밀번호 검증
        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("관리자 로그인 시도 실패 - 비밀번호 불일치: {}", request.email());
            throw AdminException.invalidCredentials();
        }

        // 5. 마지막 로그인 시간 업데이트
        user.updateLastLoginAt();

        // 6. 세션에 관리자 정보 저장
        // TODO: Session Fixation 방지 - 로그인 성공 시 세션 ID 재생성 필요
        //       HttpServletRequest.changeSessionId() 또는 session.invalidate() 후 새 세션 생성
        session.setAttribute(ADMIN_SESSION_KEY, user.getId());
        session.setMaxInactiveInterval(60 * 60); // 1시간

        log.info("관리자 로그인 성공: {} (ID: {})", user.getEmail(), user.getId());

        return AdminLoginResponse.from(user);
    }

    /**
     * 관리자 로그아웃
     */
    public void logout(HttpSession session) {
        Long adminId = (Long) session.getAttribute(ADMIN_SESSION_KEY);
        if (adminId != null) {
            log.info("관리자 로그아웃: ID {}", adminId);
        }
        session.invalidate();
    }

    /**
     * 현재 로그인한 관리자 정보 조회
     */
    public AdminLoginResponse getCurrentAdmin(HttpSession session) {
        Long adminId = (Long) session.getAttribute(ADMIN_SESSION_KEY);
        if (adminId == null) {
            throw AdminException.sessionRequired();
        }

        User user = userRepository.findById(adminId)
            .orElseThrow(AdminException::adminNotFound);

        if (!user.isAdmin()) {
            session.invalidate();
            throw AdminException.notAdminUser();
        }

        return AdminLoginResponse.from(user);
    }

    /**
     * 세션에서 관리자 ID 조회 (null 가능)
     */
    public Long getAdminIdFromSession(HttpSession session) {
        return (Long) session.getAttribute(ADMIN_SESSION_KEY);
    }
}
