package com.ipzy.domain.product.exception;

import com.ipzy._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 상품 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROD_001", "상품을 찾을 수 없습니다"),
    PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "PROD_002", "품절된 상품입니다"),
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "PROD_003", "브랜드를 찾을 수 없습니다"),
    CRAWLING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROD_004", "크롤링에 실패했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
