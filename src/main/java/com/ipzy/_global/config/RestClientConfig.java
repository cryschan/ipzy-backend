package com.ipzy._global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${ai.python.base-url}")
    private String pythonBaseUrl;

    @Value("${ai.python.connect-timeout}")
    private int pythonConnectTimeout;

    @Value("${ai.python.read-timeout}")
    private int pythonReadTimeout;

    /**
     * 무신사 API 호출용 RestClient
     */
    @Bean
    public RestClient musinsaRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .baseUrl("https://api.musinsa.com")
                .requestFactory(factory)
                .defaultHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .build();
    }

    /**
     * 파이썬 AI 서버 호출용 RestClient
     */
    @Bean
    public RestClient pythonRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(pythonConnectTimeout));
        factory.setReadTimeout(Duration.ofMillis(pythonReadTimeout));

        return RestClient.builder()
                .baseUrl(pythonBaseUrl)
                .requestFactory(factory)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
