package com.ipzy.domain.outfit.exception;

import com.ipzy.global.exception.BusinessException;

public class OutfitException extends BusinessException {

    public OutfitException(OutfitErrorCode errorCode) {
        super(errorCode);
    }

    public OutfitException(OutfitErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
