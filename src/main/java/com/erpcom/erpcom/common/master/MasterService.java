package com.erpcom.erpcom.common.master;

import java.util.Map;
public interface MasterService {
    MasterPage list(String resource, int offset, int limit, boolean includeInactive);
    Map<String, Object> get(String resource, String key);
    Map<String, Object> create(String resource, MasterRequest request, long actor);
    Map<String, Object> update(String resource, String key, MasterRequest request, long actor);
    void deactivate(String resource, String key, long expectedVersion, long actor);
}
