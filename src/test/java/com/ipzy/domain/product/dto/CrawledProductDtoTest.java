package com.ipzy.domain.product.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CrawledProductDto의 필드가 null 처리되는지 확인하는 테스트
 */
class CrawledProductDtoTest {

    @Test
    @DisplayName("필수 필드가 모두 있는 경우")
    void allFieldsPresent() {
        // given & when
        CrawledProductDto dto = CrawledProductDto.builder()
                .brandName("나이키")
                .name("에어포스 1")
                .category("SHOES")
                .subCategory("스니커즈")
                .price(119000)
                .originalPrice(119000)
                .discountPercent(0)
                .thumbnailImageUrl("https://example.com/image.jpg")
                .review("클래식한 스니커즈")
                .colors(List.of("white", "black"))
                .purchaseUrl("https://example.com/product")
                .build();

        // then
        assertThat(dto.getBrandName()).isEqualTo("나이키");
        assertThat(dto.getName()).isEqualTo("에어포스 1");
        assertThat(dto.getCategory()).isEqualTo("SHOES");
        assertThat(dto.getSubCategory()).isEqualTo("스니커즈");
        assertThat(dto.getPrice()).isEqualTo(119000);
        assertThat(dto.getOriginalPrice()).isEqualTo(119000);
        assertThat(dto.getDiscountPercent()).isEqualTo(0);
        assertThat(dto.getThumbnailImageUrl()).isNotBlank();
        assertThat(dto.getReview()).isNotBlank();
        assertThat(dto.getColors()).hasSize(2);
        assertThat(dto.getPurchaseUrl()).isNotBlank();
    }

    @Test
    @DisplayName("원가가 null인 경우 - ProductCrawlingService에서 price로 대체되어야 함")
    void originalPriceIsNull() {
        // given & when
        CrawledProductDto dto = CrawledProductDto.builder()
                .brandName("아디다스")
                .name("스탠스미스")
                .category("SHOES")
                .subCategory("")
                .price(99000)
                .originalPrice(null) // null
                .discountPercent(0)
                .thumbnailImageUrl("https://example.com/image.jpg")
                .build();

        // then
        assertThat(dto.getOriginalPrice()).isNull();
        System.out.println("⚠️ DTO의 originalPrice가 null입니다.");
        System.out.println("→ ProductCrawlingService.convertToProduct()에서 price로 대체됩니다 (241번 라인)");
    }

    @Test
    @DisplayName("할인율이 null인 경우 - ProductCrawlingService에서 0으로 대체되어야 함")
    void discountPercentIsNull() {
        // given & when
        CrawledProductDto dto = CrawledProductDto.builder()
                .brandName("유니클로")
                .name("슈프림 티셔츠")
                .category("TOP")
                .subCategory("")
                .price(14900)
                .originalPrice(14900)
                .discountPercent(null) // null
                .thumbnailImageUrl("https://example.com/image.jpg")
                .build();

        // then
        assertThat(dto.getDiscountPercent()).isNull();
        System.out.println("⚠️ DTO의 discountPercent가 null입니다.");
        System.out.println("→ ProductCrawlingService.convertToProduct()에서 0으로 대체됩니다 (242번 라인)");
    }

    @Test
    @DisplayName("서브카테고리가 빈 문자열인 경우")
    void subCategoryIsEmpty() {
        // given & when
        CrawledProductDto dto = CrawledProductDto.builder()
                .brandName("무신사 스탠다드")
                .name("오버핏 셔츠")
                .category("TOP")
                .subCategory("") // 빈 문자열
                .price(39000)
                .originalPrice(39000)
                .discountPercent(0)
                .thumbnailImageUrl("https://example.com/image.jpg")
                .build();

        // then
        assertThat(dto.getSubCategory()).isEmpty();
        System.out.println("⚠️ 의류 상품의 subCategory가 빈 문자열입니다.");
        System.out.println("→ MusinsaCrawlerService 249번 라인에서 빈 문자열로 하드코딩되어 있습니다.");
        System.out.println("→ 실제 서브카테고리 정보(반팔티, 청바지, 패딩 등)를 파싱하도록 개선 필요!");
    }

    @Test
    @DisplayName("할인 상품 - 원가가 가격보다 큰 경우")
    void discountedProduct() {
        // given & when
        CrawledProductDto dto = CrawledProductDto.builder()
                .brandName("노스페이스")
                .name("눕시 다운 재킷")
                .category("OUTER")
                .subCategory("")
                .price(295200)
                .originalPrice(369000)
                .discountPercent(20)
                .thumbnailImageUrl("https://example.com/image.jpg")
                .build();

        // then
        assertThat(dto.getOriginalPrice()).isGreaterThan(dto.getPrice());
        assertThat(dto.getDiscountPercent()).isGreaterThan(0);

        // 할인율 계산 검증
        int expectedPrice = dto.getOriginalPrice() * (100 - dto.getDiscountPercent()) / 100;
        assertThat(dto.getPrice()).isEqualTo(expectedPrice);

        System.out.println("할인 상품 정보:");
        System.out.println("- 원가: " + dto.getOriginalPrice() + "원");
        System.out.println("- 할인가: " + dto.getPrice() + "원");
        System.out.println("- 할인율: " + dto.getDiscountPercent() + "%");
    }

    @Test
    @DisplayName("색상 정보가 null인 경우")
    void colorsIsNull() {
        // given & when
        CrawledProductDto dto = CrawledProductDto.builder()
                .brandName("브랜드")
                .name("상품명")
                .category("TOP")
                .price(50000)
                .colors(null) // null
                .thumbnailImageUrl("https://example.com/image.jpg")
                .build();

        // then
        assertThat(dto.getColors()).isNull();
        System.out.println("⚠️ DTO의 colors가 null입니다.");
        System.out.println("→ ProductCrawlingService.convertToProduct()에서 null로 그대로 전달됩니다 (255번 라인)");
        System.out.println("→ Product 엔티티는 colors가 null 가능합니다.");
    }

    @Test
    @DisplayName("리뷰 정보가 null인 경우")
    void reviewIsNull() {
        // given & when
        CrawledProductDto dto = CrawledProductDto.builder()
                .brandName("브랜드")
                .name("상품명")
                .category("TOP")
                .price(50000)
                .review(null) // null
                .thumbnailImageUrl("https://example.com/image.jpg")
                .build();

        // then
        assertThat(dto.getReview()).isNull();
        System.out.println("⚠️ DTO의 review가 null입니다.");
        System.out.println("→ Product 엔티티의 review가 null 가능합니다.");
    }

    @Test
    @DisplayName("구매 URL이 null인 경우")
    void purchaseUrlIsNull() {
        // given & when
        CrawledProductDto dto = CrawledProductDto.builder()
                .brandName("브랜드")
                .name("상품명")
                .category("TOP")
                .price(50000)
                .purchaseUrl(null) // null
                .thumbnailImageUrl("https://example.com/image.jpg")
                .build();

        // then
        assertThat(dto.getPurchaseUrl()).isNull();
        System.out.println("⚠️ DTO의 purchaseUrl이 null입니다.");
        System.out.println("→ Product 엔티티는 purchaseUrl이 null 가능합니다.");
    }
}
