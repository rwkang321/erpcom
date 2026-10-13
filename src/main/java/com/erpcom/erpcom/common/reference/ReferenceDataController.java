package com.erpcom.erpcom.common.reference;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.erpcom.erpcom.common.reference.ReferenceModels.*;
import com.erpcom.erpcom.common.web.RequestUserService;
import com.erpcom.erpcom.common.message.MessageService;

@RestController
@RequestMapping("/api/common")
public class ReferenceDataController {
    private final ReferenceDataService service;
    private final RequestUserService users;
    private final MessageService messages;
    public ReferenceDataController(ReferenceDataService service, RequestUserService users, MessageService messages) {
        this.service = service; this.users = users; this.messages = messages;
    }
    @GetMapping("/bootstrap")
    public Bootstrap bootstrap(HttpServletRequest request, @RequestParam(required = false) String lang) {
        var user = users.require(request);
        return service.bootstrap(user.userId(),messages.language(user.langCode(),lang,request.getHeader("Accept-Language")));
    }
    @GetMapping("/codes/{group}")
    public List<CodeInfo> codes(@PathVariable String group, HttpServletRequest request,
            @RequestParam(required = false) String lang) {
        var user = users.require(request);
        return service.codes(user.userId(),group,messages.language(user.langCode(),lang,request.getHeader("Accept-Language")));
    }
}
