package com.ipzy.domain.product.service;

import com.ipzy.domain.product.dto.CrawledProductDto;
import com.ipzy.domain.product.dto.FailedProductInfo;
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
import java.util.Set;

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
     * @param brandType 브랜드 타입 (CLOTHING, SHOES)
     * @param limit 상품 수
     * @return 크롤링 결과 (저장된 상품 수, 스타일, 실패 목록)
     */
    public CrawlingResult crawlAndSaveBrandProducts(String brandName, String brandType, int limit) {
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

        // DB에서 가져온 스타일 사용
        String style = brand.getPrimaryStyle();
        log.info("브랜드 스타일: {}", style);

        // 브랜드 타입에 따라 크롤러 선택 (트랜잭션 밖에서 크롤링)
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

        // 저장 (별도 트랜잭션)
        return saveProductsBatch(crawledProducts, brand, brandName, style);
    }

    /**
     * 크롤링한 상품 목록을 배치로 저장 (트랜잭션 내부)
     *
     * NOTE: Spring AOP 프록시가 동작하려면 public 또는 protected이어야 합니다.
     * private 메서드는 @Transactional이 무시됩니다.
     *
     * @param dtos 크롤링한 상품 DTO 목록
     * @param brand 브랜드 엔티티
     * @param brandName 브랜드명 (로그용)
     * @param style 스타일 (응답용)
     * @return 크롤링 결과
     */
    @Transactional
    protected CrawlingResult saveProductsBatch(List<CrawledProductDto> dtos, Brand brand, String brandName, String style) {
        if (dtos.isEmpty()) {
            log.warn("크롤링된 상품이 없습니다: {}", brandName);
            return new CrawlingResult(0, style, List.of());
        }

        // 1. 중복 체크 (배치 조회)
        List<String> productNames = dtos.stream()
                .map(CrawledProductDto::getName)
                .toList();

        Set<String> existingNames = productRepository.findExistingNamesByBrandIdAndNameIn(brand.getId(), productNames);

        log.debug("중복 상품 {}개 발견: {}", existingNames.size(), existingNames);

        // 2. 중복 제외 및 Entity 변환
        List<Product> newProducts = new ArrayList<>();
        List<FailedProductInfo> failedProducts = new ArrayList<>();

        for (CrawledProductDto dto : dtos) {
            try {
                // 중복 체크 (메모리에서 처리)
                if (existingNames.contains(dto.getName())) {
                    log.debug("이미 존재하는 상품: {}", dto.getName());
                    failedProducts.add(FailedProductInfo.of(dto.getName(), brandName, "중복 상품"));
                    continue;
                }

                Product product = convertToProduct(dto, brand);
                newProducts.add(product);

            } catch (Exception e) {
                String reason = e.getMessage() != null ? e.getMessage() : "알 수 없는 오류";
                log.error("상품 변환 실패: {}, 에러: {}", dto.getName(), reason);
                failedProducts.add(FailedProductInfo.of(dto.getName(), brandName, reason));
            }
        }

        // 3. 배치 저장
        if (!newProducts.isEmpty()) {
            productRepository.saveAll(newProducts);
            log.info("브랜드 상품 배치 저장 완료: {} ({}개)", brandName, newProducts.size());
        }

        log.info("브랜드 상품 저장 완료: {} (성공: {}개, 실패: {}개)", brandName, newProducts.size(), failedProducts.size());
        return new CrawlingResult(newProducts.size(), style, failedProducts);
    }

    /**
     * 크롤링 결과 (내부용)
     */
    public record CrawlingResult(int savedCount, String style, List<FailedProductInfo> failedProducts) {
    }

    /**
     * DB에 등록된 모든 브랜드의 상품을 크롤링하여 저장
     * 각 브랜드별로 독립적인 트랜잭션으로 처리됩니다.
     *
     * @param perBrandLimit 브랜드당 크롤링할 상품 수 (의류는 3의 배수로 자동 조정)
     * @return 크롤링 결과 (저장된 상품 수, 실패 목록)
     */
    public CrawlingResult crawlAndSaveAllProducts(int perBrandLimit) {
        log.info("전체 브랜드 상품 크롤링 시작: 브랜드당 {}개", perBrandLimit);

        // DB에 등록된 모든 브랜드 조회
        List<Brand> allBrands = brandRepository.findAll();

        if (allBrands.isEmpty()) {
            log.warn("등록된 브랜드가 없습니다");
            return new CrawlingResult(0, null, List.of());
        }

        log.info("총 {}개 브랜드 크롤링 예정 (CLOTHING: {}개, SHOES: {}개)",
                allBrands.size(),
                allBrands.stream().filter(b -> "CLOTHING".equals(b.getBrandType())).count(),
                allBrands.stream().filter(b -> "SHOES".equals(b.getBrandType())).count());

        int totalSaved = 0;
        List<FailedProductInfo> allFailedProducts = new ArrayList<>();
        int successBrandCount = 0;
        int failedBrandCount = 0;

        // 각 브랜드별로 크롤링
        for (Brand brand : allBrands) {
            try {
                log.info("브랜드 크롤링 시작: {} ({})", brand.getName(), brand.getBrandType());

                CrawlingResult result = crawlAndSaveBrandProducts(
                    brand.getName(),
                    brand.getBrandType(),
                    perBrandLimit
                );

                totalSaved += result.savedCount();
                allFailedProducts.addAll(result.failedProducts());
                successBrandCount++;

                log.info("브랜드 크롤링 완료: {} (성공: {}개, 실패: {}개)",
                        brand.getName(), result.savedCount(), result.failedProducts().size());

                // 브랜드 간 크롤링 간격 (API 부하 방지)
                Thread.sleep(2000);

            } catch (InterruptedException e) {
                log.error("크롤링 대기 중 인터럽트 발생", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                String reason = e.getMessage() != null ? e.getMessage() : "알 수 없는 오류";
                log.error("브랜드 크롤링 실패: {} ({}), 에러: {}", brand.getName(), brand.getBrandType(), reason, e);
                allFailedProducts.add(FailedProductInfo.of(
                    "브랜드 크롤링 실패",
                    brand.getName(),
                    reason
                ));
                failedBrandCount++;
            }
        }

        log.info("전체 브랜드 크롤링 완료: 총 {}개 저장, 총 {}개 실패, 성공 브랜드 {}개, 실패 브랜드 {}개",
                totalSaved, allFailedProducts.size(), successBrandCount, failedBrandCount);

        return new CrawlingResult(totalSaved, null, allFailedProducts);
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

        // TODO: 파이썬 서버에서 누끼 이미지를 받아와야 함 (현재는 임시로 thumbnailImageUrl 사용)
        String removedBackgroundImageUrl = dto.getRemovedBackgroundImageUrl() != null
                ? dto.getRemovedBackgroundImageUrl()
                : thumbnailImageUrl; // 임시: 파이썬 API 구현 전까지 썸네일 사용

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
                .removedBackgroundImageUrl(removedBackgroundImageUrl)
                .review(dto.getReview())
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

}
