package com.ipzy.domain.product.exception;

import com.ipzy._global.exception.BusinessException;

/**
 * 상품 관련 예외
 */
public class ProductException extends BusinessException {

    public ProductException(ProductErrorCode errorCode) {
        super(errorCode);
    }

    public ProductException(ProductErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
