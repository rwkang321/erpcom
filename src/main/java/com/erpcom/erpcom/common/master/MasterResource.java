package com.erpcom.erpcom.common.master;

import java.util.*;
import com.erpcom.erpcom.common.error.ApiException;
import com.erpcom.erpcom.common.master.MasterCatalog.Column;

public record MasterResource(
        String name,
        String table,
        List<String> keys,
        String identity,
        List<Column> columns
) {
    public MasterResource {
        keys = List.copyOf(keys);
        var editable = new ArrayList<>(columns);
        editable.add(new Column("USE_YN", MasterCatalog.Kind.TEXT, 1, true, "Y"));
        columns = List.copyOf(editable);
    }

    public static MasterResource named(String name) {
        return MasterCatalog.RESOURCES.stream()
                .filter(r -> r.name.equals(name))  /** if named("users"), "users" 만 통과 */
                .findFirst()
                .orElseThrow(() -> new ApiException("ERR_NOT_FOUND: %s".formatted(name)));
    }

    public String getTable() { return table; }
    public List<String> getKeys() { return keys; }
    public String getOrderBy() { return String.join(", ", keys); }
    public String getProjection() {
        var fields = new LinkedHashSet<String>(keys);
        columns.forEach(c -> fields.add(c.name()));
        fields.addAll(List.of("USE_YN", "CREATED_AT", "CREATED_BY", "UPDATED_AT", "UPDATED_BY", "VERSION_NO"));
        return String.join(", ", fields);
    }
}

/**
 * public static named(String name) {
 *     return MasterCatalog.RESOURCE.stream()
 *     .filter( r -> r.name().equeals(name) )
 *     .findFirst()
 *     .orElseThrow( () -> new ApiException("ERR_NOT_FOUND: %s).formatted(name)));
 * }
 *
 * public String getOrderBy() { return String.join(", ", keys); }
 * public String getProjection() {
 *      var fields = LinkedHashSet<String>(keys);
 *      columns.forEach(c -> fields.add(c.name()));
 *      fields.addAll(List.of("USE_YN", "CREATED_AT", "CREATED_BY", "UPDATED_AT", "UPDATED_BY", "VERSION_NO"));
 *      return String.join(", ", fields);
 * }
 */