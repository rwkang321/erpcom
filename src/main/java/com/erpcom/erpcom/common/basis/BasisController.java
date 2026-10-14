package com.erpcom.erpcom.common.basis;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.erpcom.erpcom.common.basis.BasisModels.*;
import com.erpcom.erpcom.common.message.MessageService;
import com.erpcom.erpcom.common.web.RequestUserService;

/** Authenticated reference queries; administration reuses MasterController. */
@RestController
@RequestMapping("/api/common")
public class BasisController {
    private final BasisService service;
    private final RequestUserService users;
    private final MessageService messages;
    public BasisController(BasisService service, RequestUserService users, MessageService messages) {
        this.service=service; this.users=users; this.messages=messages;
    }
    @GetMapping("/units")
    public List<UnitInfo> units(HttpServletRequest request, @RequestParam(required=false) String lang) {
        var user=users.require(request);
        return service.units(user.userId(),messages.language(user.langCode(),lang,request.getHeader("Accept-Language")));
    }
    @GetMapping("/currencies")
    public List<CurrencyInfo> currencies(HttpServletRequest request, @RequestParam(required=false) String lang) {
        var user=users.require(request);
        return service.currencies(user.userId(),messages.language(user.langCode(),lang,request.getHeader("Accept-Language")));
    }
    @GetMapping("/amount-formats/{menuCode}")
    public List<AmountFormatInfo> amountFormats(@PathVariable String menuCode, HttpServletRequest request,
            @RequestParam(required=false) String lang) {
        var user=users.require(request);
        return service.amountFormats(user.userId(),menuCode,
            messages.language(user.langCode(),lang,request.getHeader("Accept-Language")));
    }
}
