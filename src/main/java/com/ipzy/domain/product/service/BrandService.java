package com.ipzy.domain.product.service;

import com.ipzy.domain.product.dto.BrandRequest;
import com.ipzy.domain.product.dto.BrandResponse;
import com.ipzy.domain.product.dto.BrandValidationResult;
import com.ipzy.domain.product.entity.Brand;
import com.ipzy.domain.product.exception.ProductErrorCode;
import com.ipzy.domain.product.exception.ProductException;
import com.ipzy.domain.product.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository brandRepository;
    private final MusinsaCrawlerService musinsaCrawlerService;

    /**
     * 브랜드 등록
     */
    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        log.info("브랜드 등록 시작: name={}, brandType={}", request.getName(), request.getBrandType());

        // 중복 체크 (name + brandType 조합)
        if (brandRepository.existsByNameAndBrandType(request.getName(), request.getBrandType())) {
            throw new ProductException(ProductErrorCode.BRAND_ALREADY_EXISTS);
        }

        // 무신사 검증 (선택사항)
        if (request.isValidateMusinsa()) {
            BrandValidationResult validation = musinsaCrawlerService.validateBrandExists(request.getName());
            if (!validation.isExists()) {
                throw new ProductException(ProductErrorCode.BRAND_NOT_FOUND_IN_MUSINSA);
            }
            log.info("무신사 브랜드 검증 성공: {}", validation.getMessage());
        }

        // 브랜드 저장
        Brand brand = Brand.builder()
                .name(request.getName())
                .logoUrl(request.getLogoUrl())
                .primaryStyle(request.getPrimaryStyle())
                .brandType(request.getBrandType())
                .build();

        Brand savedBrand = brandRepository.save(brand);
        log.info("브랜드 등록 완료: id={}, name={}", savedBrand.getId(), savedBrand.getName());

        return BrandResponse.from(savedBrand);
    }

    /**
     * 브랜드 수정
     */
    @Transactional
    public BrandResponse updateBrand(Long brandId, BrandRequest request) {
        log.info("브랜드 수정 시작: id={}, name={}, brandType={}", brandId, request.getName(), request.getBrandType());

        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.BRAND_NOT_FOUND));

        // 이름 또는 타입 변경 시 중복 체크 (name + brandType 조합)
        boolean nameOrTypeChanged = !brand.getName().equals(request.getName())
                || !brand.getBrandType().equals(request.getBrandType());
        if (nameOrTypeChanged && brandRepository.existsByNameAndBrandType(request.getName(), request.getBrandType())) {
            throw new ProductException(ProductErrorCode.BRAND_ALREADY_EXISTS);
        }

        // 무신사 검증 (선택사항)
        if (request.isValidateMusinsa()) {
            BrandValidationResult validation = musinsaCrawlerService.validateBrandExists(request.getName());
            if (!validation.isExists()) {
                throw new ProductException(ProductErrorCode.BRAND_NOT_FOUND_IN_MUSINSA);
            }
            log.info("무신사 브랜드 검증 성공: {}", validation.getMessage());
        }

        // 브랜드 업데이트
        brand.update(
                request.getName(),
                request.getLogoUrl(),
                request.getPrimaryStyle(),
                request.getBrandType()
        );

        log.info("브랜드 수정 완료: id={}, name={}", brand.getId(), brand.getName());
        return BrandResponse.from(brand);
    }

    /**
     * 브랜드 삭제
     */
    @Transactional
    public void deleteBrand(Long brandId) {
        log.info("브랜드 삭제 시작: id={}", brandId);

        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.BRAND_NOT_FOUND));

        brandRepository.delete(brand);
        log.info("브랜드 삭제 완료: id={}, name={}", brand.getId(), brand.getName());
    }

    /**
     * 브랜드 단건 조회
     */
    public BrandResponse getBrand(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.BRAND_NOT_FOUND));

        return BrandResponse.from(brand);
    }

    /**
     * 브랜드 전체 조회
     */
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(BrandResponse::from)
                .toList();
    }

    /**
     * 스타일별 브랜드 조회
     */
    public List<BrandResponse> getBrandsByStyle(String style) {
        return brandRepository.findByPrimaryStyle(style).stream()
                .map(BrandResponse::from)
                .toList();
    }

    /**
     * 무신사 브랜드 검증 (검증만 수행, 저장 안 함)
     */
    public BrandValidationResult validateMusinsaBrand(String brandName) {
        log.info("브랜드 검증 요청: name={}", brandName);
        return musinsaCrawlerService.validateBrandExists(brandName);
    }
}
