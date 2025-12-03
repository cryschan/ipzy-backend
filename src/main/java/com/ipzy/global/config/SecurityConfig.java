package com.ipzy.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)  // REST API이므로 비활성화 (SameSite 쿠키로 대응)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(5)  // 동시 세션 최대 5개
                .maxSessionsPreventsLogin(false)  // 새 로그인 허용, 기존 세션 만료
            )
            .authorizeHttpRequests(auth -> auth
                // 인증
                .requestMatchers("/api/auth/**").permitAll()
                // 공개 API
                .requestMatchers("/api/quizzes/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/brands/**").permitAll()
                // Swagger
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
