package com.erpcom.erpcom.common.permission;

public enum AccessLevel {
    NONE(0), READ_PARTIAL(1), READ_ALL(3), WRITE_PARTIAL(5), WRITE_ALL(15);
    private final int mask;
    AccessLevel(int mask) { this.mask = mask; }
    public boolean allows(AccessLevel required) {
        return required != NONE && (mask & required.mask) == required.mask;
    }
}
