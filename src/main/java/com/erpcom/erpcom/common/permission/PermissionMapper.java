package com.erpcom.erpcom.common.permission;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.erpcom.erpcom.common.permission.PermissionModels.*;

@Mapper
public interface PermissionMapper {
    UserContext user(long userId);
    List<MenuGrant> roleMenus(long userId);
    List<MenuOverride> userMenus(long userId);
    List<ButtonGrant> roleButtons(long userId);
    List<ButtonGrant> userButtons(long userId);
    Long menuId(String menuCode);
    Long buttonId(@Param("menuId") long menuId, @Param("buttonCode") String buttonCode);
}
