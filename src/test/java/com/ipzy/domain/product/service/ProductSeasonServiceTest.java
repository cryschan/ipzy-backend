package com.ipzy.domain.product.service;

import com.ipzy._global.common.enums.ClothingCategory;
import com.ipzy.domain.product.entity.Brand;
import com.ipzy.domain.product.entity.Product;
import com.ipzy.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductSeasonServiceTest {

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    ProductSeasonService productSeasonService;

    private Brand brand;
    private Product ssProduct;
    private Product fwProduct;
    private Product allSeasonProduct;

    @BeforeEach
    void setUp() {
        brand = Brand.builder()
                .name("테스트브랜드")
                .primaryStyle("캐주얼")
                .brandType("국내")
                .build();

        // SS 상품
        ssProduct = Product.builder()
                .brand(brand)
                .name("여름 티셔츠")
                .category(ClothingCategory.TOP)
                .price(30000)
                .imageUrl("https://example.com/image1.jpg")
                .removedBackgroundImageUrl("https://example.com/image1_nobg.jpg")
                .seasons(new String[]{"2025_SS"})
                .isActive(false)
                .build();

        // FW 상품
        fwProduct = Product.builder()
                .brand(brand)
                .name("겨울 패딩")
                .category(ClothingCategory.OUTER)
                .price(200000)
                .imageUrl("https://example.com/image2.jpg")
                .removedBackgroundImageUrl("https://example.com/image2_nobg.jpg")
                .seasons(new String[]{"2025_FW"})
                .isActive(false)
                .build();

        // 사계절 상품
        allSeasonProduct = Product.builder()
                .brand(brand)
                .name("사계절 청바지")
                .category(ClothingCategory.BOTTOM)
                .price(80000)
                .imageUrl("https://example.com/image3.jpg")
                .removedBackgroundImageUrl("https://example.com/image3_nobg.jpg")
                .seasons(new String[]{"ALL"})
                .isActive(false)
                .build();
    }

    @Nested
    @DisplayName("현재 시즌 조회")
    class GetCurrentSeason {

        @Test
        @DisplayName("성공 - 현재 시즌을 반환한다")
        void success() {
            // when
            String currentSeason = productSeasonService.getCurrentSeason();

            // then
            assertThat(currentSeason).isNotNull();
            assertThat(currentSeason).matches("\\d{4}_(SS|FW)");

            // 현재 월에 따라 SS 또는 FW 확인
            LocalDate now = LocalDate.now();
            int month = now.getMonthValue();
            if (month >= 3 && month <= 8) {
                assertThat(currentSeason).endsWith("_SS");
            } else {
                assertThat(currentSeason).endsWith("_FW");
            }
        }

        @Test
        @DisplayName("성공 - SS 시즌 기간 확인 (3월~8월)")
        void success_ssSeasonPeriod() {
            // when
            String season = productSeasonService.getCurrentSeason();

            // then
            int month = LocalDate.now().getMonthValue();
            if (month >= 3 && month <= 8) {
                assertThat(season).contains("SS");
            }
        }

        @Test
        @DisplayName("성공 - FW 시즌 기간 확인 (9월~2월)")
        void success_fwSeasonPeriod() {
            // when
            String season = productSeasonService.getCurrentSeason();

            // then
            int month = LocalDate.now().getMonthValue();
            if (month >= 9 || month <= 2) {
                assertThat(season).contains("FW");
            }
        }
    }

    @Nested
    @DisplayName("시즌별 상품 활성화")
    class ActivateSeasonalProducts {

        @Test
        @DisplayName("성공 - 현재 시즌 상품만 활성화한다")
        void success() {
            // given
            String currentSeason = productSeasonService.getCurrentSeason();
            Product currentSeasonProduct;

            if (currentSeason.endsWith("SS")) {
                currentSeasonProduct = ssProduct;
            } else {
                currentSeasonProduct = fwProduct;
            }

            List<Product> allProducts = List.of(ssProduct, fwProduct, allSeasonProduct);
            given(productRepository.findAll()).willReturn(allProducts);

            // when
            int activatedCount = productSeasonService.activateSeasonalProducts();

            // then
            assertThat(activatedCount).isGreaterThan(0);
            verify(productRepository).findAll();

            // 사계절 상품은 항상 활성화
            assertThat(allSeasonProduct.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("성공 - 삭제된 상품은 활성화하지 않는다")
        void success_skipDeletedProducts() {
            // given
            Product deletedProduct = Product.builder()
                    .brand(brand)
                    .name("삭제된 상품")
                    .category(ClothingCategory.TOP)
                    .price(50000)
                    .imageUrl("https://example.com/deleted.jpg")
                    .removedBackgroundImageUrl("https://example.com/deleted_nobg.jpg")
                    .seasons(new String[]{productSeasonService.getCurrentSeason()})
                    .isActive(false)
                    .build();
            deletedProduct.softDelete();

            List<Product> allProducts = List.of(deletedProduct, allSeasonProduct);
            given(productRepository.findAll()).willReturn(allProducts);

            // when
            int activatedCount = productSeasonService.activateSeasonalProducts();

            // then
            assertThat(deletedProduct.getIsActive()).isFalse();
            assertThat(deletedProduct.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("성공 - 사계절 상품(ALL)은 항상 활성화")
        void success_allSeasonProducts() {
            // given
            Product allSeasonShoes = Product.builder()
                    .brand(brand)
                    .name("사계절 운동화")
                    .category(ClothingCategory.SHOES)
                    .price(100000)
                    .imageUrl("https://example.com/shoes.jpg")
                    .removedBackgroundImageUrl("https://example.com/shoes_nobg.jpg")
                    .seasons(new String[]{"ALL"})
                    .isActive(false)
                    .build();

            given(productRepository.findAll()).willReturn(List.of(allSeasonShoes));

            // when
            productSeasonService.activateSeasonalProducts();

            // then
            assertThat(allSeasonShoes.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("성공 - seasons가 null인 상품은 사계절로 간주")
        void success_nullSeasonsProducts() {
            // given
            Product nullSeasonProduct = Product.builder()
                    .brand(brand)
                    .name("시즌 미지정 상품")
                    .category(ClothingCategory.ACCESSORY)
                    .price(20000)
                    .imageUrl("https://example.com/accessory.jpg")
                    .removedBackgroundImageUrl("https://example.com/accessory_nobg.jpg")
                    .seasons(null)
                    .isActive(false)
                    .build();

            given(productRepository.findAll()).willReturn(List.of(nullSeasonProduct));

            // when
            productSeasonService.activateSeasonalProducts();

            // then
            assertThat(nullSeasonProduct.getIsActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("시즌 정보 결정")
    class DetermineSeasons {

        @Test
        @DisplayName("성공 - 아우터는 현재 시즌 반환")
        void success_outerCategory() {
            // given
            String currentSeason = "2025_FW";

            // when
            String[] seasons = productSeasonService.determineSeasons("OUTER", currentSeason);

            // then
            assertThat(seasons).hasSize(1);
            assertThat(seasons[0]).isEqualTo(currentSeason);
        }

        @Test
        @DisplayName("성공 - 상의는 현재 시즌 반환")
        void success_topCategory() {
            // given
            String currentSeason = "2025_SS";

            // when
            String[] seasons = productSeasonService.determineSeasons("TOP", currentSeason);

            // then
            assertThat(seasons).hasSize(1);
            assertThat(seasons[0]).isEqualTo(currentSeason);
        }

        @Test
        @DisplayName("성공 - 신발은 사계절(ALL) 반환")
        void success_shoesCategory() {
            // given
            String currentSeason = "2025_SS";

            // when
            String[] seasons = productSeasonService.determineSeasons("SHOES", currentSeason);

            // then
            assertThat(seasons).hasSize(1);
            assertThat(seasons[0]).isEqualTo("ALL");
        }

        @Test
        @DisplayName("성공 - 액세서리는 사계절(ALL) 반환")
        void success_accessoryCategory() {
            // given
            String currentSeason = "2025_FW";

            // when
            String[] seasons = productSeasonService.determineSeasons("ACCESSORY", currentSeason);

            // then
            assertThat(seasons).hasSize(1);
            assertThat(seasons[0]).isEqualTo("ALL");
        }

        @Test
        @DisplayName("성공 - 카테고리가 null이면 현재 시즌 반환")
        void success_nullCategory() {
            // given
            String currentSeason = "2025_SS";

            // when
            String[] seasons = productSeasonService.determineSeasons(null, currentSeason);

            // then
            assertThat(seasons).hasSize(1);
            assertThat(seasons[0]).isEqualTo(currentSeason);
        }
    }

    @Nested
    @DisplayName("시즌 전환")
    class SeasonTransition {

        @Test
        @DisplayName("성공 - 시즌 전환 시 상품 활성화를 수행한다")
        void success() {
            // given
            List<Product> products = new ArrayList<>();
            products.add(ssProduct);
            products.add(fwProduct);
            products.add(allSeasonProduct);
            given(productRepository.findAll()).willReturn(products);

            // when
            productSeasonService.seasonTransition();

            // then
            verify(productRepository).findAll();
        }
    }
}
