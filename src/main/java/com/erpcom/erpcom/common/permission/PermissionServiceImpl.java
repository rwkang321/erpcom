package com.erpcom.erpcom.common.permission;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.erpcom.erpcom.common.error.ApiException;
import com.erpcom.erpcom.common.permission.PermissionModels.*;

@Service
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {
    private final PermissionMapper mapper;
    public PermissionServiceImpl(PermissionMapper mapper) { this.mapper = mapper; }

    public UserContext user(long userId) {
        var user = mapper.user(userId);
        if (user == null) throw new ApiException("ERR_AUTH_REQUIRED");
        return user;
    }

    public PermissionSnapshot get(long userId) {
        user(userId);
        Map<Long, List<Grant>> menus = new LinkedHashMap<>();
        for (var grant : mapper.roleMenus(userId)) {
            if (grant.accessLevel() != AccessLevel.NONE)
                menus.computeIfAbsent(grant.menuId(), k -> new ArrayList<>())
                        .add(new Grant(grant.accessLevel(), grant.dataScope()));
        }
        Set<Long> overridden = new HashSet<>();
        for (var override : mapper.userMenus(userId)) {
            // Overlapping active overrides are ambiguous: deny rather than choose by row order.
            if (!overridden.add(override.menuId())) throw new ApiException("ERR_CONFLICT");
            var base = menus.getOrDefault(override.menuId(), List.of(new Grant(AccessLevel.NONE, DataScope.OWN)));
            List<Grant> result = new ArrayList<>();
            for (var grant : base) {
                var level = override.accessLevel() == null ? grant.accessLevel() : override.accessLevel();
                var scope = override.dataScope() == null ? grant.dataScope() : override.dataScope();
                if (level != AccessLevel.NONE) result.add(new Grant(level, scope));
            }
            menus.put(override.menuId(), result);
        }
        Map<String, ButtonGrant> buttons = new LinkedHashMap<>();
        for (var grant : mapper.roleButtons(userId)) {
            if ("Y".equals(grant.accessYn())) buttons.put(key(grant), grant);
        }
        Set<String> buttonOverrides = new HashSet<>();
        for (var override : mapper.userButtons(userId)) {
            if (!buttonOverrides.add(key(override))) throw new ApiException("ERR_CONFLICT");
            if ("Y".equals(override.accessYn())) buttons.put(key(override), override);
            if ("N".equals(override.accessYn())) buttons.remove(key(override));
        }
        var menuList = menus.entrySet().stream()
                .map(e -> new MenuPermission(e.getKey(), e.getValue().stream().distinct().toList()))
                .filter(m -> !m.grants().isEmpty()).toList();
        var allowedMenus = new HashSet<Long>();
        menuList.forEach(m -> allowedMenus.add(m.menuId()));
        var buttonList = buttons.values().stream().filter(b -> allowedMenus.contains(b.menuId())).toList();
        return new PermissionSnapshot(menuList, buttonList);
    }

    private String key(ButtonGrant grant) { return grant.menuId() + ":" + grant.buttonId(); }

    public void requireMenu(long userId, String menuCode, AccessLevel required) {
        Long menuId = mapper.menuId(menuCode);
        if (menuId == null || get(userId).menus().stream()
                .noneMatch(m -> m.menuId() == menuId && m.allows(required)))
            throw new ApiException("ERR_ACCESS_DENIED");
    }

    public void requireButton(long userId, String menuCode, String buttonCode, AccessLevel required) {
        requireMenu(userId, menuCode, required);
        Long menuId = mapper.menuId(menuCode);
        Long buttonId = mapper.buttonId(menuId, buttonCode);
        if (buttonId == null || get(userId).buttons().stream()
                .noneMatch(b -> b.menuId() == menuId && b.buttonId() == buttonId))
            throw new ApiException("ERR_ACCESS_DENIED");
    }

    public void requireScope(long userId, String menuCode, AccessLevel required, DataScope... scopes) {
        Long menuId = mapper.menuId(menuCode);
        Set<DataScope> accepted = Set.of(scopes);
        boolean allowed = menuId != null && get(userId).menus().stream().filter(m -> m.menuId() == menuId)
                .flatMap(m -> m.grants().stream()).anyMatch(g -> g.accessLevel().allows(required)
                        && accepted.contains(g.dataScope()));
        if (!allowed) throw new ApiException("ERR_ACCESS_DENIED");
    }

    public void requireData(long userId, long menuId, AccessLevel required, DataTarget target) {
        var user = user(userId);
        if (target.companyId() != user.companyId()) throw new ApiException("ERR_ACCESS_DENIED");
        boolean allowed = get(userId).menus().stream().filter(m -> m.menuId() == menuId)
                .flatMap(m -> m.grants().stream()).anyMatch(g -> g.accessLevel().allows(required)
                        && switch (g.dataScope()) {
                            case COMPANY -> true;
                            case SITE -> user.siteId() != null && user.siteId().equals(target.siteId());
                            case DEPT -> user.deptId() != null && user.deptId().equals(target.deptId());
                            case OWN -> Objects.equals(user.userId(), target.ownerUserId());
                        });
        if (!allowed) throw new ApiException("ERR_ACCESS_DENIED");
    }
}
