package com.ipzy.domain.product.service;

import com.ipzy.domain.product.dto.BrandRequest;
import com.ipzy.domain.product.dto.BrandResponse;
import com.ipzy.domain.product.dto.BrandValidationResult;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    BrandRepository brandRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    MusinsaCrawlerService musinsaCrawlerService;

    @InjectMocks
    BrandService brandService;

    private Brand brand;
    private BrandRequest brandRequest;

    @BeforeEach
    void setUp() {
        brand = Brand.builder()
                .name("나이키")
                .logoUrl("https://example.com/nike-logo.png")
                .primaryStyle("스포츠")
                .brandType("글로벌")
                .build();

        brandRequest = BrandRequest.builder()
                .name("아디다스")
                .primaryStyle("스포츠")
                .brandType("글로벌")
                .logoUrl("https://example.com/adidas-logo.png")
                .validateMusinsa(false)
                .build();
    }

    @Nested
    @DisplayName("브랜드 등록")
    class CreateBrand {

        @Test
        @DisplayName("성공 - 브랜드를 등록한다")
        void success() {
            // given
            given(brandRepository.existsByNameAndBrandType(brandRequest.getName(), brandRequest.getBrandType())).willReturn(false);
            given(brandRepository.save(any(Brand.class))).willReturn(brand);

            // when
            BrandResponse result = brandService.createBrand(brandRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(brand.getName());
            verify(brandRepository).save(any(Brand.class));
        }

        @Test
        @DisplayName("실패 - 이미 존재하는 브랜드")
        void fail_brandAlreadyExists() {
            // given
            given(brandRepository.existsByNameAndBrandType(brandRequest.getName(), brandRequest.getBrandType())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> brandService.createBrand(brandRequest))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining(ProductErrorCode.BRAND_ALREADY_EXISTS.getMessage());
        }

        @Test
        @DisplayName("성공 - 무신사 검증과 함께 브랜드 등록")
        void success_withMusinsaValidation() {
            // given
            BrandRequest requestWithValidation = BrandRequest.builder()
                    .name("나이키")
                    .primaryStyle("스포츠")
                    .brandType("글로벌")
                    .validateMusinsa(true)
                    .build();

            BrandValidationResult validationResult = BrandValidationResult.success("nike");

            given(brandRepository.existsByNameAndBrandType(requestWithValidation.getName(), requestWithValidation.getBrandType())).willReturn(false);
            given(musinsaCrawlerService.validateBrandExists(requestWithValidation.getName())).willReturn(validationResult);
            given(brandRepository.save(any(Brand.class))).willReturn(brand);

            // when
            BrandResponse result = brandService.createBrand(requestWithValidation);

            // then
            assertThat(result).isNotNull();
            verify(musinsaCrawlerService).validateBrandExists(requestWithValidation.getName());
            verify(brandRepository).save(any(Brand.class));
        }

        @Test
        @DisplayName("실패 - 무신사에 존재하지 않는 브랜드")
        void fail_brandNotFoundInMusinsa() {
            // given
            BrandRequest requestWithValidation = BrandRequest.builder()
                    .name("없는브랜드")
                    .primaryStyle("스포츠")
                    .brandType("글로벌")
                    .validateMusinsa(true)
                    .build();

            BrandValidationResult validationResult = BrandValidationResult.failure("unknown", "브랜드를 찾을 수 없습니다");

            given(brandRepository.existsByNameAndBrandType(requestWithValidation.getName(), requestWithValidation.getBrandType())).willReturn(false);
            given(musinsaCrawlerService.validateBrandExists(requestWithValidation.getName())).willReturn(validationResult);

            // when & then
            assertThatThrownBy(() -> brandService.createBrand(requestWithValidation))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining(ProductErrorCode.BRAND_NOT_FOUND_IN_MUSINSA.getMessage());
        }
    }

    @Nested
    @DisplayName("브랜드 수정")
    class UpdateBrand {

        @Test
        @DisplayName("성공 - 브랜드 정보를 수정한다 (이름과 타입 동일)")
        void success() {
            // given
            Long brandId = 1L;
            BrandRequest updateRequest = BrandRequest.builder()
                    .name("나이키")
                    .primaryStyle("애슬레저")
                    .brandType("글로벌")
                    .logoUrl("https://example.com/new-logo.png")
                    .build();

            given(brandRepository.findById(brandId)).willReturn(Optional.of(brand));
            // 이름과 타입이 동일하면 중복 체크를 하지 않으므로 stub 제거

            // when
            BrandResponse result = brandService.updateBrand(brandId, updateRequest);

            // then
            assertThat(result).isNotNull();
            verify(brandRepository).findById(brandId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 브랜드")
        void fail_brandNotFound() {
            // given
            Long brandId = 999L;
            given(brandRepository.findById(brandId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> brandService.updateBrand(brandId, brandRequest))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining(ProductErrorCode.BRAND_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패 - 이름/타입 변경 시 중복된 브랜드 존재")
        void fail_duplicateBrandWhenUpdating() {
            // given
            Long brandId = 1L;
            BrandRequest updateRequest = BrandRequest.builder()
                    .name("다른브랜드")
                    .primaryStyle("스포츠")
                    .brandType("글로벌")
                    .build();

            given(brandRepository.findById(brandId)).willReturn(Optional.of(brand));
            given(brandRepository.existsByNameAndBrandType(updateRequest.getName(), updateRequest.getBrandType())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> brandService.updateBrand(brandId, updateRequest))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining(ProductErrorCode.BRAND_ALREADY_EXISTS.getMessage());
        }
    }

    @Nested
    @DisplayName("브랜드 삭제")
    class DeleteBrand {

        @Test
        @DisplayName("성공 - 브랜드를 삭제한다")
        void success() {
            // given
            Long brandId = 1L;
            given(brandRepository.findById(brandId)).willReturn(Optional.of(brand));
            given(productRepository.findByBrandId(brandId)).willReturn(List.of());

            // when
            brandService.deleteBrand(brandId);

            // then
            verify(brandRepository).delete(brand);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 브랜드")
        void fail_brandNotFound() {
            // given
            Long brandId = 999L;
            given(brandRepository.findById(brandId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> brandService.deleteBrand(brandId))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining(ProductErrorCode.BRAND_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패 - 연관된 상품이 존재하는 브랜드")
        void fail_brandHasProducts() {
            // given
            Long brandId = 1L;
            Product product = Product.builder()
                    .brand(brand)
                    .name("테스트 상품")
                    .price(10000)
                    .imageUrl("https://example.com/test.jpg")
                    .removedBackgroundImageUrl("https://example.com/test_nobg.jpg")
                    .build();
            given(brandRepository.findById(brandId)).willReturn(Optional.of(brand));
            given(productRepository.findByBrandId(brandId)).willReturn(List.of(product));

            // when & then
            assertThatThrownBy(() -> brandService.deleteBrand(brandId))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining("상품이 연결되어 있습니다");
        }
    }

    @Nested
    @DisplayName("브랜드 조회")
    class GetBrand {

        @Test
        @DisplayName("성공 - 브랜드를 단건 조회한다")
        void success_getSingleBrand() {
            // given
            Long brandId = 1L;
            given(brandRepository.findById(brandId)).willReturn(Optional.of(brand));

            // when
            BrandResponse result = brandService.getBrand(brandId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(brand.getName());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 브랜드")
        void fail_brandNotFound() {
            // given
            Long brandId = 999L;
            given(brandRepository.findById(brandId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> brandService.getBrand(brandId))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining(ProductErrorCode.BRAND_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("성공 - 전체 브랜드를 조회한다")
        void success_getAllBrands() {
            // given
            Brand brand2 = Brand.builder()
                    .name("아디다스")
                    .primaryStyle("스포츠")
                    .brandType("글로벌")
                    .build();
            given(brandRepository.findAll()).willReturn(List.of(brand, brand2));

            // when
            List<BrandResponse> result = brandService.getAllBrands();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo(brand.getName());
            assertThat(result.get(1).getName()).isEqualTo(brand2.getName());
        }

        @Test
        @DisplayName("성공 - 스타일별 브랜드를 조회한다")
        void success_getBrandsByStyle() {
            // given
            String style = "스포츠";
            given(brandRepository.findByPrimaryStyle(style)).willReturn(List.of(brand));

            // when
            List<BrandResponse> result = brandService.getBrandsByStyle(style);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo(brand.getName());
            assertThat(result.get(0).getPrimaryStyle()).isEqualTo(style);
        }
    }

    @Nested
    @DisplayName("무신사 브랜드 검증")
    class ValidateMusinsaBrand {

        @Test
        @DisplayName("성공 - 무신사에 존재하는 브랜드")
        void success_brandExists() {
            // given
            String brandName = "나이키";
            BrandValidationResult validationResult = BrandValidationResult.success("nike");
            given(musinsaCrawlerService.validateBrandExists(brandName)).willReturn(validationResult);

            // when
            BrandValidationResult result = brandService.validateMusinsaBrand(brandName);

            // then
            assertThat(result.isExists()).isTrue();
            assertThat(result.getBrandCode()).isEqualTo("nike");
            assertThat(result.getMessage()).isEqualTo("무신사에서 브랜드를 찾았습니다");
        }

        @Test
        @DisplayName("성공 - 무신사에 존재하지 않는 브랜드")
        void success_brandNotExists() {
            // given
            String brandName = "없는브랜드";
            BrandValidationResult validationResult = BrandValidationResult.failure("unknown", "검색 결과 없음");
            given(musinsaCrawlerService.validateBrandExists(brandName)).willReturn(validationResult);

            // when
            BrandValidationResult result = brandService.validateMusinsaBrand(brandName);

            // then
            assertThat(result.isExists()).isFalse();
            assertThat(result.getBrandCode()).isEqualTo("unknown");
        }
    }
}
