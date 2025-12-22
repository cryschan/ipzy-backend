package com.ipzy.domain.admin.service;

import com.ipzy.domain.admin.dto.AdminProductDetailResponse;
import com.ipzy.domain.admin.dto.AdminProductResponse;
import com.ipzy.domain.admin.dto.AdminProductSearchRequest;
import com.ipzy.domain.admin.exception.AdminException;
import com.ipzy.domain.admin.specification.AdminProductSpecification;
import com.ipzy.domain.product.entity.Product;
import com.ipzy.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;

    public Page<AdminProductResponse> findProducts(AdminProductSearchRequest request, Pageable pageable) {
        Specification<Product> spec = Specification
            .where(AdminProductSpecification.nameOrBrandContains(request.keyword()))
            .and(AdminProductSpecification.categoryEquals(request.category()))
            .and(AdminProductSpecification.brandIdEquals(request.brandId()))
            .and(AdminProductSpecification.isActiveEquals(request.isActive()))
            .and(AdminProductSpecification.notDeleted())
            .and(AdminProductSpecification.createdAtBetween(request.createdFrom(), request.createdTo()));

        return productRepository.findAll(spec, pageable)
            .map(AdminProductResponse::from);
    }

    public AdminProductDetailResponse findProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(AdminException::productNotFound);
        return AdminProductDetailResponse.from(product);
    }

    @Transactional
    public AdminProductDetailResponse activateProduct(Long productId, Long adminId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(AdminException::productNotFound);

        if (product.isDeleted()) {
            throw AdminException.productAlreadyDeleted();
        }

        product.activate();
        log.info("상품 활성화: productId={}, adminId={}", productId, adminId);

        return AdminProductDetailResponse.from(product);
    }

    @Transactional
    public AdminProductDetailResponse deactivateProduct(Long productId, Long adminId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(AdminException::productNotFound);

        if (product.isDeleted()) {
            throw AdminException.productAlreadyDeleted();
        }

        product.deactivate();
        log.info("상품 비활성화: productId={}, adminId={}", productId, adminId);

        return AdminProductDetailResponse.from(product);
    }

    @Transactional
    public void deleteProduct(Long productId, Long adminId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(AdminException::productNotFound);

        if (product.isDeleted()) {
            throw AdminException.productAlreadyDeleted();
        }

        product.softDelete();
        log.info("상품 삭제: productId={}, adminId={}", productId, adminId);
    }
}
