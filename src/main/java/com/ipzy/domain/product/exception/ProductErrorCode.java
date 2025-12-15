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
    CRAWLING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROD_004", "크롤링에 실패했습니다"),
    BRAND_ALREADY_EXISTS(HttpStatus.CONFLICT, "PROD_005", "이미 존재하는 브랜드입니다"),
    BRAND_NOT_FOUND_IN_MUSINSA(HttpStatus.BAD_REQUEST, "PROD_006", "무신사에서 브랜드를 찾을 수 없습니다"),
    INVALID_BRAND_TYPE(HttpStatus.BAD_REQUEST, "PROD_007", "유효하지 않은 브랜드 타입입니다"),
    INVALID_SHOE_CATEGORY(HttpStatus.BAD_REQUEST, "PROD_008", "유효하지 않은 신발 카테고리입니다"),
    INVALID_PRODUCT_DATA(HttpStatus.BAD_REQUEST, "PROD_009", "유효하지 않은 상품 데이터입니다"),
    BRAND_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "PROD_010", "브랜드명은 필수입니다"),
    BRAND_HAS_PRODUCTS(HttpStatus.CONFLICT, "PROD_011", "해당 브랜드에 연결된 상품이 존재하여 삭제할 수 없습니다"),
    PRODUCT_ALREADY_DELETED(HttpStatus.CONFLICT, "PROD_012", "이미 삭제된 상품입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
