package com.erpcom.erpcom.common.reference;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.erpcom.erpcom.common.reference.ReferenceModels.*;

@Mapper
public interface ReferenceMapper {
    List<CodeRow> codes(String group);
    List<MenuRow> menus();
    List<ButtonRow> buttons();
}
