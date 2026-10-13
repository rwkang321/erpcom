package com.erpcom.erpcom.common.reference;

import java.util.List;
import com.erpcom.erpcom.common.permission.PermissionModels.*;

public final class ReferenceModels {
    private ReferenceModels() {}
    public record CodeRow(String codeGroup, String code, String codeNameMsgCode, int sortOrder,
                          String extValue1, String extValue2, String extValue3) {}
    public record CodeInfo(String codeGroup, String code, String name, int sortOrder,
                           String extValue1, String extValue2, String extValue3) {}
    public record MenuRow(long menuId, Long parentMenuId, String menuCode, String menuNameMsgCode,
                          String menuType, String screenId, String formPath, String iconName, int sortOrder) {}
    public record ButtonRow(long menuId, long buttonId, String buttonCode, String buttonNameMsgCode, int sortOrder) {}
    public record ButtonInfo(long buttonId, String buttonCode, String name, int sortOrder) {}
    public record MenuInfo(long menuId, Long parentMenuId, String menuCode, String name,
                           String menuType, String screenId, String formPath, String iconName,
                           int sortOrder, List<Grant> grants, List<ButtonInfo> buttons) {}
    public record Bootstrap(UserContext user, String langCode, List<MenuInfo> menus) {}
}
