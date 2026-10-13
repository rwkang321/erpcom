package com.erpcom.erpcom.common.permission;

import com.erpcom.erpcom.common.permission.PermissionModels.*;

public interface PermissionService {
    UserContext user(long userId);
    PermissionSnapshot get(long userId);
    void requireMenu(long userId, String menuCode, AccessLevel required);
    void requireButton(long userId, String menuCode, String buttonCode, AccessLevel required);
    void requireScope(long userId, String menuCode, AccessLevel required, DataScope... scopes);
    void requireData(long userId, long menuId, AccessLevel required, DataTarget target);
}
