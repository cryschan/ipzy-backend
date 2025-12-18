package com.ipzy._global.config;

import com.ipzy.domain.auth.exception.AuthErrorCode;
import com.ipzy.domain.auth.handler.OAuth2FailureHandler;
import com.ipzy.domain.auth.handler.OAuth2LogoutSuccessHandler;
import com.ipzy.domain.auth.handler.OAuth2SuccessHandler;
import com.ipzy.domain.auth.service.CustomOAuth2UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security 설정 클래스
 * - OAuth2 카카오 로그인
 * - 세션 기반 인증 (30분 타임아웃)
 * - REST API용 JSON 응답
 */
@Configuration  // Spring 설정 클래스임을 선언
@EnableWebSecurity  // Spring Security 활성화
@RequiredArgsConstructor  // final 필드 생성자 자동 생성 (DI용)
public class SecurityConfig {

    // OAuth2 로그인 시 사용자 정보를 처리하는 서비스
    private final CustomOAuth2UserService customOAuth2UserService;
    // OAuth2 로그인 성공 시 처리 핸들러 (프론트엔드로 리다이렉트)
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    // OAuth2 로그인 실패 시 처리 핸들러 (에러 코드와 함께 리다이렉트)
    private final OAuth2FailureHandler oAuth2FailureHandler;
    // OAuth2 로그아웃 성공 시 처리 핸들러 (Provider 토큰 revoke)
    private final OAuth2LogoutSuccessHandler oAuth2LogoutSuccessHandler;
    // OAuth2 클라이언트 등록 정보 저장소
    private final ClientRegistrationRepository clientRegistrationRepository;

    /**
     * Security Filter Chain 설정
     * 모든 HTTP 요청은 이 필터 체인을 거쳐 인증/인가 처리됨
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ========== CSRF 설정 ==========
                // CSRF 비활성화: SPA 프론트엔드와 통신하는 REST API 서버
                // 세션 쿠키는 SameSite=Lax 속성으로 CSRF 공격 방어
                .csrf(AbstractHttpConfigurer::disable)

                // ========== 폼 로그인 비활성화 ==========
                // REST API 서버이므로 기본 로그인 페이지 불필요
                .formLogin(AbstractHttpConfigurer::disable)

                // ========== 세션 관리 설정 ==========
                .sessionManagement(session -> session
                        // IF_REQUIRED: 필요할 때만 세션 생성 (OAuth2 로그인 성공 시)
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        // 동일 사용자 최대 2개 세션 허용 (PC + 모바일)
                        .maximumSessions(2)
                        // false: 세션 초과 시 기존 세션 만료 (새 로그인 허용)
                        // true로 하면 새 로그인 차단
                        .maxSessionsPreventsLogin(false)
                )

                // ========== URL별 접근 권한 설정 ==========
                .authorizeHttpRequests(auth -> auth
                        // /api/auth/me: 로그인한 사용자만 접근 가능
                        .requestMatchers("/api/auth/me").authenticated()
                        // /api/auth/**: 로그인, 로그아웃 등 인증 관련 API는 누구나 접근
                        .requestMatchers("/api/auth/**").permitAll()
                        // 퀴즈, 상품, 브랜드 API: 비로그인도 조회 가능
                        .requestMatchers("/api/quizzes/**").permitAll()
                        .requestMatchers("/api/quiz-sessions/**").permitAll()
                        .requestMatchers("/api/products/**").permitAll()
                        .requestMatchers("/api/brands/**").permitAll()
                        // 관리자 API: 개발/테스트용 임시 허용 (TODO: 프로덕션에서는 ADMIN 권한 필요)
                        .requestMatchers("/api/admin/**").permitAll()
                        // Swagger UI: 개발 편의를 위해 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 추천 API 설정
                        .requestMatchers("/api/recommendations/test").permitAll()  // 통신 테스트 (개발용)
                        .requestMatchers("/api/recommendations/sessions/*/preview-request").permitAll()  // 요청 미리보기 (개발용)
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/recommendations/sessions/*/generate").authenticated()  // 추천 생성: 로그인 필수
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/recommendations/sessions/*").permitAll()  // 추천 조회: 비로그인 허용
                        .requestMatchers("/api/subscription/plans").permitAll() // 플랜 목록 조회(비회원도 조회 가능)
                        // 그 외 모든 요청: 인증 필요
                        .anyRequest().authenticated()
                )

                // ========== OAuth2 로그인 설정 ==========
                .oauth2Login(oauth2 -> oauth2
                        // OAuth2 인증 시작 URL: /oauth2/authorization/kakao
                        // prompt=login 파라미터 추가하여 항상 카카오 로그인 화면 표시
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri("/oauth2/authorization")
                                .authorizationRequestResolver(
                                        new CustomAuthorizationRequestResolver(clientRegistrationRepository)
                                )
                        )
                        // 카카오에서 인증 후 돌아오는 콜백 URL
                        .redirectionEndpoint(endpoint -> endpoint
                                .baseUri("/api/auth/*/callback")
                        )
                        // 카카오에서 받은 사용자 정보 처리 서비스
                        .userInfoEndpoint(endpoint -> endpoint
                                .userService(customOAuth2UserService)
                        )
                        // 로그인 성공 시: 세션 생성 후 프론트엔드로 리다이렉트
                        .successHandler(oAuth2SuccessHandler)
                        // 로그인 실패 시: 에러 코드와 함께 프론트엔드로 리다이렉트
                        .failureHandler(oAuth2FailureHandler)
                )

                // ========== 인증 예외 처리 ==========
                .exceptionHandling(exception -> exception
                        // 인증되지 않은 요청이 보호된 리소스에 접근할 때 실행
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 세션 만료 vs 비로그인 구분 로직
                            // JSESSIONID 쿠키가 있으면 이전에 로그인했던 사용자 (세션 만료)
                            Cookie[] cookies = request.getCookies();
                            boolean hasSessionCookie = cookies != null &&
                                    Arrays.stream(cookies)
                                            .anyMatch(c -> "JSESSIONID".equals(c.getName()));

                            // 쿠키 있음 → AUTH_005 (세션 만료)
                            // 쿠키 없음 → AUTH_001 (비로그인)
                            AuthErrorCode errorCode = hasSessionCookie
                                    ? AuthErrorCode.SESSION_EXPIRED
                                    : AuthErrorCode.UNAUTHORIZED;

                            // JSON 형식으로 에러 응답 반환
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    String.format("{\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\"}}",
                                            errorCode.getCode(), errorCode.getMessage())
                            );
                        })
                )

                // ========== 로그아웃 설정 ==========
                .logout(logout -> logout
                        // 로그아웃 요청 URL (GET 방식 허용 - 하이퍼링크로 로그아웃 가능)
                        .logoutRequestMatcher(new AntPathRequestMatcher("/api/auth/logout"))
                        // 서버의 세션 무효화
                        .invalidateHttpSession(true)
                        // 브라우저의 세션 쿠키 삭제
                        .deleteCookies("JSESSIONID")
                        // 비로그인 상태에서도 로그아웃 요청 허용
                        .permitAll()
                        // 로그아웃 성공 시 카카오 계정 로그아웃 페이지로 리다이렉트
                        .logoutSuccessHandler(oAuth2LogoutSuccessHandler)
                );

        return http.build();
    }

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder Bean
     * 관리자 로그인 시 BCrypt 해시 비교에 사용
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
