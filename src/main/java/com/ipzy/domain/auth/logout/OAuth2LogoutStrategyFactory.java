package com.ipzy.domain.auth.logout;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provider별 LogoutStrategy를 조회하는 팩토리.
 *
 * <p>Spring의 자동 주입을 활용하여 모든 Strategy 구현체를 수집합니다.
 */
@Slf4j
@Component
public class OAuth2LogoutStrategyFactory {

    private final Map<String, OAuth2LogoutStrategy> strategies;

    /**
     * 모든 OAuth2LogoutStrategy 빈을 주입받아 Map으로 변환합니다.
     *
     * @param strategyList 등록된 모든 OAuth2LogoutStrategy 구현체
     */
    public OAuth2LogoutStrategyFactory(List<OAuth2LogoutStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        OAuth2LogoutStrategy::getProvider,
                        Function.identity()
                ));
        log.info("등록된 로그아웃 전략: {}", strategies.keySet());
    }

    /**
     * Provider에 해당하는 Strategy를 반환합니다.
     *
     * @param provider Provider 이름 (KAKAO, NAVER, GOOGLE)
     * @return Strategy Optional (없으면 empty)
     */
    public Optional<OAuth2LogoutStrategy> getStrategy(String provider) {
        return Optional.ofNullable(strategies.get(provider.toUpperCase()));
    }
}
