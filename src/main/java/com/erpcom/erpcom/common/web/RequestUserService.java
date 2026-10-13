package com.erpcom.erpcom.common.web;

import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;
import com.erpcom.erpcom.common.error.ApiException;
import com.erpcom.erpcom.common.permission.PermissionService;
import com.erpcom.erpcom.common.permission.PermissionModels.UserContext;

@Service
public class RequestUserService {
    private final CurrentUserProvider provider;
    private final PermissionService permissions;
    public RequestUserService(CurrentUserProvider provider, PermissionService permissions) {
        this.provider = provider;
        this.permissions = permissions;
    }
    public UserContext require(HttpServletRequest request) {
        Long id = provider.userId(request);
        if (id == null || id <= 0) throw new ApiException("ERR_AUTH_REQUIRED");
        return permissions.user(id);
    }
    public String language(HttpServletRequest request) {
        try { return require(request).langCode(); }
        catch (ApiException e) { return null; }
    }
}
