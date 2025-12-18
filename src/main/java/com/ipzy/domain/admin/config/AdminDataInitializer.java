package com.ipzy.domain.admin.config;

import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 초기 데이터 생성
 * 애플리케이션 시작 시 기본 관리자 계정이 없으면 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(0) // 다른 Initializer보다 먼저 실행
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_ADMIN_EMAIL = "admin@ipzy.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "1234qwer";
    private static final String DEFAULT_ADMIN_NAME = "관리자";

    @Override
    @Transactional
    public void run(String... args) {
        // 이미 관리자가 존재하면 초기화하지 않음
        if (userRepository.findByEmail(DEFAULT_ADMIN_EMAIL).isPresent()) {
            log.info("관리자 계정이 이미 존재합니다. 초기화를 건너뜁니다. (email: {})", DEFAULT_ADMIN_EMAIL);
            return;
        }

        log.info("기본 관리자 계정을 생성합니다...");

        User admin = User.builder()
                .email(DEFAULT_ADMIN_EMAIL)
                .name(DEFAULT_ADMIN_NAME)
                .password(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                .provider("LOCAL")
                .providerId("admin-local")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(admin);

        log.info("관리자 계정 생성 완료 (email: {}, password: {})", DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD);
    }
}
