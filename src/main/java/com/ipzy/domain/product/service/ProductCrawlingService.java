package com.ipzy.domain.product.service;

import com.ipzy.domain.product.dto.CrawledProductDto;
import com.ipzy.domain.product.entity.Brand;
import com.ipzy.domain.product.entity.Product;
import com.ipzy.domain.product.exception.ProductErrorCode;
import com.ipzy.domain.product.exception.ProductException;
import com.ipzy.domain.product.repository.BrandRepository;
import com.ipzy.domain.product.repository.ProductRepository;
import com.ipzy._global.common.enums.ClothingCategory;
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
    public int crawlAndSaveBrandProducts(String brandName, String brandType, String style, int limit) {
        log.info("브랜드 상품 크롤링 및 저장 시작: {}, brandType: {}", brandName, brandType);

        // 파라미터 검증
        if (brandType == null || brandType.isBlank()) {
            throw new ProductException(ProductErrorCode.INVALID_BRAND_TYPE,
                    "브랜드 타입은 필수입니다");
        }

        // 브랜드 확인 (name + brandType으로 조회)
        Brand brand = brandRepository.findByNameAndBrandType(brandName, brandType)
                .orElseThrow(() -> new ProductException(ProductErrorCode.BRAND_NOT_FOUND,
                        String.format("브랜드를 찾을 수 없습니다: name=%s, brandType=%s", brandName, brandType)));

        // 브랜드 타입 검증 (DB에서 조회한 엔티티도 체크)
        if (brand.getBrandType() == null || brand.getBrandType().isBlank()) {
            throw new ProductException(ProductErrorCode.INVALID_BRAND_TYPE,
                    "DB의 브랜드 타입이 설정되지 않았습니다: " + brandName);
        }

        // 브랜드 타입에 따라 크롤러 선택
        List<CrawledProductDto> crawledProducts;
        if ("CLOTHING".equals(brand.getBrandType())) {
            log.info("의류 브랜드로 무신사 크롤링 시작");
            crawledProducts = musinsaCrawlerService.crawlBrandProducts(brandName, style, limit);
        } else if ("SHOES".equals(brand.getBrandType())) {
            log.info("신발 브랜드로 무신사 크롤링 시작");
            crawledProducts = musinsaCrawlerService.crawlShoeProducts(brandName, style, limit);
        } else {
            throw new ProductException(ProductErrorCode.INVALID_BRAND_TYPE,
                    "지원하지 않는 브랜드 타입입니다: " + brand.getBrandType());
        }

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
                // 브랜드 조회 (카테고리에 따라 brandType 결정)
                String brandType = "SHOES".equals(dto.getCategory()) ? "SHOES" : "CLOTHING";
                Brand brand = brandRepository.findByNameAndBrandType(dto.getBrandName(), brandType).orElse(null);
                if (brand == null) {
                    log.warn("브랜드를 찾을 수 없어 건너뜀: brandName={}, brandType={}", dto.getBrandName(), brandType);
                    continue;
                }

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
     *
     * @throws ProductException price가 null이거나 0 이하인 경우 (크롤링 시점에서 필터링되어야 함)
     */
    private Product convertToProduct(CrawledProductDto dto, Brand brand) {
        // 필수 필드 검증: price는 크롤링 시점에서 이미 필터링되어야 하므로, 여기서 null이면 논리적 오류
        if (dto.getPrice() == null || dto.getPrice() <= 0) {
            throw new ProductException(ProductErrorCode.INVALID_PRODUCT_DATA,
                    String.format("상품 가격이 유효하지 않습니다 (크롤링 필터링 누락 가능성): name=%s, price=%s, brand=%s",
                            dto.getName(), dto.getPrice(), dto.getBrandName()));
        }

        // 카테고리 변환
        ClothingCategory category = parseCategory(dto.getCategory());

        // 현재 시즌 가져오기
        String currentSeason = productSeasonService.getCurrentSeason();

        // 카테고리에 따라 시즌 자동 설정
        String[] seasons = productSeasonService.determineSeasons(dto.getCategory(), currentSeason);
        String thumbnailImageUrl = dto.getThumbnailImageUrl() != null ? dto.getThumbnailImageUrl() : "";

        // null 기본값 처리
        int originalPrice = dto.getOriginalPrice() != null ? dto.getOriginalPrice() : dto.getPrice();
        int discountPercent = dto.getDiscountPercent() != null ? dto.getDiscountPercent() : 0;

        return Product.builder()
                .brand(brand)
                .name(dto.getName())
                .category(category)
                .subCategory(dto.getSubCategory())
                .primaryStyle(brand.getPrimaryStyle()) // 브랜드의 스타일 사용
                .price(dto.getPrice())
                .originalPrice(originalPrice)
                .discountPercent(discountPercent)
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
     * 신발 랭킹에서 카테고리별 상품 크롤링 및 저장 (브랜드 자동 생성)
     *
     * @param shoeCategory 신발 카테고리 (sneakers, boots, sandals 등)
     * @param limit 상품 수
     * @return 저장된 상품 수
     */
    @Transactional
    public int crawlAndSaveShoesRanking(String shoeCategory, int limit) {
        log.info("신발 랭킹 크롤링 및 저장 시작: {}", shoeCategory);

        // 신발 랭킹 API에서 상품 크롤링
        List<CrawledProductDto> crawledProducts = musinsaCrawlerService.crawlShoesRanking(shoeCategory, limit);

        // 저장
        int savedCount = 0;
        for (CrawledProductDto dto : crawledProducts) {
            try {
                // 브랜드명 검증
                if (dto.getBrandName() == null || dto.getBrandName().isBlank()) {
                    log.warn("브랜드명이 없는 상품 건너뜀: {}", dto.getName());
                    continue;
                }

                // 브랜드 조회 또는 생성 (SHOES 타입)
                Brand brand = getOrCreateBrand(dto.getBrandName(), "SHOES", shoeCategory);

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

        log.info("신발 랭킹 저장 완료: {} ({}개)", shoeCategory, savedCount);
        return savedCount;
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
            int saved = crawlAndSaveBrandProducts(brand.getName(), brand.getBrandType(), style, 1);
            totalSaved += saved;
        }

        log.info("스타일별 상품 크롤링 완료: {} (총 {}개)", style, totalSaved);
        return totalSaved;
    }

    /**
     * 브랜드 조회 또는 생성 (동시성 문제 해결)
     * - unique constraint 위반 시 재조회
     */
    private Brand getOrCreateBrand(String brandName, String brandType, String primaryStyle) {
        return brandRepository.findByNameAndBrandType(brandName, brandType)
                .orElseGet(() -> {
                    try {
                        log.info("브랜드 자동 생성: name={}, type={}", brandName, brandType);
                        Brand newBrand = Brand.builder()
                                .name(brandName)
                                .brandType(brandType)
                                .primaryStyle(primaryStyle)
                                .build();
                        return brandRepository.save(newBrand);
                    } catch (Exception e) {
                        // 동시 요청으로 인한 중복 생성 시도 시 재조회
                        log.warn("브랜드 생성 실패 (중복 가능성), 재조회: name={}, type={}", brandName, brandType);
                        return brandRepository.findByNameAndBrandType(brandName, brandType)
                                .orElseThrow(() -> new ProductException(ProductErrorCode.BRAND_NOT_FOUND,
                                        "브랜드 생성 및 재조회 실패: " + brandName));
                    }
                });
    }
}
