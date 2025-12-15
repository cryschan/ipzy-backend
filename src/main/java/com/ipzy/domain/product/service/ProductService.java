package com.ipzy.domain.product.service;

import com.ipzy.domain.product.dto.ProductResponse;
import com.ipzy.domain.product.entity.Product;
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
}
