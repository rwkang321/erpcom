package com.erpcom.erpcom.common.message;

import java.text.MessageFormat;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MessageServiceImpl implements MessageService {
    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);
    private final MessageMapper mapper;

    public MessageServiceImpl(MessageMapper mapper) { this.mapper = mapper; }

    public List<LanguageInfo> languages() { return mapper.languages(); }

    public String language(String userLanguage, String requestedLanguage, String acceptLanguage) {
        try {
            var supported = languages();
            for (String candidate : new String[]{userLanguage, requestedLanguage}) {
                String match = match(candidate, supported);
                if (match != null) return match;
            }
            if (acceptLanguage != null) {
                try {
                    for (var range : Locale.LanguageRange.parse(acceptLanguage)) {
                        if (range.getWeight() == 0) continue;
                        String match = match(range.getRange(), supported);
                        if (match != null) return match;
                    }
                } catch (IllegalArgumentException e) {
                    log.debug("Ignoring malformed Accept-Language header");
                }
            }
            return defaultLanguage(supported);
        } catch (RuntimeException e) {
            log.warn("Language resolution failed: {}", e.getClass().getSimpleName());
            return "ko";
        }
    }

    private String match(String tag, List<LanguageInfo> supported) {
        if (tag == null || tag.isBlank()) return null;
        for (var lang : supported) if (lang.langCode().equalsIgnoreCase(tag)) return lang.langCode();
        int separator = tag.indexOf('-');
        if (separator > 0) return match(tag.substring(0, separator), supported);
        return null;
    }

    private String defaultLanguage(List<LanguageInfo> supported) {
        return supported.stream().filter(l -> "Y".equals(l.defaultYn()))
                .map(LanguageInfo::langCode).findFirst().orElse("ko");
    }

    public MessageInfo get(String code, String language) {
        try {
            var supported = languages();
            String next = match(language, supported);
            if (next == null) next = defaultLanguage(supported);
            Set<String> visited = new LinkedHashSet<>();
            MessageInfo metadata = null;
            while (next != null && visited.add(next)) {
                var info = mapper.find(code, next);
                if (info == null) return fallback(code);
                metadata = info;
                if (info.messageText() != null) return info;
                String current = next;
                next = supported.stream().filter(l -> l.langCode().equals(current))
                        .map(l -> l.fallbackLangCode() == null ? "" : l.fallbackLangCode())
                        .findFirst().orElse("");
                if (next.isBlank()) next = null;
                else next = match(next, supported);
            }
            String defaultCode = defaultLanguage(supported);
            if (visited.add(defaultCode)) {
                var info = mapper.find(code, defaultCode);
                if (info != null && info.messageText() != null) return info;
                if (info != null) metadata = info;
            }
            if (metadata != null) return new MessageInfo(code, metadata.messageType(), metadata.httpStatus(),
                    metadata.severity(), defaultCode, code, metadata.argCount());
        } catch (RuntimeException e) {
            log.error("Message lookup failed for {}", code, e);
        }
        return fallback(code);
    }

    public String text(String code, String language, Object... args) {
        var info = get(code, language);
        try {
            return new MessageFormat(info.messageText(), Locale.forLanguageTag(info.langCode()))
                    .format(args == null ? new Object[0] : args);
        } catch (RuntimeException e) {
            log.warn("Message format failed for {}", code);
            return code;
        }
    }

    // Only used when the message store is unavailable or the message is absent.
    private MessageInfo fallback(String code) {
        int status = switch (code) {
            case "ERR_AUTH_REQUIRED" -> 401;
            case "ERR_ACCESS_DENIED" -> 403;
            case "ERR_INVALID_INPUT" -> 400;
            case "ERR_NOT_FOUND" -> 404;
            case "ERR_CONFLICT" -> 409;
            default -> 500;
        };
        return new MessageInfo(code, "ERROR", status, "ERROR", "en",
                "The request could not be completed.", 0);
    }
}
