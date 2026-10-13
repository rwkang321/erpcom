package com.erpcom.erpcom.common.reference;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.erpcom.erpcom.common.message.MessageService;
import com.erpcom.erpcom.common.permission.*;
import com.erpcom.erpcom.common.reference.ReferenceModels.*;

@Service
@Transactional(readOnly = true)
public class ReferenceDataServiceImpl implements ReferenceDataService {
    private final ReferenceMapper mapper;
    private final MessageService messages;
    private final PermissionService permissions;
    public ReferenceDataServiceImpl(ReferenceMapper mapper, MessageService messages, PermissionService permissions) {
        this.mapper = mapper; this.messages = messages; this.permissions = permissions;
    }
    public List<CodeInfo> codes(long userId, String group, String language) {
        permissions.user(userId);
        return mapper.codes(group).stream().map(c -> new CodeInfo(c.codeGroup(),c.code(),
            messages.text(c.codeNameMsgCode(),language),c.sortOrder(),c.extValue1(),c.extValue2(),c.extValue3())).toList();
    }
    public Bootstrap bootstrap(long userId, String language) {
        var user = permissions.user(userId);
        var snapshot = permissions.get(userId);
        var grants = new HashMap<Long, com.erpcom.erpcom.common.permission.PermissionModels.MenuPermission>();
        snapshot.menus().forEach(m -> grants.put(m.menuId(),m));
        var allowedButtons = new HashSet<String>();
        snapshot.buttons().forEach(b -> allowedButtons.add(b.menuId()+":"+b.buttonId()));
        Map<Long,List<ButtonInfo>> buttons = new HashMap<>();
        for (var b : mapper.buttons()) {
            if (allowedButtons.contains(b.menuId()+":"+b.buttonId()))
                buttons.computeIfAbsent(b.menuId(), k -> new ArrayList<>()).add(
                    new ButtonInfo(b.buttonId(),b.buttonCode(),messages.text(b.buttonNameMsgCode(),language),b.sortOrder()));
        }
        List<MenuInfo> menus = new ArrayList<>();
        for (var m : mapper.menus()) {
            var permission = grants.get(m.menuId());
            if (permission == null || !permission.allows(AccessLevel.READ_PARTIAL)) continue;
            menus.add(new MenuInfo(m.menuId(),m.parentMenuId(),m.menuCode(),messages.text(m.menuNameMsgCode(),language),
                m.menuType(),m.screenId(),m.formPath(),m.iconName(),m.sortOrder(),permission.grants(),
                List.copyOf(buttons.getOrDefault(m.menuId(),List.of()))));
        }
        return new Bootstrap(user,language,List.copyOf(menus));
    }
}
