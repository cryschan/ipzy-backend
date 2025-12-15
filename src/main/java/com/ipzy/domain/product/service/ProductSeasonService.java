package com.ipzy.domain.product.service;

import com.ipzy.domain.product.entity.Product;
import com.ipzy.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

/**
 * 시즌별 상품 관리 서비스
 * 현재 시즌에 맞는 상품만 활성화하고, 이전 시즌 상품은 비활성화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSeasonService {

    private final ProductRepository productRepository;

    /**
     * 현재 시즌 가져오기
     * SS (Spring/Summer): 3월 ~ 8월
     * FW (Fall/Winter): 9월 ~ 2월
     *
     * @return 현재 시즌 (예: "2025_SS", "2025_FW")
     */
    public String getCurrentSeason() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        Month month = now.getMonth();

        // 3월 ~ 8월: SS (Spring/Summer)
        if (month.getValue() >= 3 && month.getValue() <= 8) {
            return year + "_SS";
        }
        // 9월 ~ 12월: 다음 해 FW
        else if (month.getValue() >= 9) {
            return year + "_FW";
        }
        // 1월 ~ 2월: 현재 해 FW
        else {
            return year + "_FW";
        }
    }

    /**
     * 현재 시즌에 맞는 상품만 활성화
     * seasons 배열에 현재 시즌이 포함된 상품만 isActive = true로 설정
     *
     * @return 활성화된 상품 수
     */
    @Transactional
    public int activateSeasonalProducts() {
        String currentSeason = getCurrentSeason();
        log.info("시즌별 상품 활성화 시작: 현재 시즌={}", currentSeason);

        // 모든 상품 조회
        List<Product> allProducts = productRepository.findAll();
        int activatedCount = 0;
        int deactivatedCount = 0;

        for (Product product : allProducts) {
            // 이미 삭제된 상품은 건너뛰기
            if (product.isDeleted()) {
                continue;
            }

            boolean shouldBeActive = isSeasonalProduct(product, currentSeason);

            // 상태 변경이 필요한 경우에만 업데이트
            if (shouldBeActive && !product.getIsActive()) {
                product.activate();
                activatedCount++;
                log.debug("상품 활성화: {}", product.getName());
            } else if (!shouldBeActive && product.getIsActive()) {
                product.deactivate();
                deactivatedCount++;
                log.debug("상품 비활성화: {}", product.getName());
            }
        }

        log.info("시즌별 상품 활성화 완료: 활성화={}개, 비활성화={}개", activatedCount, deactivatedCount);
        return activatedCount;
    }

    /**
     * 특정 상품이 현재 시즌에 맞는지 확인
     */
    private boolean isSeasonalProduct(Product product, String currentSeason) {
        // seasons가 null이거나 비어있으면 사계절 상품으로 간주
        if (product.getSeasons() == null || product.getSeasons().length == 0) {
            return true;
        }

        // seasons 배열에 현재 시즌이 포함되어 있는지 확인
        for (String season : product.getSeasons()) {
            if (season != null && season.equals(currentSeason)) {
                return true;
            }
            // ALL 또는 all_season 같은 값은 사계절 상품
            if (season != null && (season.equalsIgnoreCase("ALL") || season.equalsIgnoreCase("all_season"))) {
                return true;
            }
        }

        return false;
    }

    /**
     * 상품에 시즌 정보 추가
     * 크롤링 시 카테고리별로 적절한 시즌 자동 설정
     *
     * @param category 카테고리
     * @param currentSeason 현재 시즌
     * @return 시즌 배열
     */
    public String[] determineSeasons(String category, String currentSeason) {
        if (category == null) {
            return new String[]{currentSeason};
        }

        return switch (category.toUpperCase()) {
            case "OUTER", "아우터" -> {
                // 아우터는 주로 FW 시즌
                if (currentSeason.endsWith("_FW")) {
                    yield new String[]{currentSeason};
                } else {
                    // SS 시즌에는 얇은 아우터만 해당
                    yield new String[]{currentSeason};
                }
            }
            case "TOP", "상의", "BOTTOM", "하의" -> {
                // 상의/하의는 대부분 현재 시즌 + 다음 시즌도 가능
                yield new String[]{currentSeason};
            }
            case "SHOES", "신발", "ACCESSORY", "액세서리" -> {
                // 신발/액세서리는 사계절
                yield new String[]{"ALL"};
            }
            default -> new String[]{currentSeason};
        };
    }

    /**
     * 시즌 전환 시 이전 시즌 상품 정리
     * 삭제하지 않고 비활성화만 수행
     */
    @Transactional
    public void seasonTransition() {
        log.info("시즌 전환 시작");

        // 현재 시즌에 맞는 상품만 활성화
        int activatedCount = activateSeasonalProducts();

        log.info("시즌 전환 완료: {}개 상품 활성화", activatedCount);
    }
}
