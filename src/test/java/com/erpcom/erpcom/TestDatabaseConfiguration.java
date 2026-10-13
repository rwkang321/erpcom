package com.erpcom.erpcom;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;
import javax.sql.DataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.*;
import org.springframework.jdbc.datasource.init.*;

@TestConfiguration(proxyBeanMethods = false)
public class TestDatabaseConfiguration {
    @Bean DataSourceInitializer testSchema(DataSource dataSource) throws java.io.IOException {
        StringBuilder sql = new StringBuilder();
        for (String migration : new String[]{"V001__create_common_tables.sql", "V002__seed_common_reference_data.sql", "V003__harden_common_core.sql"}) {
            String source = new ClassPathResource("db/migration/" + migration)
                .getContentAsString(StandardCharsets.UTF_8).replaceAll("(?m)^\\s*--.*$", "");
            source = source.replaceAll("(?s)CREATE UNIQUE INDEX UX_(?:USER_LOGIN_UPPER|USER_ROLE_OPEN_ACTIVE|UMA_OPEN_ACTIVE|UBA_OPEN_ACTIVE|LANGUAGE_DEFAULT).*?;", "");
            Matcher blocks = Pattern.compile("(?ms)^INSERT ALL\\s+(.*?)^SELECT 1 FROM DUAL;").matcher(source);
            StringBuffer expanded = new StringBuffer();
            while (blocks.find()) {
                Matcher inserts = Pattern.compile("(?s)\\s*INTO\\s+(.*?)(?=\\s+INTO\\s+|\\z)").matcher(blocks.group(1));
                StringBuilder replacement = new StringBuilder();
                while (inserts.find()) replacement.append("INSERT INTO ").append(inserts.group(1).trim()).append(";\n");
                blocks.appendReplacement(expanded, Matcher.quoteReplacement(replacement.toString()));
            }
            blocks.appendTail(expanded);
            sql.append(expanded).append("\n");
        }
        sql.append("""
            ALTER TABLE TB_USER ADD LOGIN_ID_UPPER VARCHAR2(100) GENERATED ALWAYS AS (UPPER(LOGIN_ID));
            CREATE UNIQUE INDEX UX_TEST_USER_LOGIN ON TB_USER(LOGIN_ID_UPPER);
            ALTER TABLE TB_LANGUAGE ADD ACTIVE_DEFAULT NUMBER GENERATED ALWAYS AS
              (CASE WHEN DEFAULT_YN='Y' AND USE_YN='Y' THEN 1 END);
            CREATE UNIQUE INDEX UX_TEST_LANGUAGE_DEFAULT ON TB_LANGUAGE(ACTIVE_DEFAULT);
            """);
        var initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(new ResourceDatabasePopulator(new ByteArrayResource(sql.toString().getBytes(StandardCharsets.UTF_8))));
        return initializer;
    }
}
