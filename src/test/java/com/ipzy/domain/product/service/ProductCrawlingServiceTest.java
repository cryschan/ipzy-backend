package com.ipzy.domain.product.service;

import com.ipzy._global.common.enums.ClothingCategory;
import com.ipzy.domain.product.dto.CrawledProductDto;
import com.ipzy.domain.product.dto.FailedProductInfo;
import com.ipzy.domain.product.entity.Brand;
import com.ipzy.domain.product.entity.Product;
import com.ipzy.domain.product.exception.ProductErrorCode;
import com.ipzy.domain.product.exception.ProductException;
import com.ipzy.domain.product.repository.BrandRepository;
import com.ipzy.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductCrawlingServiceTest {

    @Mock
    MusinsaCrawlerService musinsaCrawlerService;

    @Mock
    BrandRepository brandRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    ProductSeasonService productSeasonService;

    @InjectMocks
    ProductCrawlingService productCrawlingService;

    private Brand clothingBrand;
    private Brand shoesBrand;

    @BeforeEach
    void setUp() {
        clothingBrand = Brand.builder()
                .name("Musinsa Standard")
                .primaryStyle("minimalist")
                .brandType("CLOTHING")
                .build();
        ReflectionTestUtils.setField(clothingBrand, "id", 10L);

        shoesBrand = Brand.builder()
                .name("Nike")
                .primaryStyle("sneakers")
                .brandType("SHOES")
                .build();
        ReflectionTestUtils.setField(shoesBrand, "id", 20L);
    }

    @Nested
    @DisplayName("crawlAndSaveBrandProducts는")
    class CrawlAndSaveBrandProducts {

        @Test
        @DisplayName("실패 - brandType이 null/blank면 INVALID_BRAND_TYPE 예외")
        void fail_whenBrandTypeIsNullOrBlank() {
            assertThatThrownBy(() -> productCrawlingService.crawlAndSaveBrandProducts("Nike", null, 1))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining(ProductErrorCode.INVALID_BRAND_TYPE.getMessage());

            assertThatThrownBy(() -> productCrawlingService.crawlAndSaveBrandProducts("Nike", " ", 1))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining(ProductErrorCode.INVALID_BRAND_TYPE.getMessage());
        }

        @Test
        @DisplayName("실패 - 브랜드가 존재하지 않으면 BRAND_NOT_FOUND 예외")
        void fail_whenBrandNotFound() {
            // given
            given(brandRepository.findByNameAndBrandType("Nike", "SHOES")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productCrawlingService.crawlAndSaveBrandProducts("Nike", "SHOES", 1))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining(ProductErrorCode.BRAND_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패 - DB에 저장된 브랜드 타입이 비어있으면 INVALID_BRAND_TYPE 예외")
        void fail_whenStoredBrandTypeBlank() {
            // given
            Brand invalidBrand = Brand.builder()
                    .name("Nike")
                    .primaryStyle("sneakers")
                    .brandType("")
                    .build();
            given(brandRepository.findByNameAndBrandType("Nike", "SHOES")).willReturn(Optional.of(invalidBrand));

            // when & then
            assertThatThrownBy(() -> productCrawlingService.crawlAndSaveBrandProducts("Nike", "SHOES", 1))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining(ProductErrorCode.INVALID_BRAND_TYPE.getMessage());
        }

        @Test
        @DisplayName("성공 - 의류 브랜드는 crawlBrandProducts를 호출하고 저장한다")
        void success_clothingBrand() {
            // given
            given(brandRepository.findByNameAndBrandType(clothingBrand.getName(), "CLOTHING"))
                    .willReturn(Optional.of(clothingBrand));

            List<CrawledProductDto> crawled = List.of(
                    CrawledProductDto.builder()
                            .brandName(clothingBrand.getName())
                            .name("오버핏 티셔츠")
                            .category("TOP")
                            .subCategory("반팔티")
                            .price(19000)
                            .imageUrl("https://example.com/p1.jpg")
                            .build()
            );
            given(musinsaCrawlerService.crawlBrandProducts(eq(clothingBrand.getName()), eq(clothingBrand.getPrimaryStyle()), anyInt()))
                    .willReturn(crawled);
            given(productRepository.findExistingNamesByBrandIdAndNameIn(eq(10L), anyList())).willReturn(Set.of());
            given(productSeasonService.getCurrentSeason()).willReturn("2025_SS");
            given(productSeasonService.determineSeasons(eq("TOP"), eq("2025_SS"))).willReturn(new String[]{"2025_SS"});

            // when
            ProductCrawlingService.CrawlingResult result =
                    productCrawlingService.crawlAndSaveBrandProducts(clothingBrand.getName(), "CLOTHING", 1);

            // then
            assertThat(result.savedCount()).isEqualTo(1);
            verify(musinsaCrawlerService).crawlBrandProducts(eq(clothingBrand.getName()), eq(clothingBrand.getPrimaryStyle()), anyInt());
            verify(musinsaCrawlerService, never()).crawlShoeProducts(eq(clothingBrand.getName()), eq(clothingBrand.getPrimaryStyle()), anyInt());
            verify(productRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("성공 - 신발 브랜드는 crawlShoeProducts를 호출하고 저장한다")
        void success_shoesBrand() {
            // given
            given(brandRepository.findByNameAndBrandType(shoesBrand.getName(), "SHOES"))
                    .willReturn(Optional.of(shoesBrand));

            List<CrawledProductDto> crawled = List.of(
                    CrawledProductDto.builder()
                            .brandName(shoesBrand.getName())
                            .name("에어포스 1")
                            .category("SHOES")
                            .subCategory("스니커즈")
                            .price(129000)
                            .imageUrl("https://example.com/p1.jpg")
                            .build()
            );
            given(musinsaCrawlerService.crawlShoeProducts(eq(shoesBrand.getName()), eq(shoesBrand.getPrimaryStyle()), anyInt()))
                    .willReturn(crawled);
            given(productRepository.findExistingNamesByBrandIdAndNameIn(eq(20L), anyList())).willReturn(Set.of());
            given(productSeasonService.getCurrentSeason()).willReturn("2025_FW");
            given(productSeasonService.determineSeasons(eq("SHOES"), eq("2025_FW"))).willReturn(new String[]{"ALL"});

            // when
            ProductCrawlingService.CrawlingResult result =
                    productCrawlingService.crawlAndSaveBrandProducts(shoesBrand.getName(), "SHOES", 1);

            // then
            assertThat(result.savedCount()).isEqualTo(1);
            verify(musinsaCrawlerService).crawlShoeProducts(eq(shoesBrand.getName()), eq(shoesBrand.getPrimaryStyle()), anyInt());
            verify(musinsaCrawlerService, never()).crawlBrandProducts(eq(shoesBrand.getName()), eq(shoesBrand.getPrimaryStyle()), anyInt());
            verify(productRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("성공 - 중복 상품은 저장하지 않고 실패 목록으로 반환한다")
        void success_duplicateProductsGoToFailedList() {
            // given
            given(brandRepository.findByNameAndBrandType(clothingBrand.getName(), "CLOTHING"))
                    .willReturn(Optional.of(clothingBrand));

            List<CrawledProductDto> crawled = List.of(
                    CrawledProductDto.builder()
                            .brandName(clothingBrand.getName())
                            .name("중복 상품")
                            .category("TOP")
                            .subCategory("반팔티")
                            .price(19000)
                            .imageUrl("https://example.com/dup.jpg")
                            .build(),
                    CrawledProductDto.builder()
                            .brandName(clothingBrand.getName())
                            .name("신규 상품")
                            .category("TOP")
                            .subCategory("반팔티")
                            .price(20000)
                            .imageUrl("https://example.com/new.jpg")
                            .build()
            );
            given(musinsaCrawlerService.crawlBrandProducts(eq(clothingBrand.getName()), eq(clothingBrand.getPrimaryStyle()), anyInt()))
                    .willReturn(crawled);
            given(productRepository.findExistingNamesByBrandIdAndNameIn(eq(10L), anyList())).willReturn(Set.of("중복 상품"));
            given(productSeasonService.getCurrentSeason()).willReturn("2025_SS");
            given(productSeasonService.determineSeasons(eq("TOP"), eq("2025_SS"))).willReturn(new String[]{"2025_SS"});

            // when
            ProductCrawlingService.CrawlingResult result =
                    productCrawlingService.crawlAndSaveBrandProducts(clothingBrand.getName(), "CLOTHING", 2);

            // then
            assertThat(result.savedCount()).isEqualTo(1);
            assertThat(result.failedProducts())
                    .extracting(FailedProductInfo::productName)
                    .containsExactly("중복 상품");
        }

        @Test
        @DisplayName("성공 - 원가/할인율/누끼 URL이 null이면 기본값으로 저장한다")
        void success_defaultsAppliedDuringConversion() {
            // given
            given(brandRepository.findByNameAndBrandType(clothingBrand.getName(), "CLOTHING"))
                    .willReturn(Optional.of(clothingBrand));

            CrawledProductDto dto = CrawledProductDto.builder()
                    .brandName(clothingBrand.getName())
                    .name("기본값 테스트 상품")
                    .category("TOP")
                    .subCategory("반팔티")
                    .price(15000)
                    .originalPrice(null)
                    .discountPercent(null)
                    .imageUrl("https://example.com/thumb.jpg")
                    .removedBackgroundImageUrl(null)
                    .build();

            given(musinsaCrawlerService.crawlBrandProducts(eq(clothingBrand.getName()), eq(clothingBrand.getPrimaryStyle()), anyInt()))
                    .willReturn(List.of(dto));
            given(productRepository.findExistingNamesByBrandIdAndNameIn(eq(10L), anyList())).willReturn(Set.of());
            given(productSeasonService.getCurrentSeason()).willReturn("2025_SS");
            given(productSeasonService.determineSeasons(eq("TOP"), eq("2025_SS"))).willReturn(new String[]{"2025_SS"});

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Product>> savedCaptor =
                    (ArgumentCaptor<List<Product>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);

            // when
            ProductCrawlingService.CrawlingResult result =
                    productCrawlingService.crawlAndSaveBrandProducts(clothingBrand.getName(), "CLOTHING", 1);

            // then
            assertThat(result.savedCount()).isEqualTo(1);
            verify(productRepository).saveAll(savedCaptor.capture());
            Product saved = savedCaptor.getValue().get(0);

            assertThat(saved.getCategory()).isEqualTo(ClothingCategory.TOP);
            assertThat(saved.getOriginalPrice()).isEqualTo(dto.getPrice());
            assertThat(saved.getDiscountPercent()).isEqualTo(0);
            assertThat(saved.getImageUrl()).isEqualTo(dto.getImageUrl());
            assertThat(saved.getRemovedBackgroundImageUrl()).isEqualTo(dto.getImageUrl());
            assertThat(saved.getSeasons()).containsExactly("2025_SS");
        }

        @Test
        @DisplayName("성공 - 가격이 유효하지 않은 상품은 실패 목록으로 반환하고 저장하지 않는다")
        void success_invalidPriceGoesToFailedList() {
            // given
            given(brandRepository.findByNameAndBrandType(clothingBrand.getName(), "CLOTHING"))
                    .willReturn(Optional.of(clothingBrand));

            CrawledProductDto invalid = CrawledProductDto.builder()
                    .brandName(clothingBrand.getName())
                    .name("가격 없음")
                    .category("TOP")
                    .subCategory("반팔티")
                    .price(0)
                    .imageUrl("https://example.com/invalid.jpg")
                    .build();

            given(musinsaCrawlerService.crawlBrandProducts(eq(clothingBrand.getName()), eq(clothingBrand.getPrimaryStyle()), anyInt()))
                    .willReturn(List.of(invalid));
            given(productRepository.findExistingNamesByBrandIdAndNameIn(eq(10L), anyList())).willReturn(Set.of());

            // when
            ProductCrawlingService.CrawlingResult result =
                    productCrawlingService.crawlAndSaveBrandProducts(clothingBrand.getName(), "CLOTHING", 1);

            // then
            assertThat(result.savedCount()).isEqualTo(0);
            assertThat(result.failedProducts()).hasSize(1);
            verify(productRepository, never()).saveAll(anyList());
        }
    }
}
