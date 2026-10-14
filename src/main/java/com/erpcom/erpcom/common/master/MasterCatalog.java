package com.erpcom.erpcom.common.master;
import java.util.List;

/** Review-only catalog generated from the proposed V001. */
public final class MasterCatalog {
    private MasterCatalog() {}
    public enum Kind { TEXT, NUMBER, DECIMAL, TIME }
    public record Column(String name, Kind kind, int length, boolean required, String defaultValue, int scale) {
        public Column(String name, Kind kind, int length, boolean required, String defaultValue) {
            this(name, kind, length, required, defaultValue, 0);
        }
    }
    public static final List<MasterResource> RESOURCES = List.of(
        new MasterResource("languages", "TB_LANGUAGE", List.of("LANG_CODE"), null, List.of(
            new Column("LANG_CODE", Kind.TEXT, 10, true, null, 0),
            new Column("LANG_NAME", Kind.TEXT, 100, true, null, 0),
            new Column("NATIVE_NAME", Kind.TEXT, 100, false, null, 0),
            new Column("FALLBACK_LANG_CODE", Kind.TEXT, 10, false, null, 0),
            new Column("DEFAULT_YN", Kind.TEXT, 1, true, "N", 0),
            new Column("SORT_ORDER", Kind.NUMBER, 5, true, "0", 0))),
        new MasterResource("messages", "TB_MESSAGE", List.of("MESSAGE_CODE"), null, List.of(
            new Column("MESSAGE_CODE", Kind.TEXT, 100, true, null, 0),
            new Column("MESSAGE_TYPE", Kind.TEXT, 30, true, null, 0),
            new Column("HTTP_STATUS", Kind.NUMBER, 3, false, null, 0),
            new Column("SEVERITY", Kind.TEXT, 20, false, null, 0),
            new Column("ARG_COUNT", Kind.NUMBER, 2, true, "0", 0))),
        new MasterResource("message-translations", "TB_MESSAGE_LANG", List.of("MESSAGE_CODE", "LANG_CODE"), null, List.of(
            new Column("MESSAGE_CODE", Kind.TEXT, 100, true, null, 0),
            new Column("LANG_CODE", Kind.TEXT, 10, true, null, 0),
            new Column("MESSAGE_TEXT", Kind.TEXT, 2000, true, null, 0),
            new Column("TRANSLATED_YN", Kind.TEXT, 1, true, "Y", 0))),
        new MasterResource("companies", "TB_COMPANY", List.of("COMPANY_ID"), "COMPANY_ID", List.of(
            new Column("COMPANY_CODE", Kind.TEXT, 20, true, null, 0),
            new Column("COMPANY_NAME", Kind.TEXT, 100, true, null, 0))),
        new MasterResource("sites", "TB_SITE", List.of("SITE_ID"), "SITE_ID", List.of(
            new Column("COMPANY_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("SITE_CODE", Kind.TEXT, 20, true, null, 0),
            new Column("SITE_NAME", Kind.TEXT, 100, true, null, 0))),
        new MasterResource("departments", "TB_DEPT", List.of("DEPT_ID"), "DEPT_ID", List.of(
            new Column("COMPANY_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("SITE_ID", Kind.NUMBER, 0, false, null, 0),
            new Column("PARENT_DEPT_ID", Kind.NUMBER, 0, false, null, 0),
            new Column("DEPT_CODE", Kind.TEXT, 30, true, null, 0),
            new Column("DEPT_NAME", Kind.TEXT, 100, true, null, 0),
            new Column("DEPT_LEVEL", Kind.NUMBER, 3, true, "1", 0),
            new Column("SORT_ORDER", Kind.NUMBER, 5, true, "0", 0))),
        new MasterResource("users", "TB_USER", List.of("USER_ID"), "USER_ID", List.of(
            new Column("LOGIN_ID", Kind.TEXT, 100, true, null, 0),
            new Column("USER_NAME", Kind.TEXT, 200, true, null, 0),
            new Column("EMP_NO", Kind.TEXT, 30, false, null, 0),
            new Column("COMPANY_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("SITE_ID", Kind.NUMBER, 0, false, null, 0),
            new Column("DEPT_ID", Kind.NUMBER, 0, false, null, 0),
            new Column("EMAIL", Kind.TEXT, 200, false, null, 0),
            new Column("LANG_CODE", Kind.TEXT, 10, false, null, 0),
            new Column("USER_STATUS", Kind.TEXT, 20, true, "ACTIVE", 0))),
        new MasterResource("roles", "TB_ROLE", List.of("ROLE_ID"), "ROLE_ID", List.of(
            new Column("ROLE_CODE", Kind.TEXT, 50, true, null, 0),
            new Column("ROLE_NAME_MSG_CODE", Kind.TEXT, 100, true, null, 0),
            new Column("ROLE_TYPE", Kind.TEXT, 20, true, "BUSINESS", 0),
            new Column("DESCRIPTION", Kind.TEXT, 500, false, null, 0))),
        new MasterResource("user-roles", "TB_USER_ROLE", List.of("USER_ROLE_ID"), "USER_ROLE_ID", List.of(
            new Column("USER_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("ROLE_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("VALID_FROM", Kind.TIME, 0, true, "@now", 0),
            new Column("VALID_TO", Kind.TIME, 0, false, null, 0),
            new Column("REASON", Kind.TEXT, 500, false, null, 0))),
        new MasterResource("menus", "TB_MENU", List.of("MENU_ID"), "MENU_ID", List.of(
            new Column("PARENT_MENU_ID", Kind.NUMBER, 0, false, null, 0),
            new Column("MENU_CODE", Kind.TEXT, 50, true, null, 0),
            new Column("MENU_NAME_MSG_CODE", Kind.TEXT, 100, true, null, 0),
            new Column("MENU_LEVEL", Kind.NUMBER, 3, true, "1", 0),
            new Column("MENU_TYPE", Kind.TEXT, 20, true, "SCREEN", 0),
            new Column("SCREEN_ID", Kind.TEXT, 100, false, null, 0),
            new Column("FORM_PATH", Kind.TEXT, 300, false, null, 0),
            new Column("ICON_NAME", Kind.TEXT, 100, false, null, 0),
            new Column("SORT_ORDER", Kind.NUMBER, 5, true, "0", 0),
            new Column("SENSITIVE_YN", Kind.TEXT, 1, true, "N", 0))),
        new MasterResource("buttons", "TB_BUTTON", List.of("BUTTON_ID"), "BUTTON_ID", List.of(
            new Column("BUTTON_CODE", Kind.TEXT, 50, true, null, 0),
            new Column("BUTTON_NAME_MSG_CODE", Kind.TEXT, 100, true, null, 0),
            new Column("BUTTON_TYPE", Kind.TEXT, 30, false, null, 0),
            new Column("SENSITIVE_YN", Kind.TEXT, 1, true, "N", 0))),
        new MasterResource("menu-buttons", "TB_MENU_BUTTON", List.of("MENU_ID", "BUTTON_ID"), null, List.of(
            new Column("MENU_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("BUTTON_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("SORT_ORDER", Kind.NUMBER, 5, true, "0", 0))),
        new MasterResource("role-menu-permissions", "TB_ROLE_MENU_AUTH", List.of("ROLE_MENU_AUTH_ID"), "ROLE_MENU_AUTH_ID", List.of(
            new Column("ROLE_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("MENU_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("ACCESS_LEVEL", Kind.TEXT, 20, true, "NONE", 0),
            new Column("DATA_SCOPE", Kind.TEXT, 20, true, "OWN", 0))),
        new MasterResource("user-menu-permissions", "TB_USER_MENU_AUTH", List.of("USER_MENU_AUTH_ID"), "USER_MENU_AUTH_ID", List.of(
            new Column("USER_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("MENU_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("ACCESS_LEVEL", Kind.TEXT, 20, false, null, 0),
            new Column("DATA_SCOPE", Kind.TEXT, 20, false, null, 0),
            new Column("VALID_FROM", Kind.TIME, 0, true, "@now", 0),
            new Column("VALID_TO", Kind.TIME, 0, false, null, 0),
            new Column("REASON", Kind.TEXT, 500, false, null, 0))),
        new MasterResource("role-button-permissions", "TB_ROLE_BUTTON_AUTH", List.of("ROLE_ID", "MENU_ID", "BUTTON_ID"), null, List.of(
            new Column("ROLE_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("MENU_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("BUTTON_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("ACCESS_YN", Kind.TEXT, 1, true, "Y", 0))),
        new MasterResource("user-button-permissions", "TB_USER_BUTTON_AUTH", List.of("USER_BUTTON_AUTH_ID"), "USER_BUTTON_AUTH_ID", List.of(
            new Column("USER_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("MENU_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("BUTTON_ID", Kind.NUMBER, 0, true, null, 0),
            new Column("ACCESS_YN", Kind.TEXT, 1, false, null, 0),
            new Column("VALID_FROM", Kind.TIME, 0, true, "@now", 0),
            new Column("VALID_TO", Kind.TIME, 0, false, null, 0),
            new Column("REASON", Kind.TEXT, 500, false, null, 0))),
        new MasterResource("code-groups", "TB_COMMON_CODE_GROUP", List.of("CODE_GROUP"), null, List.of(
            new Column("CODE_GROUP", Kind.TEXT, 50, true, null, 0),
            new Column("DESCRIPTION", Kind.TEXT, 500, false, null, 0))),
        new MasterResource("codes", "TB_COMMON_CODE", List.of("CODE_GROUP", "CODE"), null, List.of(
            new Column("CODE_GROUP", Kind.TEXT, 50, true, null, 0),
            new Column("CODE", Kind.TEXT, 50, true, null, 0),
            new Column("CODE_NAME_MSG_CODE", Kind.TEXT, 100, true, null, 0),
            new Column("SORT_ORDER", Kind.NUMBER, 5, true, "0", 0),
            new Column("EXT_VALUE1", Kind.TEXT, 500, false, null, 0),
            new Column("EXT_VALUE2", Kind.TEXT, 500, false, null, 0),
            new Column("EXT_VALUE3", Kind.TEXT, 500, false, null, 0))),
        new MasterResource("units", "TB_UNIT", List.of("UNIT_CODE"), null, List.of(
            new Column("UNIT_CODE", Kind.TEXT, 20, true, null, 0),
            new Column("UNIT_NAME_MSG_CODE", Kind.TEXT, 100, true, null, 0),
            new Column("UNIT_TYPE_CODE", Kind.TEXT, 50, true, null, 0),
            new Column("QUANTITY_SCALE", Kind.NUMBER, 2, true, "0", 0),
            new Column("BASE_UNIT_CODE", Kind.TEXT, 20, false, null, 0),
            new Column("TO_BASE_FACTOR", Kind.DECIMAL, 38, false, null, 18),
            new Column("SORT_ORDER", Kind.NUMBER, 5, true, "0", 0))),
        new MasterResource("currencies", "TB_CURRENCY", List.of("CURRENCY_CODE"), null, List.of(
            new Column("CURRENCY_CODE", Kind.TEXT, 3, true, null, 0),
            new Column("CURRENCY_NAME_MSG_CODE", Kind.TEXT, 100, true, null, 0),
            new Column("MINOR_UNIT_DIGITS", Kind.NUMBER, 2, true, null, 0),
            new Column("NUMERIC_CODE", Kind.TEXT, 3, false, null, 0),
            new Column("SORT_ORDER", Kind.NUMBER, 5, true, "0", 0))),
        new MasterResource("menu-amount-formats", "TB_MENU_AMOUNT_FORMAT", List.of("MENU_ID", "FIELD_CODE"), null, List.of(
            new Column("MENU_ID", Kind.NUMBER, 19, true, null, 0),
            new Column("FIELD_CODE", Kind.TEXT, 100, true, null, 0),
            new Column("CURRENCY_CODE", Kind.TEXT, 3, false, null, 0),
            new Column("DISPLAY_DIVISOR", Kind.DECIMAL, 38, true, "1", 0),
            new Column("DECIMAL_PLACES", Kind.NUMBER, 2, true, "0", 0),
            new Column("ROUNDING_MODE", Kind.TEXT, 20, true, "HALF_UP", 0),
            new Column("UNIT_LABEL_MSG_CODE", Kind.TEXT, 100, true, null, 0)))
    );
}
