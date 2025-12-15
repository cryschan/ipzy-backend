package com.ipzy.domain.product.service;

import com.ipzy._global.common.enums.ClothingCategory;
import com.ipzy.domain.product.dto.ProductResponse;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    ProductService productService;

    private Brand brand;
    private Product activeProduct;
    private Product inactiveProduct;
    private Product deletedProduct;

    @BeforeEach
    void setUp() {
        brand = Brand.builder()
                .name("Nike")
                .primaryStyle("sneakers")
                .brandType("SHOES")
                .build();

        activeProduct = Product.builder()
                .brand(brand)
                .name("에어포스 1")
                .category(ClothingCategory.SHOES)
                .subCategory("스니커즈")
                .primaryStyle("sneakers")
                .price(129000)
                .originalPrice(159000)
                .discountPercent(18)
                .thumbnailImageUrl("https://example.com/image1.jpg")
                .description("리뷰: 100개")
                .colors(new String[]{"블랙", "화이트"})
                .seasons(new String[]{"ALL"})
                .isActive(true)
                .purchaseUrl("https://www.musinsa.com/...")
                .build();

        inactiveProduct = Product.builder()
                .brand(brand)
                .name("조던 1")
                .category(ClothingCategory.SHOES)
                .subCategory("스니커즈")
                .primaryStyle("sneakers")
                .price(159000)
                .thumbnailImageUrl("https://example.com/image2.jpg")
                .seasons(new String[]{"2024_FW"})
                .isActive(false)
                .build();

        deletedProduct = Product.builder()
                .brand(brand)
                .name("삭제된 상품")
                .category(ClothingCategory.SHOES)
                .price(100000)
                .thumbnailImageUrl("https://example.com/deleted.jpg")
                .isActive(false)
                .build();
        deletedProduct.softDelete();
    }

    @Nested
    @DisplayName("전체 상품 조회")
    class GetAllProducts {

        @Test
        @DisplayName("성공 - 삭제되지 않은 모든 상품을 조회한다")
        void success() {
            // given
            given(productRepository.findAll()).willReturn(List.of(activeProduct, inactiveProduct, deletedProduct));

            // when
            List<ProductResponse> result = productService.getAllProducts();

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ProductResponse::getName)
                    .containsExactlyInAnyOrder("에어포스 1", "조던 1");
            assertThat(result).extracting(ProductResponse::getName)
                    .doesNotContain("삭제된 상품");
        }

        @Test
        @DisplayName("성공 - 상품이 없으면 빈 리스트를 반환한다")
        void success_emptyList() {
            // given
            given(productRepository.findAll()).willReturn(List.of());

            // when
            List<ProductResponse> result = productService.getAllProducts();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("성공 - 활성/비활성 상품 모두 포함된다")
        void success_includesBothActiveAndInactive() {
            // given
            given(productRepository.findAll()).willReturn(List.of(activeProduct, inactiveProduct));

            // when
            List<ProductResponse> result = productService.getAllProducts();

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ProductResponse::getIsActive)
                    .containsExactlyInAnyOrder(true, false);
        }
    }

    @Nested
    @DisplayName("활성 상품 조회")
    class GetActiveProducts {

        @Test
        @DisplayName("성공 - 활성화된 상품만 조회한다")
        void success() {
            // given
            given(productRepository.findByIsActiveTrue()).willReturn(List.of(activeProduct));

            // when
            List<ProductResponse> result = productService.getActiveProducts();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("에어포스 1");
            assertThat(result.get(0).getIsActive()).isTrue();
        }

        @Test
        @DisplayName("성공 - 삭제된 활성 상품은 제외된다")
        void success_excludesDeletedProducts() {
            // given
            Product deletedActiveProduct = Product.builder()
                    .brand(brand)
                    .name("삭제된 활성 상품")
                    .category(ClothingCategory.SHOES)
                    .price(100000)
                    .thumbnailImageUrl("https://example.com/deleted.jpg")
                    .isActive(true)
                    .build();
            deletedActiveProduct.softDelete();

            given(productRepository.findByIsActiveTrue()).willReturn(List.of(activeProduct, deletedActiveProduct));

            // when
            List<ProductResponse> result = productService.getActiveProducts();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("에어포스 1");
        }

        @Test
        @DisplayName("성공 - 활성 상품이 없으면 빈 리스트를 반환한다")
        void success_emptyList() {
            // given
            given(productRepository.findByIsActiveTrue()).willReturn(List.of());

            // when
            List<ProductResponse> result = productService.getActiveProducts();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("ProductResponse 변환")
    class ProductResponseConversion {

        @Test
        @DisplayName("성공 - Product 엔티티가 ProductResponse로 올바르게 변환된다")
        void success_conversion() {
            // when
            ProductResponse response = ProductResponse.from(activeProduct);

            // then
            assertThat(response.getId()).isEqualTo(activeProduct.getId());
            assertThat(response.getBrandName()).isEqualTo(brand.getName());
            assertThat(response.getName()).isEqualTo(activeProduct.getName());
            assertThat(response.getCategory()).isEqualTo(activeProduct.getCategory());
            assertThat(response.getSubCategory()).isEqualTo(activeProduct.getSubCategory());
            assertThat(response.getPrimaryStyle()).isEqualTo(activeProduct.getPrimaryStyle());
            assertThat(response.getPrice()).isEqualTo(activeProduct.getPrice());
            assertThat(response.getOriginalPrice()).isEqualTo(activeProduct.getOriginalPrice());
            assertThat(response.getDiscountPercent()).isEqualTo(activeProduct.getDiscountPercent());
            assertThat(response.getThumbnailImageUrl()).isEqualTo(activeProduct.getThumbnailImageUrl());
            assertThat(response.getColors()).containsExactly("블랙", "화이트");
            assertThat(response.getSeasons()).containsExactly("ALL");
            assertThat(response.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("성공 - colors/seasons가 null이면 빈 리스트로 변환된다")
        void success_nullArraysToEmptyList() {
            // given
            Product productWithNulls = Product.builder()
                    .brand(brand)
                    .name("테스트 상품")
                    .category(ClothingCategory.SHOES)
                    .price(100000)
                    .thumbnailImageUrl("https://example.com/test.jpg")
                    .colors(null)
                    .seasons(null)
                    .isActive(true)
                    .build();

            // when
            ProductResponse response = ProductResponse.from(productWithNulls);

            // then
            assertThat(response.getColors()).isEmpty();
            assertThat(response.getSeasons()).isEmpty();
        }
    }
}
