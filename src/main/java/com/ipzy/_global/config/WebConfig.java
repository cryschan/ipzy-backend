package com.ipzy._global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정 - CORS 정책 설정
 *
 * SECURITY NOTE:
 * - allowedOriginPatterns("*") + allowCredentials(true)는 CSRF 취약점
 * - 반드시 환경변수로 명시적인 origin 목록을 설정해야 함
 * - 예: CORS_ALLOWED_ORIGINS=http://localhost:5173,https://ipzy.com
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split(",");

        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
