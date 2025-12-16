package com.ipzy.domain.product.service;

import com.ipzy.domain.product.dto.ProductResponse;
import com.ipzy.domain.product.entity.Product;
import com.ipzy.domain.product.exception.ProductErrorCode;
import com.ipzy.domain.product.exception.ProductException;
import com.ipzy.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 전체 상품 조회
     * 삭제되지 않은 모든 상품을 반환합니다.
     *
     * @return 상품 목록
     */
    public List<ProductResponse> getAllProducts() {
        log.info("전체 상품 조회");

        return productRepository.findAll().stream()
                .filter(product -> !product.isDeleted())
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 활성화된 상품만 조회
     * 현재 시즌에 맞는 활성 상품을 반환합니다.
     *
     * @return 활성 상품 목록
     */
    public List<ProductResponse> getActiveProducts() {
        log.info("활성 상품 조회");

        return productRepository.findByIsActiveTrue().stream()
                .filter(product -> !product.isDeleted())
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 상품 삭제 (Soft Delete)
     * deletedAt을 설정하고 isActive를 false로 변경합니다.
     *
     * @param productId 삭제할 상품 ID
     * @throws ProductException 상품을 찾을 수 없거나 이미 삭제된 경우
     */
    @Transactional
    public void deleteProduct(Long productId) {
        log.info("상품 삭제 요청: productId={}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND,
                        "상품을 찾을 수 없습니다: " + productId));

        if (product.isDeleted()) {
            throw new ProductException(ProductErrorCode.PRODUCT_ALREADY_DELETED,
                    "이미 삭제된 상품입니다: " + productId);
        }

        product.softDelete();
        log.info("상품 삭제 완료: productId={}", productId);
    }
}
