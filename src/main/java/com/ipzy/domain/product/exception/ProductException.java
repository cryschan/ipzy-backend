package com.ipzy.domain.product.exception;

import com.ipzy.global.exception.BusinessException;

public class ProductException extends BusinessException {

    public ProductException(ProductErrorCode errorCode) {
        super(errorCode);
    }

    public ProductException(ProductErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
