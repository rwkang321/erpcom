package com.erpcom.erpcom.common.message;

import java.util.List;

public interface MessageService {
    List<LanguageInfo> languages();
    MessageInfo get(String code, String language);
    String text(String code, String language, Object... args);
    String language(String userLanguage, String requestedLanguage, String acceptLanguage);
}
