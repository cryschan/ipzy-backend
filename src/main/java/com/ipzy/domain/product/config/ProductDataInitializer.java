package com.ipzy.domain.product.config;

import com.ipzy.domain.product.entity.Brand;
import com.ipzy.domain.product.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 브랜드 초기 데이터 생성
 * docs/product/brand-by-style.md 문서 기준으로 생성
 */
@Slf4j
@Component
@Order(2) // QuizDataInitializer 다음에 실행
@RequiredArgsConstructor
public class ProductDataInitializer implements CommandLineRunner {

    private final BrandRepository brandRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // 이미 데이터가 있으면 초기화하지 않음
        if (brandRepository.count() > 0) {
            log.info("브랜드/상품 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("브랜드 기본 데이터를 초기화합니다...");

        List<Brand> brands = createBrands();
        brandRepository.saveAll(brands);

        log.info("브랜드 초기화 완료: 총 {}개 (CLOTHING: 2개, SHOES: 2개)", brands.size());
        log.info("상품 크롤링: /api/admin/crawling 엔드포인트 사용");
    }

    private List<Brand> createBrands() {
        List<Brand> brands = new ArrayList<>();

        // ==========================================
        // 의류 브랜드 (CLOTHING) - 무신사 크롤링
        // ==========================================

        // 스트릿 스타일
        brands.add(Brand.builder()
                .name("Thisisneverthat")
                .primaryStyle("street")
                .brandType("CLOTHING")
                .build());

        // 미니멀 스타일
        brands.add(Brand.builder()
                .name("Musinsa Standard")
                .primaryStyle("minimalist")
                .brandType("CLOTHING")
                .build());

        // ==========================================
        // 신발 브랜드 (SHOES) - 무신사 크롤링
        // ==========================================

        // 스니커즈
        brands.add(Brand.builder()
                .name("Nike")
                .primaryStyle("sneakers")
                .brandType("SHOES")
                .build());

        brands.add(Brand.builder()
                .name("Adidas")
                .primaryStyle("sneakers")
                .brandType("SHOES")
                .build());

        return brands;
    }

}
