package com.erpcom.erpcom;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import com.erpcom.erpcom.common.web.CommonCorsProperties;
import static org.assertj.core.api.Assertions.*;

class DevelopmentConfigurationTests {
    @TempDir Path directory;

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CommonCorsProperties.class)
    static class BindingConfiguration {}

    private ApplicationContextRunner dev(String... overrides) throws java.io.IOException {
        // Synthetic settings: never import the developer's actual credentials during tests.
        Path settings = directory.resolve("mysettings.ini");
        Files.writeString(settings, """
            ERPCOM_API_PORT:8183
            ERPCOM_DB_URL=jdbc:oracle:thin:@test.invalid:1521/TEST
            ERPCOM_DB_USERNAME=test_schema
            ERPCOM_DB_PASSWORD=synthetic_test_value
            ERPCOM_DB_MIN_IDLE=2
            ERPCOM_DB_MAX_POOL_SIZE=7
            ERPCOM_DB_CONNECTION_TIMEOUT_MS=12000
            ERPCOM_FLYWAY_ENABLED=true
            ERPCOM_FLYWAY_BASELINE_ON_MIGRATE=true
            ERPCOM_FLYWAY_BASELINE_VERSION=0
            ERPCOM_SWAGGER_ENABLED=false
            ERPCOM_NEXACRO_ORIGIN=http://localhost:9193
            ERPCOM_SESSION_TTL_HOURS=9
            ERPCOM_LOGIN_FAILURE_THRESHOLD=6
            ERPCOM_LOGIN_LOCK_MINUTES=20
            ERPCOM_BCRYPT_STRENGTH=13
            ERPCOM_BOOTSTRAP_COMPANY_CODE=TEST_COMPANY
            ERPCOM_ADMIN_LOGIN_ID=TEST_ADMIN
            ERPCOM_ADMIN_NAME=TEST_MANAGER
            """, StandardCharsets.UTF_8);
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(BindingConfiguration.class)
                .withPropertyValues("spring.profiles.active=dev",
                        "ERPCOM_SETTINGS_LOCATION=" + settings.toUri() + "[.properties]")
                .withPropertyValues(overrides);
    }

    @Test void devImportsIniAndResolvesApplicationSettings() throws Exception {
        dev().run(context -> {
            assertThat(context).hasNotFailed();
            var environment = context.getEnvironment();
            assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8183);
            assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("test_schema");
            assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("synthetic_test_value");
            assertThat(environment.getProperty("spring.datasource.hikari.minimum-idle", Integer.class)).isEqualTo(2);
            assertThat(environment.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class)).isEqualTo(7);
            assertThat(environment.getProperty("spring.datasource.hikari.connection-timeout", Long.class)).isEqualTo(12000L);
            assertThat(environment.getProperty("spring.flyway.baseline-version")).isEqualTo("0");
            assertThat(environment.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration");
            assertThat(environment.getProperty("springdoc.api-docs.enabled", Boolean.class)).isFalse();
            assertThat(environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();
            assertThat(environment.getProperty("mybatis.configuration.local-cache-scope")).isEqualTo("STATEMENT");
            assertThat(context.getBean(CommonCorsProperties.class).allowedOrigin()).isEqualTo("http://localhost:9193");
            assertThat(environment.getProperty("erpcom.auth.session-ttl-hours", Integer.class)).isEqualTo(9);
            assertThat(environment.getProperty("erpcom.auth.local-login.failure-threshold", Integer.class)).isEqualTo(6);
            assertThat(environment.getProperty("erpcom.bootstrap.admin-company-code")).isEqualTo("TEST_COMPANY");
        });
    }

    @Test void explicitOverridesWinOverPrivateIni() throws Exception {
        dev("ERPCOM_API_PORT=8283", "ERPCOM_DB_USERNAME=override_schema",
                "ERPCOM_SWAGGER_ENABLED=true", "ERPCOM_FLYWAY_BASELINE_ON_MIGRATE=false").run(context -> {
            assertThat(context).hasNotFailed();
            var environment = context.getEnvironment();
            assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8283);
            assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("override_schema");
            assertThat(environment.getProperty("springdoc.api-docs.enabled", Boolean.class)).isTrue();
            assertThat(environment.getProperty("spring.flyway.baseline-on-migrate", Boolean.class)).isFalse();
        });
    }

    @Test void missingOptionalIniStillAllowsEnvironmentConfiguration() {
        new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(BindingConfiguration.class)
                .withPropertyValues("spring.profiles.active=dev",
                        "ERPCOM_SETTINGS_LOCATION=optional:" + directory.resolve("absent.ini").toUri() + "[.properties]",
                        "ERPCOM_DB_USERNAME=environment_schema", "ERPCOM_DB_PASSWORD=synthetic_value")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment().getProperty("spring.datasource.username")).isEqualTo("environment_schema");
                });
    }

    @Test void nonDevProfileDoesNotImportPrivateIni() throws Exception {
        dev("spring.profiles.active=test").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getProperty("ERPCOM_ADMIN_NAME")).isNull();
        });
    }

    @Test void corsRejectsWildcardAndOriginsWithPaths() throws Exception {
        for (String origin : new String[]{"*", "http://localhost:9093/app", "http://user:password@localhost:9093"}) {
            dev("erpcom.cors.allowed-origin=" + origin).run(context -> assertThat(context).hasFailed());
        }
    }
}
