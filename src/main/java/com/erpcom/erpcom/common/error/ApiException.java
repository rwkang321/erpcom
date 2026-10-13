package com.erpcom.erpcom.common.error;

public final class ApiException extends RuntimeException {
    private final String messageCode;
    private final Object[] args;

    public ApiException(String messageCode, Object... args) {
        super(messageCode);
        this.messageCode = messageCode;
        this.args = args == null ? new Object[0] : args.clone();
    }

    public String messageCode() { return messageCode; }
    public Object[] args() { return args.clone(); }
}
