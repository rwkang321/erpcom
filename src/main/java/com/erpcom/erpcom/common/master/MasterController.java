package com.erpcom.erpcom.common.master;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.erpcom.erpcom.common.web.*;
import com.erpcom.erpcom.common.permission.AccessLevel;
import com.erpcom.erpcom.common.permission.DataScope;

/**
 * @RestController
 * @RequestMapping("/api/common/admin")
 * @RequiresPermission(scopes = DataScope.COMPANY)
 */

@RestController
@RequestMapping("/api/common/admin")
@RequiresPermission(scopes = DataScope.COMPANY)
public class MasterController {
    private final MasterService service;
    private final RequestUserService users;
    public MasterController(MasterService service, RequestUserService users) {
        this.service = service;
        this.users = users;
    }

    @GetMapping("/resources")
    public List<MasterResource> resources() { return MasterCatalog.RESOURCES; }

    @GetMapping("/{resource}")
    public MasterPage list(@PathVariable String resource, @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit, @RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.list(resource, offset, limit, includeInactive);
    }
    @GetMapping("/{resource}/{key}")
    public Map<String, Object> get(@PathVariable String resource, @PathVariable String key) { return service.get(resource, key); }

    @PostMapping("/{resource}")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(button = "BTN_NEW", level = AccessLevel.WRITE_ALL)
    public Map<String, Object> create(@PathVariable String resource, @Valid @RequestBody MasterRequest body,
            HttpServletRequest request) { return service.create(resource, body, users.require(request).userId()); }

    @PutMapping("/{resource}/{key}")
    @RequiresPermission(button = "BTN_SAVE", level = AccessLevel.WRITE_ALL)
    public Map<String, Object> update(@PathVariable String resource, @PathVariable String key,
            @Valid @RequestBody MasterRequest body, HttpServletRequest request) {
        return service.update(resource, key, body, users.require(request).userId());
    }

    @DeleteMapping("/{resource}/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission(button = "BTN_DELETE", level = AccessLevel.WRITE_ALL)
    public void deactivate(@PathVariable String resource, @PathVariable String key,
            @RequestParam long expectedVersion, HttpServletRequest request) {
        service.deactivate(resource, key, expectedVersion, users.require(request).userId());
    }
}
