package com.erpcom.erpcom.common.web;

import java.net.URI;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "erpcom.cors")
public record CommonCorsProperties(String allowedOrigin) {
    public CommonCorsProperties {
        allowedOrigin = allowedOrigin == null ? "" : allowedOrigin.trim();
    }

    @AssertTrue(message = "erpcom.cors.allowed-origin must be an exact HTTP(S) origin without a path")
    public boolean isValidOrigin() {
        if (allowedOrigin.isEmpty()) return true;
        try {
            URI origin = URI.create(allowedOrigin);
            return ("http".equals(origin.getScheme()) || "https".equals(origin.getScheme()))
                    && origin.getHost() != null && !allowedOrigin.contains("*")
                    && origin.getRawUserInfo() == null && origin.getRawQuery() == null
                    && origin.getRawFragment() == null && origin.getRawPath().isEmpty()
                    && (origin.getPort() == -1 || origin.getPort() > 0 && origin.getPort() <= 65535);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
