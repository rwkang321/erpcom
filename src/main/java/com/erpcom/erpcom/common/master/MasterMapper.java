package com.erpcom.erpcom.common.master;

import java.sql.Timestamp;
import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MasterMapper {
    List<Map<String, Object>> list(@Param("resource") MasterResource resource,
            @Param("offset") int offset, @Param("limit") int limit, @Param("includeInactive") boolean includeInactive);
    long count(@Param("resource") MasterResource resource, @Param("includeInactive") boolean includeInactive);
    Map<String, Object> find(@Param("resource") MasterResource resource, @Param("keys") Map<String, Object> keys);
    long nextId();
    Timestamp now();
    int insert(@Param("resource") MasterResource resource, @Param("values") Map<String, Object> values,
               @Param("actor") long actor);
    int update(@Param("resource") MasterResource resource, @Param("keys") Map<String, Object> keys,
               @Param("values") Map<String, Object> values, @Param("version") long version, @Param("actor") long actor);
    int deactivate(@Param("resource") MasterResource resource, @Param("keys") Map<String, Object> keys,
                   @Param("version") long version, @Param("actor") long actor);
    Long lockUser(long userId);
    void lockHierarchy(@Param("resource") MasterResource resource);
    long overlaps(@Param("resource") MasterResource resource, @Param("partition") Map<String, Object> partition,
                  @Param("identity") Object identity, @Param("from") Timestamp from, @Param("to") Timestamp to);
}
