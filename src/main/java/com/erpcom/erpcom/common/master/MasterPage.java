package com.erpcom.erpcom.common.master;

import java.util.List;
import java.util.Map;

public record MasterPage(
        List<Map<String, Object>> items,
        long total,
        int offset,
        int limit) {}
