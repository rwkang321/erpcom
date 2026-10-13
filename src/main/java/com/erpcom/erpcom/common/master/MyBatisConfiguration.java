package com.erpcom.erpcom.common.master;

import java.util.Properties;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisConfiguration {
    @Bean
    DatabaseIdProvider databaseIdProvider() {
        var provider = new VendorDatabaseIdProvider();
        var properties = new Properties();
        properties.setProperty("Oracle", "oracle");
        properties.setProperty("H2", "h2");
        provider.setProperties(properties);
        return provider;
    }
}
