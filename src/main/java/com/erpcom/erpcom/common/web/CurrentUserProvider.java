package com.erpcom.erpcom.common.web;

import jakarta.servlet.http.HttpServletRequest;

/** Level 2 supplies a bean backed by its verified authentication context. Never trust a request header. */
public interface CurrentUserProvider {
    Long userId(HttpServletRequest request);
}
