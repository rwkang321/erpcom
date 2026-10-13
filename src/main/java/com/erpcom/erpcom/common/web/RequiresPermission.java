package com.erpcom.erpcom.common.web;

import java.lang.annotation.*;
import com.erpcom.erpcom.common.permission.AccessLevel;
import com.erpcom.erpcom.common.permission.DataScope;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    String menu() default "COMMON_ADMIN";
    String button() default "BTN_SEARCH";
    AccessLevel level() default AccessLevel.READ_ALL;
    DataScope[] scopes() default {};
}
