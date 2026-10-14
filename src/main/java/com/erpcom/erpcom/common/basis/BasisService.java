package com.erpcom.erpcom.common.basis;

import java.math.BigDecimal;
import java.util.List;
import com.erpcom.erpcom.common.basis.BasisModels.*;

public interface BasisService {
    List<UnitInfo> units(long userId, String language);
    List<CurrencyInfo> currencies(long userId, String language);
    List<AmountFormatInfo> amountFormats(long userId, String menuCode, String language);
    BigDecimal convertQuantity(long userId, BigDecimal quantity, String fromUnit, String toUnit);
    AmountDisplay displayAmount(long userId, String menuCode, String fieldCode,
                               BigDecimal amount, String currencyCode, String language);
}
