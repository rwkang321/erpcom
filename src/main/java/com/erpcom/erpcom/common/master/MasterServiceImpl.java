package com.erpcom.erpcom.common.master;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.erpcom.erpcom.common.error.ApiException;
import com.erpcom.erpcom.common.master.MasterCatalog.*;

@Service
@Transactional(readOnly = true)
public class MasterServiceImpl implements MasterService {
    private final MasterMapper mapper;
    public MasterServiceImpl(MasterMapper mapper) { this.mapper = mapper; }

    public MasterPage list(String resource, int offset, int limit, boolean includeInactive) {
        if (offset < 0 || limit < 1 || limit > 200) invalid();
        var definition = MasterResource.named(resource);
        return new MasterPage(mapper.list(definition, offset, limit, includeInactive),
                mapper.count(definition, includeInactive), offset, limit);
    }

    public Map<String, Object> get(String resource, String key) {
        var definition = MasterResource.named(resource);
        return find(definition, keys(definition, key));
    }

    @Transactional
    public Map<String, Object> create(String resource, MasterRequest request, long actor) {
        var definition = MasterResource.named(resource);
        if (request.expectedVersion() != null) invalid();
        var values = normalize(definition, request.values(), true);
        if (definition.identity() != null) values.put(definition.identity(), mapper.nextId());
        validate(definition, values, null);
        mapper.insert(definition, values, actor);
        Map<String, Object> keyValues = new LinkedHashMap<>();
        definition.keys().forEach(k -> keyValues.put(k, values.get(k)));
        return find(definition, keyValues);
    }

    @Transactional
    public Map<String, Object> update(String resource, String key, MasterRequest request, long actor) {
        if (request.expectedVersion() == null) invalid();
        var definition = MasterResource.named(resource);
        var keyValues = keys(definition, key);
        var changes = normalize(definition, request.values(), false);
        for (String primary : definition.keys()) if (changes.containsKey(primary)) invalid();
        var merged = new LinkedHashMap<>(find(definition, keyValues));
        merged.putAll(changes);
        validate(definition, merged, keyValues);
        if (mapper.update(definition, keyValues, changes, request.expectedVersion(), actor) != 1)
            throw new ApiException("ERR_CONFLICT");
        return find(definition, keyValues);
    }

    @Transactional
    public void deactivate(String resource, String key, long expectedVersion, long actor) {
        if (expectedVersion < 0) invalid();
        var definition = MasterResource.named(resource);
        var keyValues = keys(definition, key);
        var row = find(definition, keyValues);
        if (definition.name().equals("languages") && "Y".equals(row.get("DEFAULT_YN")))
            throw new ApiException("ERR_CONFLICT");
        if (mapper.deactivate(definition, keyValues, expectedVersion, actor) != 1)
            throw new ApiException("ERR_CONFLICT");
    }

    private Map<String, Object> find(MasterResource definition, Map<String, Object> keys) {
        var row = mapper.find(definition, keys);
        if (row == null) throw new ApiException("ERR_NOT_FOUND");
        return row;
    }

    private Map<String, Object> keys(MasterResource definition, String key) {
        String[] parts = key.split("~", -1);
        if (parts.length != definition.keys().size()) invalid();
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < parts.length; i++) {
            String column = definition.keys().get(i);
            if (parts[i].isBlank()) invalid();
            var metadata = definition.columns().stream().filter(c -> c.name().equals(column)).findFirst();
            if (metadata.isPresent()) result.put(column, convert(metadata.get(), parts[i]));
            else {
                try {
                    long id = new BigDecimal(parts[i]).longValueExact();
                    if (id <= 0) invalid();
                    result.put(column, id);
                }
                catch (RuntimeException e) { invalid(); }
            }
        }
        return result;
    }

    private Map<String, Object> normalize(MasterResource resource, Map<String, Object> input, boolean create) {
        if (input == null || input.isEmpty() || input.size() > 40) invalid();
        Map<String, Column> allowed = new HashMap<>();
        resource.columns().forEach(c -> allowed.put(c.name(), c));
        Map<String, Object> values = new LinkedHashMap<>();
        for (var entry : input.entrySet()) {
            var field = allowed.get(entry.getKey());
            if (field == null) invalid();
            values.put(field.name(), convert(field, entry.getValue()));
        }
        if (create) for (var column : resource.columns()) {
            if (!values.containsKey(column.name())) {
                Object value = column.defaultValue();
                if ("@now".equals(value)) value = mapper.now();
                values.put(column.name(), convert(column, value));
            }
        }
        return values;
    }

    private Object convert(Column column, Object raw) {
        if (raw == null || raw instanceof String text && text.isBlank()) {
            if (column.required()) invalid();
            return null;
        }
    }

