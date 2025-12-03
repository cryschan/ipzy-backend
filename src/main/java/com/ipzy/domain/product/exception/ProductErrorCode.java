package com.ipzy.domain.product.exception;

import com.ipzy.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROD_001", "상품을 찾을 수 없습니다"),
    PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "PROD_002", "품절된 상품입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
