package com.erpcom.erpcom.common.message;

public record LanguageInfo(
        String langCode,
        String langName,
        String nativeName,
        String fallbackLangCode,
        String defaultYn) {}
