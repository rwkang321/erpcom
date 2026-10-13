package com.erpcom.erpcom.common.web;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import com.erpcom.erpcom.common.message.*;
import com.erpcom.erpcom.common.permission.*;
import com.erpcom.erpcom.common.permission.PermissionModels.PermissionSnapshot;

@RestController
@RequestMapping("/api/common")
public class ReferenceController {
    private final MessageService messages;
    private final PermissionService permissions;
    private final RequestUserService users;
    public ReferenceController(MessageService messages, PermissionService permissions, RequestUserService users) {
        this.messages = messages; this.permissions = permissions; this.users = users;
    }
    @GetMapping("/languages")
    public List<LanguageInfo> languages() { return messages.languages(); }
    @GetMapping("/messages/{code}")
    public MessageInfo message(@PathVariable String code, @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        return messages.get(code, messages.language(users.language(request), lang, request.getHeader("Accept-Language")));
    }
    @GetMapping("/permissions/me")
    public PermissionSnapshot permissions(HttpServletRequest request) {
        return permissions.get(users.require(request).userId());
    }
}
