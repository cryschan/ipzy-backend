package com.ipzy.domain.outfit.exception;

import com.ipzy._global.exception.BusinessException;

/**
 * 코디 관련 예외
 */
public class OutfitException extends BusinessException {

    public OutfitException(OutfitErrorCode errorCode) {
        super(errorCode);
    }

    public OutfitException(OutfitErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
