package com.erpcom.erpcom.common.message;

public record MessageInfo(
        String messageCode,
        String messageType,
        Integer httpStatus,
        String severity,
        String langCode,
        String messageText,
        int argCount) {}
