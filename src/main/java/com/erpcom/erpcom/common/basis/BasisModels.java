package com.erpcom.erpcom.common.basis;

import java.math.BigDecimal;

/** Review proposal. Decimal values sent to screens are strings to preserve precision. */
public final class BasisModels {
    private BasisModels() {}
    public record UnitRow(String unitCode, String unitNameMsgCode, String unitTypeCode,
                          int quantityScale, String baseUnitCode, BigDecimal toBaseFactor) {}
    public record UnitInfo(String unitCode, String name, String unitTypeCode,
                           int quantityScale, String baseUnitCode, String toBaseFactor) {}
    public record CurrencyRow(String currencyCode, String currencyNameMsgCode,
                              int minorUnitDigits, String numericCode) {}
    public record CurrencyInfo(String currencyCode, String name, int minorUnitDigits, String numericCode) {}
    public record MenuIdentity(long menuId, String menuCode) {}
    public record AmountFormatRow(long menuId, String fieldCode, String currencyCode,
                                  BigDecimal displayDivisor, int decimalPlaces,
                                  String roundingMode, String unitLabelMsgCode) {}
    public record AmountFormatInfo(long menuId, String fieldCode, String currencyCode,
                                   String displayDivisor, int decimalPlaces,
                                   String roundingMode, String unitLabel) {}
    public record AmountDisplay(String originalAmount, String currencyCode, String displayValue,
                                String displayDivisor, int decimalPlaces, String roundingMode, String unitLabel) {}
}
