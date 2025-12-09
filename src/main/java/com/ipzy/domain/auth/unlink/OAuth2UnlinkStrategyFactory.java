package com.ipzy.domain.auth.unlink;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provider별 UnlinkStrategy를 조회하는 팩토리.
 *
 * <p>Spring의 자동 주입을 활용하여 모든 Strategy 구현체를 수집합니다.
 */
@Slf4j
@Component
public class OAuth2UnlinkStrategyFactory {

    private final Map<String, OAuth2UnlinkStrategy> strategies;

    /**
     * 모든 OAuth2UnlinkStrategy 빈을 주입받아 Map으로 변환합니다.
     *
     * @param strategyList 등록된 모든 OAuth2UnlinkStrategy 구현체
     */
    public OAuth2UnlinkStrategyFactory(List<OAuth2UnlinkStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        strategy -> strategy.getProvider().toUpperCase(),
                        Function.identity()
                ));
        log.info("등록된 연결 끊기 전략: {}", strategies.keySet());
    }

    /**
     * Provider에 해당하는 Strategy를 반환합니다.
     *
     * @param provider Provider 이름 (KAKAO, NAVER, GOOGLE)
     * @return Strategy Optional (없으면 empty)
     */
    public Optional<OAuth2UnlinkStrategy> getStrategy(String provider) {

        if (provider == null) {
            log.warn("provider 정보가 없어 연결 끊기 전략을 찾을 수 없습니다");
            return Optional.empty();
        }

        return Optional.ofNullable(strategies.get(provider.toUpperCase()));
    }

    /**
     * Provider의 연결 끊기를 수행합니다.
     *
     * @param provider    Provider 이름
     * @param accessToken OAuth2 access token
     * @return 연결 끊기 성공 여부 (Strategy가 없으면 false)
     */
    public boolean unlink(String provider, String accessToken) {
        return getStrategy(provider)
                .map(strategy -> strategy.unlink(accessToken))
                .orElseGet(() -> {
                    log.warn("{}에 대한 연결 끊기 전략이 없습니다", provider);
                    return false;
                });
    }
}