package com.erpcom.erpcom.common.basis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.erpcom.erpcom.common.basis.BasisModels.*;
import com.erpcom.erpcom.common.error.ApiException;
import com.erpcom.erpcom.common.message.MessageService;
import com.erpcom.erpcom.common.permission.AccessLevel;
import com.erpcom.erpcom.common.permission.PermissionService;

@Service
@Transactional(readOnly = true)
public class BasisServiceImpl implements BasisService {
    private final BasisMapper mapper;
    private final MessageService messages;
    private final PermissionService permissions;
    public BasisServiceImpl(BasisMapper mapper, MessageService messages, PermissionService permissions) {
        this.mapper=mapper; this.messages=messages; this.permissions=permissions;
    }

    public List<UnitInfo> units(long userId, String language) {
        permissions.user(userId);
        return mapper.units().stream().map(u -> new UnitInfo(u.unitCode(),
            messages.text(u.unitNameMsgCode(),language),u.unitTypeCode(),u.quantityScale(),u.baseUnitCode(),
            u.toBaseFactor()==null ? null : u.toBaseFactor().stripTrailingZeros().toPlainString())).toList();
    }

    public List<CurrencyInfo> currencies(long userId, String language) {
        permissions.user(userId);
        return mapper.currencies().stream().map(c -> new CurrencyInfo(c.currencyCode(),
            messages.text(c.currencyNameMsgCode(),language),c.minorUnitDigits(),c.numericCode())).toList();
    }

    private MenuIdentity readableMenu(long userId, String menuCode) {
        permissions.requireMenu(userId,menuCode,AccessLevel.READ_PARTIAL);
        var menu=mapper.menu(menuCode);
        if (menu==null) throw new ApiException("ERR_NOT_FOUND");
        return menu;
    }

    public List<AmountFormatInfo> amountFormats(long userId, String menuCode, String language) {
        var menu=readableMenu(userId,menuCode);
        return mapper.amountFormats(menu.menuId()).stream().map(f -> new AmountFormatInfo(f.menuId(),
            f.fieldCode(),f.currencyCode(),f.displayDivisor().toPlainString(),f.decimalPlaces(),f.roundingMode(),
            messages.text(f.unitLabelMsgCode(),language))).toList();
    }

    public BigDecimal convertQuantity(long userId, BigDecimal quantity, String fromUnit, String toUnit) {
        permissions.user(userId);
        if (quantity==null || fromUnit==null || toUnit==null) throw new ApiException("ERR_INVALID_INPUT");
        var from=mapper.unit(fromUnit);
        var to=mapper.unit(toUnit);
        if (from==null || to==null) throw new ApiException("ERR_NOT_FOUND");
        try {
            quantity=quantity.setScale(from.quantityScale(),RoundingMode.UNNECESSARY);
            if (fromUnit.equals(toUnit)) return quantity;
            if (!Objects.equals(from.unitTypeCode(),to.unitTypeCode()) || from.baseUnitCode()==null
                    || !Objects.equals(from.baseUnitCode(),to.baseUnitCode())
                    || from.toBaseFactor()==null || to.toBaseFactor()==null)
                throw new ApiException("ERR_UNIT_INCOMPATIBLE");
            return quantity.multiply(from.toBaseFactor()).divide(to.toBaseFactor(),
                to.quantityScale(),RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) { throw new ApiException("ERR_INVALID_INPUT"); }
    }

    public AmountDisplay displayAmount(long userId, String menuCode, String fieldCode,
            BigDecimal amount, String currencyCode, String language) {
        var menu=readableMenu(userId,menuCode);
        if (amount==null || currencyCode==null || !currencyCode.matches("[A-Z]{3}")
                || fieldCode==null || !fieldCode.matches("[A-Z][A-Z0-9_]*"))
            throw new ApiException("ERR_INVALID_INPUT");
        var currency=mapper.currency(currencyCode);
        if (currency==null) throw new ApiException("ERR_INVALID_INPUT");
        var settings=mapper.amountFormats(menu.menuId());
        var selected=settings.stream().filter(f -> fieldCode.equals(f.fieldCode())).findFirst()
            .orElseGet(() -> settings.stream().filter(f -> "DEFAULT".equals(f.fieldCode())).findFirst().orElse(null));
        BigDecimal divisor=selected==null ? BigDecimal.ONE : selected.displayDivisor();
        int decimals=selected==null ? currency.minorUnitDigits() : selected.decimalPlaces();
        RoundingMode rounding=selected==null ? RoundingMode.HALF_UP : RoundingMode.valueOf(selected.roundingMode());
        String labelCode=selected==null ? "LBL_AMOUNT_UNIT_ONE" : selected.unitLabelMsgCode();
        if (selected!=null && selected.currencyCode()!=null && !currencyCode.equals(selected.currencyCode()))
            throw new ApiException("ERR_CURRENCY_MISMATCH");
        String display=amount.divide(divisor,decimals,rounding).toPlainString();
        return new AmountDisplay(amount.toPlainString(),currencyCode,display,divisor.toPlainString(),
            decimals,rounding.name(),messages.text(labelCode,language));
    }
}
