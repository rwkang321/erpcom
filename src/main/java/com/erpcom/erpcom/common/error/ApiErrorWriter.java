package com.erpcom.erpcom.common.error;

import java.time.Instant;
import java.text.MessageFormat;
import java.util.Locale;
import jakarta.servlet.http.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import com.erpcom.erpcom.common.message.MessageService;
import com.erpcom.erpcom.common.web.RequestUserService;

@Component
public class ApiErrorWriter {
    private final MessageService messages;
    private final RequestUserService users;
    private final ObjectMapper json;
    public ApiErrorWriter(MessageService messages, RequestUserService users, ObjectMapper json) {
        this.messages = messages; this.users = users; this.json = json;
    }

    public ResponseEntity<ApiError> response(ApiException exception, HttpServletRequest request) {
        String userLanguage = null;
        try {
            userLanguage = users.language(request);
        } catch (RuntimeException ignored) { /* DB outage fallback */ }

        String language = messages.language(userLanguage, request.getParameter("lang"), request.getHeader("Accept-Language"));
        var info = messages.get(exception.messageCode(), language);
        int status = info.httpStatus() != null && info.httpStatus() >= 400 && info.httpStatus() <= 599
                ? info.httpStatus() : 500;
        String text;
        try { text = new MessageFormat(info.messageText(), Locale.forLanguageTag(info.langCode())).format(exception.args()); }
        catch (RuntimeException e) { text = exception.messageCode(); }
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(new ApiError(exception.messageCode(), text,
                info.langCode(), status, request.getRequestURI(), Instant.now()));
    }

    /** Level 2 Filter errors use the same formatter as Controller errors. */
    public void write(HttpServletRequest request, HttpServletResponse response, ApiException exception)
            throws java.io.IOException {
        var result = response(exception, request);
        response.setStatus(result.getStatusCode().value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(json.writeValueAsString(result.getBody()));
    }
}