//    private Object convert(Column column, Object raw) {
//        if (raw == null || raw instanceof String text && text.isBlank()) {
//            if (column.required()) invalid();
//            return null;
//        }
//        try {
//            return switch (column.kind()) {
//                case TEXT -> {
//                    if (!(raw instanceof String)) throw new IllegalArgumentException();
//                    String value = (String) raw;
//                    // Oracle V001 uses byte-length VARCHAR2 unless the schema specifies CHAR semantics.
//                    if (value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > column.length()) invalid();
//                    if (column.name().endsWith("_YN") && !Set.of("Y", "N").contains(value)) invalid();
//                    yield value;
//                }
//                case NUMBER -> {
//                    var value = new BigDecimal(raw.toString());
//                    long number = value.longValueExact();
//                    if (column.length() > 0 && value.abs().precision() > column.length()) invalid();
//                    if (column.name().endsWith("_ID") && number <= 0) invalid();
//                    yield number;
//                }
//                case TIME -> raw instanceof Timestamp ? raw
//                        : Timestamp.valueOf(LocalDateTime.parse(raw.toString()));
//            };
//        } catch (ApiException e) { throw e; }
//        catch (RuntimeException e) { throw new ApiException("ERR_INVALID_INPUT"); }
//    }

    private void validate(MasterResource resource, Map<String, Object> values, Map<String, Object> keys) {
        // Re-enabling a row uses the same validation as inserting it.
        for (var column : resource.columns()) convert(column, values.get(column.name()));
        for (String key : resource.keys()) {
            Object value = values.get(key);
            if (value instanceof String text && (text.contains("~") || text.contains("/"))) invalid();
        }
        if (values.get("ACCESS_LEVEL") != null) {
            try { com.erpcom.erpcom.common.permission.AccessLevel.valueOf(values.get("ACCESS_LEVEL").toString()); }
            catch (IllegalArgumentException e) { invalid(); }
        }
        if (values.get("DATA_SCOPE") != null) {
            try { com.erpcom.erpcom.common.permission.DataScope.valueOf(values.get("DATA_SCOPE").toString()); }
            catch (IllegalArgumentException e) { invalid(); }
        }
        if (values.get("HTTP_STATUS") instanceof Number status && (status.intValue() < 100 || status.intValue() > 599)) invalid();
        if (values.get("ARG_COUNT") instanceof Number count && (count.intValue() < 0 || count.intValue() > 10)) invalid();
        if (values.containsKey("VALID_FROM")) validatePeriod(resource, values, keys);
        if (resource.name().equals("departments") || resource.name().equals("menus")) {
            mapper.lockHierarchy(resource);
            validateHierarchy(resource, values);
        }
        if (Set.of("sites", "departments", "users").contains(resource.name())) validateOrganization(resource, values);
        if (resource.name().equals("users")) {
            var code = mapper.find(MasterResource.named("codes"),
                    Map.of("CODE_GROUP", "USER_STATUS", "CODE", values.get("USER_STATUS")));
            if (code == null || !"Y".equals(code.get("USE_YN"))) invalid();
            Object email = values.get("EMAIL");
            if (email != null && !email.toString().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) invalid();
        }
        if (resource.name().equals("languages")) {
            mapper.lockHierarchy(resource);
            validateLanguage(values);
        }
        if (resource.name().equals("message-translations")) validateTranslation(values);
    }

    private void validateOrganization(MasterResource resource, Map<String, Object> values) {
        Object company = values.get("COMPANY_ID");
        var companyRow = find(MasterResource.named("companies"), Map.of("COMPANY_ID", company));
        if (!"Y".equals(companyRow.get("USE_YN"))) invalid();
        for (String field : List.of("SITE_ID", "DEPT_ID")) {
            if (field.equals(resource.identity())) continue;
            Object id = values.get(field);
            if (id == null) continue;
            var row = find(MasterResource.named(field.equals("SITE_ID") ? "sites" : "departments"), Map.of(field, id));
            if (!same(company, row.get("COMPANY_ID")) || !"Y".equals(row.get("USE_YN"))) invalid();
            if (field.equals("DEPT_ID") && row.get("SITE_ID") != null
                    && !same(values.get("SITE_ID"), row.get("SITE_ID"))) invalid();
        }
    }

    private void validateHierarchy(MasterResource resource, Map<String, Object> values) {
        String parentField = resource.name().equals("menus") ? "PARENT_MENU_ID" : "PARENT_DEPT_ID";
        String idField = resource.keys().getFirst();
        Object parent = values.get(parentField);
        Set<String> seen = new HashSet<>();
        seen.add(idToken(values.get(idField)));
        while (parent != null) {
            if (!seen.add(idToken(parent))) invalid();
            var row = find(resource, Map.of(idField, parent));
            if (!"Y".equals(row.get("USE_YN"))) invalid();
            if (resource.name().equals("departments")
                    && (!same(values.get("COMPANY_ID"), row.get("COMPANY_ID"))
                    || !same(values.get("SITE_ID"), row.get("SITE_ID")))) invalid();
            parent = row.get(parentField);
        }
    }

    private boolean same(Object left, Object right) {
        if (left == null || right == null) return left == right;
        if (left instanceof Number && right instanceof Number)
            return new BigDecimal(left.toString()).compareTo(new BigDecimal(right.toString())) == 0;
        return left.toString().equals(right.toString());
    }

    private String idToken(Object id) { return new BigDecimal(id.toString()).toPlainString(); }

    private void validatePeriod(MasterResource resource, Map<String, Object> values, Map<String, Object> keys) {
        Timestamp from = (Timestamp) convert(new Column("VALID_FROM", Kind.TIME, 0, true, null), values.get("VALID_FROM"));
        Timestamp to = (Timestamp) convert(new Column("VALID_TO", Kind.TIME, 0, false, null), values.get("VALID_TO"));
        if (to != null && to.before(from)) invalid();
        if ("N".equals(values.get("USE_YN"))) return;
        long userId = new BigDecimal(values.get("USER_ID").toString()).longValueExact();
        if (mapper.lockUser(userId) == null) invalid();
        var partition = new LinkedHashMap<String, Object>();
        partition.put("USER_ID", userId);
        if (resource.name().equals("user-roles")) partition.put("ROLE_ID", values.get("ROLE_ID"));
        else {
            partition.put("MENU_ID", values.get("MENU_ID"));
            if (values.containsKey("BUTTON_ID")) partition.put("BUTTON_ID", values.get("BUTTON_ID"));
        }
        Object identity = keys == null ? null : keys.get(resource.identity());
        if (mapper.overlaps(resource, partition, identity, from, to) > 0) throw new ApiException("ERR_CONFLICT");
    }

    private void validateLanguage(Map<String, Object> values) {
        if ("Y".equals(values.get("DEFAULT_YN")) && "N".equals(values.get("USE_YN")))
            throw new ApiException("ERR_CONFLICT");
        String tag = values.get("LANG_CODE").toString();
        try {
            if (!new Locale.Builder().setLanguageTag(tag).build().toLanguageTag().equals(tag)) invalid();
        } catch (RuntimeException e) { invalid(); }
        String next = (String) values.get("FALLBACK_LANG_CODE");
        Set<String> seen = new HashSet<>();
        seen.add(tag);
        while (next != null) {
            if (!seen.add(next)) invalid();
            var language = find(MasterResource.named("languages"), Map.of("LANG_CODE", next));
            next = (String) language.get("FALLBACK_LANG_CODE");
        }
    }

    private void validateTranslation(Map<String, Object> values) {
        var message = find(MasterResource.named("messages"), Map.of("MESSAGE_CODE", values.get("MESSAGE_CODE")));
        try {
            new MessageFormat(values.get("MESSAGE_TEXT").toString());
            int count = ((Number) message.get("ARG_COUNT")).intValue();
            var matcher = Pattern.compile("\\{(\\d+)").matcher(values.get("MESSAGE_TEXT").toString());
            while (matcher.find()) if (Integer.parseInt(matcher.group(1)) >= count) invalid();
        } catch (RuntimeException e) { invalid(); }
    }

    private void invalid() { throw new ApiException("ERR_INVALID_INPUT"); }
}
