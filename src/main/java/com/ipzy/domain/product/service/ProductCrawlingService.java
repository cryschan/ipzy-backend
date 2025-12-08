package com.ipzy.domain.product.service;

import com.ipzy.domain.product.dto.CrawledProductDto;
import com.ipzy.domain.product.entity.Brand;
import com.ipzy.domain.product.entity.Product;
import com.ipzy.domain.product.repository.BrandRepository;
import com.ipzy.domain.product.repository.ProductRepository;
import com.ipzy.global.common.enums.ClothingCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 크롤링한 상품 데이터를 데이터베이스에 저장하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCrawlingService {

    private final MusinsaCrawlerService musinsaCrawlerService;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ProductSeasonService productSeasonService;

    /**
     * 특정 브랜드의 상품을 크롤링하여 저장
     *
     * @param brandName 브랜드명
     * @param style 스타일
     * @param limit 상품 수
     * @return 저장된 상품 수
     */
    @Transactional
    public int crawlAndSaveBrandProducts(String brandName, String style, int limit) {
        log.info("브랜드 상품 크롤링 및 저장 시작: {}", brandName);

        // 브랜드 확인
        Brand brand = brandRepository.findByName(brandName)
                .orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다: " + brandName));

        // 크롤링
        List<CrawledProductDto> crawledProducts = musinsaCrawlerService.crawlBrandProducts(brandName, style, limit);

        // 저장
        int savedCount = 0;
        for (CrawledProductDto dto : crawledProducts) {
            try {
                // 중복 체크
                if (productRepository.existsByNameAndBrandId(dto.getName(), brand.getId())) {
                    log.debug("이미 존재하는 상품: {}", dto.getName());
                    continue;
                }

                Product product = convertToProduct(dto, brand);
                productRepository.save(product);
                savedCount++;

            } catch (Exception e) {
                log.error("상품 저장 실패: {}, 에러: {}", dto.getName(), e.getMessage());
            }
        }

        log.info("브랜드 상품 저장 완료: {} ({}개)", brandName, savedCount);
        return savedCount;
    }

    /**
     * 모든 브랜드의 상품을 크롤링하여 저장
     *
     * @return 저장된 상품 수
     */
    @Transactional
    public int crawlAndSaveAllProducts() {
        log.info("전체 상품 크롤링 및 저장 시작");

        // 전체 브랜드 크롤링
        List<CrawledProductDto> allProducts = musinsaCrawlerService.crawlAllBrands();

        // 저장
        int savedCount = 0;
        List<String> errors = new ArrayList<>();

        for (CrawledProductDto dto : allProducts) {
            try {
                // 브랜드 조회
                Brand brand = brandRepository.findByName(dto.getBrandName())
                        .orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다: " + dto.getBrandName()));

                // 중복 체크
                if (productRepository.existsByNameAndBrandId(dto.getName(), brand.getId())) {
                    log.debug("이미 존재하는 상품: {}", dto.getName());
                    continue;
                }

                // 상품 저장
                Product product = convertToProduct(dto, brand);
                productRepository.save(product);
                savedCount++;

            } catch (Exception e) {
                String error = String.format("상품 저장 실패: %s, 에러: %s", dto.getName(), e.getMessage());
                log.error(error);
                errors.add(error);
            }
        }

        log.info("전체 상품 저장 완료: 총 {}개 저장, {}개 실패", savedCount, errors.size());
        if (!errors.isEmpty()) {
            log.warn("저장 실패 목록: {}", errors);
        }

        return savedCount;
    }

    /**
     * CrawledProductDto를 Product 엔티티로 변환
     */
    private Product convertToProduct(CrawledProductDto dto, Brand brand) {
        // 카테고리 변환
        ClothingCategory category = parseCategory(dto.getCategory());

        // 현재 시즌 가져오기
        String currentSeason = productSeasonService.getCurrentSeason();

        // 카테고리에 따라 시즌 자동 설정
        String[] seasons = productSeasonService.determineSeasons(dto.getCategory(), currentSeason);
        String thumbnailImageUrl = dto.getThumbnailImageUrl() != null ? dto.getThumbnailImageUrl() : "";

        return Product.builder()
                .brand(brand)
                .name(dto.getName())
                .category(category)
                .subCategory(dto.getSubCategory())
                .primaryStyle(brand.getPrimaryStyle()) // 브랜드의 스타일 사용
                .price(dto.getPrice())
                .originalPrice(dto.getOriginalPrice())
                .discountPercent(dto.getDiscountPercent())
                .thumbnailImageUrl(thumbnailImageUrl)
                .description(dto.getDescription())
                .colors(dto.getColors() != null ? dto.getColors().toArray(new String[0]) : null)
                .seasons(seasons) // 시즌 정보 추가
                .isActive(true)
                .purchaseUrl(dto.getPurchaseUrl())
                .build();
    }

    /**
     * 카테고리 문자열을 ClothingCategory enum으로 변환
     */
    private ClothingCategory parseCategory(String categoryStr) {
        if (categoryStr == null) {
            return ClothingCategory.TOP;
        }

        return switch (categoryStr.toUpperCase()) {
            case "TOP", "상의" -> ClothingCategory.TOP;
            case "BOTTOM", "하의" -> ClothingCategory.BOTTOM;
            case "OUTER", "아우터" -> ClothingCategory.OUTER;
            case "SHOES", "신발" -> ClothingCategory.SHOES;
            case "ACCESSORY", "액세서리" -> ClothingCategory.ACCESSORY;
            default -> ClothingCategory.TOP;
        };
    }

    /**
     * 특정 스타일의 모든 브랜드 상품 크롤링
     *
     * @param style 스타일 (hip_hop, amekaji 등)
     * @return 저장된 상품 수
     */
    @Transactional
    public int crawlAndSaveByStyle(String style) {
        log.info("스타일별 상품 크롤링 시작: {}", style);

        // 해당 스타일의 브랜드 조회
        List<Brand> brands = brandRepository.findByPrimaryStyle(style);

        if (brands.isEmpty()) {
            log.warn("해당 스타일의 브랜드가 없습니다: {}", style);
            return 0;
        }

        int totalSaved = 0;
        for (Brand brand : brands) {
            int saved = crawlAndSaveBrandProducts(brand.getName(), style, 1);
            totalSaved += saved;
        }

        log.info("스타일별 상품 크롤링 완료: {} (총 {}개)", style, totalSaved);
        return totalSaved;
    }
}
