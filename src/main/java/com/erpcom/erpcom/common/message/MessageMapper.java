package com.erpcom.erpcom.common.message;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MessageMapper {
    List<LanguageInfo> languages();
    MessageInfo find(@Param("code") String code, @Param("language") String language);
}
