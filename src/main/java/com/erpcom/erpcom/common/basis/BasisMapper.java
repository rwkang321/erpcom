package com.erpcom.erpcom.common.basis;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.erpcom.erpcom.common.basis.BasisModels.*;

@Mapper
public interface BasisMapper {
    List<UnitRow> units();
    UnitRow unit(@Param("unitCode") String unitCode);
    List<CurrencyRow> currencies();
    CurrencyRow currency(@Param("currencyCode") String currencyCode);
    MenuIdentity menu(@Param("menuCode") String menuCode);
    List<AmountFormatRow> amountFormats(@Param("menuId") long menuId);
}
