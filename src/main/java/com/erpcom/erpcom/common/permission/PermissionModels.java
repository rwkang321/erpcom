package com.erpcom.erpcom.common.permission;

import java.util.List;

public final class PermissionModels {
    private PermissionModels() {}
    public record UserContext(long userId, long companyId, Long siteId, Long deptId, String langCode) {}
    public record Grant(AccessLevel accessLevel, DataScope dataScope) {}
    public record MenuGrant(long menuId, AccessLevel accessLevel, DataScope dataScope) {}
    public record MenuOverride(long menuId, AccessLevel accessLevel, DataScope dataScope) {}
    public record ButtonGrant(long menuId, long buttonId, String accessYn) {}
    public record MenuPermission(long menuId, List<Grant> grants) {
        public MenuPermission { grants = List.copyOf(grants); }
        public boolean allows(AccessLevel level) { return grants.stream().anyMatch(g -> g.accessLevel().allows(level)); }
    }
    public record DataTarget(long companyId, Long siteId, Long deptId, Long ownerUserId) {}
    public record PermissionSnapshot(List<MenuPermission> menus, List<ButtonGrant> buttons) {}
}
