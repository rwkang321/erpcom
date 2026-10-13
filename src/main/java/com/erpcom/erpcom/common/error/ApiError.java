package com.erpcom.erpcom.common.error;

import java.time.Instant;

public record ApiError(
        String messageCode,
        String message,
        String langCode,
        int status,
        String path,
        Instant timestamp) {}
