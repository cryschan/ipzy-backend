package com.ipzy.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 설정 - API 문서 자동 생성
 */
@Configuration
public class SwaggerConfig {

    @Value("${app.swagger.server-url}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList());
    }

    private Info apiInfo() {
        return new Info()
                .title("Ipzy API")
                .description(String.format("""
                        Ipzy 백엔드 REST API 문서

                        ## 인증 방식
                        세션 기반 OAuth2 인증 (카카오 로그인)

                        ## 테스트 방법
                        1. [카카오 로그인 바로가기](%s/oauth2/authorization/kakao) 클릭
                        2. 카카오 로그인 완료 후 세션 쿠키(JSESSIONID) 자동 발급
                        3. 같은 브라우저에서 Swagger UI로 API 테스트 가능

                        ## 주요 엔드포인트
                        | 엔드포인트 | 설명 |
                        |-----------|------|
                        | `GET /api/auth/login/kakao` | 카카오 로그인 (→ `/oauth2/authorization/kakao`로 리다이렉트) |
                        | `GET /api/auth/me` | 현재 로그인 사용자 정보 |
                        | `POST /api/auth/logout` | 로그아웃 |
                        """, serverUrl))
                .version("1.0.0");
    }

    private List<Server> serverList() {
        Server server = new Server()
                .url(serverUrl)
                .description("API Server");

        return List.of(server);
    }
}
