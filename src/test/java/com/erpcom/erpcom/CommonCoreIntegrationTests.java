package com.erpcom.erpcom;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.erpcom.erpcom.common.master.*;
import com.erpcom.erpcom.common.message.*;
import com.erpcom.erpcom.common.permission.*;
import com.erpcom.erpcom.common.permission.PermissionModels.*;
import com.erpcom.erpcom.common.error.ApiException;
import com.erpcom.erpcom.common.error.ApiErrorWriter;
import com.erpcom.erpcom.common.web.CurrentUserProvider;
import com.erpcom.erpcom.common.reference.ReferenceDataService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:erpcom;MODE=Oracle;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=", "spring.flyway.enabled=false",
    "erpcom.cors.allowed-origin=http://localhost:9093", "erpcom.security.swagger-enabled=false"
})
@AutoConfigureMockMvc
@Import(TestDatabaseConfiguration.class)
@ActiveProfiles("test")
@Transactional
class CommonCoreIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired MasterService masters;
    @Autowired MessageService messages;
    @Autowired PermissionService permissions;
    @Autowired ReferenceDataService referenceData;
    @Autowired ApiErrorWriter errorWriter;
    @MockitoBean CurrentUserProvider identity;
    @BeforeEach void users() {
        jdbc.update("INSERT INTO TB_USER (USER_ID,LOGIN_ID,USER_NAME,COMPANY_ID,SITE_ID,DEPT_ID,LANG_CODE,CREATED_BY) VALUES (1,'admin','Admin',100,1000,10000,'en',0)");
        jdbc.update("INSERT INTO TB_USER (USER_ID,LOGIN_ID,USER_NAME,COMPANY_ID,SITE_ID,DEPT_ID,CREATED_BY) VALUES (2,'viewer','Viewer',100,1000,10000,0)");
        jdbc.update("INSERT INTO TB_USER_ROLE (USER_ID,ROLE_ID,VALID_FROM,CREATED_BY) VALUES (1,10,SYSDATE-1,0)");
        jdbc.update("INSERT INTO TB_USER_ROLE (USER_ID,ROLE_ID,VALID_FROM,CREATED_BY) VALUES (2,50,SYSDATE-1,0)");
    }
    @Test void allTablesLoad() {
        assertThat(MasterCatalog.RESOURCES).hasSize(18);
        for (var resource : MasterCatalog.RESOURCES) assertThat(masters.list(resource.name(),0,200,true)).isNotNull();
    }
    @Test void anonymousRequestsCannotSpoofActor() throws Exception {
        mvc.perform(get("/api/common/admin/users").header("X-User-Id","1").header("Accept-Language","en"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.messageCode").value("ERR_AUTH_REQUIRED"))
            .andExpect(jsonPath("$.message").value("Authentication is required."));
        mvc.perform(get("/api/common/permissions/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/common/languages")).andExpect(status().isOk());
    }
    @Test void nexacroPreflightIsAllowedWithoutBypassingAuthentication() throws Exception {
        mvc.perform(options("/api/common/admin/users").header("Origin","http://localhost:9093")
            .header("Access-Control-Request-Method","GET")
            .header("Access-Control-Request-Headers","Authorization,Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin","http://localhost:9093"))
            .andExpect(header().string("Access-Control-Allow-Credentials","true"));
        mvc.perform(get("/api/common/admin/users").header("Origin","http://localhost:9093"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("Access-Control-Allow-Origin","http://localhost:9093"));
    }
    @Test void unregisteredCorsOriginIsDenied() throws Exception {
        mvc.perform(options("/api/common/admin/users").header("Origin","http://untrusted.example")
            .header("Access-Control-Request-Method","GET"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
    @Test void apiDocumentationIsDisabledByDefault() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }
    @Test void messageFallbackUsesConfiguredChainAndStatus() {
        assertThat(messages.get("ERR_NOT_FOUND","ja").langCode()).isEqualTo("en");
        assertThat(messages.get("ERR_NOT_FOUND","ja").httpStatus()).isEqualTo(404);
        assertThat(messages.language("en","ko","ja")).isEqualTo("en");
        assertThat(messages.language(null,null,"zh-TW;q=0.9,en;q=0.8")).isEqualTo("zh-TW");
        assertThat(messages.get("UNKNOWN","ko").httpStatus()).isEqualTo(500);
    }
    @Test void languageCyclesTerminate() {
        jdbc.update("UPDATE TB_LANGUAGE SET FALLBACK_LANG_CODE='ja' WHERE LANG_CODE='en'");
        assertThat(messages.get("ERR_AUTH_REQUIRED","ja").langCode()).isEqualTo("en");
        assertThat(messages.text("ERR_MESSAGE_NOT_FOUND","en","missing")).contains("missing");
    }
    @Test void inactiveFallbackLanguageAndMalformedHeaderUseActiveDefault() {
        jdbc.update("UPDATE TB_LANGUAGE SET USE_YN='N' WHERE LANG_CODE='en'");
        assertThat(messages.get("ERR_NOT_FOUND","ja").langCode()).isEqualTo("ko");
        jdbc.update("UPDATE TB_LANGUAGE SET DEFAULT_YN='N' WHERE LANG_CODE='ko'");
        jdbc.update("UPDATE TB_LANGUAGE SET USE_YN='Y',DEFAULT_YN='Y' WHERE LANG_CODE='en'");
        assertThat(messages.language(null,null,"bad header !")).isEqualTo("en");
    }
    @Test void standardHttpErrorsUseCommonFormat() throws Exception {
        mvc.perform(post("/api/common/languages")).andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.messageCode").value("ERR_METHOD_NOT_ALLOWED"));
        mvc.perform(get("/api/common/languages").header("Accept","application/pdf"))
            .andExpect(status().isNotAcceptable()).andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.messageCode").value("ERR_NOT_ACCEPTABLE"));
        when(identity.userId(any())).thenReturn(1L);
        mvc.perform(post("/api/common/admin/companies").contentType("text/plain").content("bad"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.messageCode").value("ERR_UNSUPPORTED_MEDIA_TYPE"));
    }
    @Test void crudControlsAuditVersionAndLogicalDeletion() {
        var created = masters.create("companies",req(Map.of("COMPANY_CODE","TEST","COMPANY_NAME","Test")),1);
        String key = created.get("COMPANY_ID").toString();
        assertThat(((Number)created.get("CREATED_BY")).longValue()).isEqualTo(1);
        var updated = masters.update("companies",key,new MasterRequest(Map.of("COMPANY_NAME","Changed"),0L),1);
        assertThat(((Number)updated.get("VERSION_NO")).longValue()).isEqualTo(1);
        assertThat(((Number)updated.get("UPDATED_BY")).longValue()).isEqualTo(1);
        code("ERR_CONFLICT", () -> masters.update("companies",key,new MasterRequest(Map.of("COMPANY_NAME","Stale"),0L),1));
        masters.deactivate("companies",key,1,1);
        assertThat(masters.get("companies",key).get("USE_YN")).isEqualTo("N");
        assertThat(masters.list("companies",0,200,false).items()).noneMatch(row -> row.get("COMPANY_ID").toString().equals(key));
    }
    @Test void identifiersAndPrivateColumnsCannotBeInjected() {
        code("ERR_NOT_FOUND", () -> masters.list("users WHERE 1=1",0,10,true));
        code("ERR_INVALID_INPUT", () -> masters.create("users",req(Map.of("PASSWORD_HASH","secret")),1));
        code("ERR_INVALID_INPUT", () -> masters.update("companies","100",new MasterRequest(Map.of("CREATED_BY",99),0L),1));
        assertThat(masters.get("users","1")).doesNotContainKeys("PASSWORD_HASH","FAILED_LOGIN_COUNT","AUTH_TYPE");
        code("ERR_INVALID_INPUT", () -> masters.list("users",0,201,true));
    }
    @Test void compositeKeysAndNullableOverridesWork() {
        var values = new LinkedHashMap<String,Object>();
        values.put("USER_ID",2); values.put("MENU_ID",1010); values.put("ACCESS_LEVEL",null); values.put("DATA_SCOPE","DEPT");
        masters.create("user-menu-permissions",req(values),1);
        assertThat(permissions.get(2).menus().stream().filter(m -> m.menuId()==1010).findFirst().orElseThrow().grants())
            .containsExactly(new Grant(AccessLevel.READ_ALL,DataScope.DEPT));
        assertThat(masters.get("codes","USER_STATUS~ACTIVE").get("CODE_NAME_MSG_CODE")).isEqualTo("COD_USER_STATUS_ACTIVE");
    }
    @Test void crossCompanyOrganizationAndHierarchyCyclesRejected() {
        var company = masters.create("companies",req(Map.of("COMPANY_CODE","OTHER","COMPANY_NAME","Other")),1);
        code("ERR_INVALID_INPUT", () -> masters.create("departments",req(Map.of("COMPANY_ID",company.get("COMPANY_ID"),
            "SITE_ID",1000,"DEPT_CODE","BAD","DEPT_NAME","Invalid")),1));
        code("ERR_INVALID_INPUT", () -> masters.update("departments","10000",new MasterRequest(Map.of("PARENT_DEPT_ID",10000),0L),1));
        code("ERR_INVALID_INPUT", () -> masters.update("menus","1000",new MasterRequest(Map.of("PARENT_MENU_ID",1010),0L),1));
    }
    @Test void expiredInactiveAndFutureRolesAreExcluded() {
        jdbc.update("UPDATE TB_USER_ROLE SET VALID_TO=SYSDATE-1 WHERE USER_ID=2");
        assertThat(permissions.get(2).menus()).isEmpty();
        jdbc.update("UPDATE TB_USER_ROLE SET VALID_TO=NULL,USE_YN='N' WHERE USER_ID=2");
        assertThat(permissions.get(2).menus()).isEmpty();
        jdbc.update("UPDATE TB_USER_ROLE SET USE_YN='Y',VALID_FROM=SYSDATE+1 WHERE USER_ID=2");
        assertThat(permissions.get(2).menus()).isEmpty();
    }
    @Test void userDeniesOverrideRoleAllowsAndMenuGatesButtons() {
        permissions.requireButton(2,"PM_PROJECT_LIST","BTN_SEARCH",AccessLevel.READ_PARTIAL);
        masters.create("user-button-permissions",req(Map.of("USER_ID",2,"MENU_ID",1010,"BUTTON_ID",1,"ACCESS_YN","N")),1);
        code("ERR_ACCESS_DENIED", () -> permissions.requireButton(2,"PM_PROJECT_LIST","BTN_SEARCH",AccessLevel.READ_PARTIAL));
        masters.create("user-menu-permissions",req(Map.of("USER_ID",2,"MENU_ID",1020,"ACCESS_LEVEL","NONE")),1);
        code("ERR_ACCESS_DENIED", () -> permissions.requireButton(2,"PM_WBS","BTN_SEARCH",AccessLevel.READ_PARTIAL));
    }
    @Test void roleUnionDoesNotWidenWriteScope() {
        jdbc.update("INSERT INTO TB_USER_ROLE (USER_ID,ROLE_ID,VALID_FROM,CREATED_BY) VALUES (2,20,SYSDATE-1,0)");
        jdbc.update("UPDATE TB_ROLE_MENU_AUTH SET ACCESS_LEVEL='READ_ALL',DATA_SCOPE='COMPANY' WHERE ROLE_ID=50 AND MENU_ID=1010");
        jdbc.update("UPDATE TB_ROLE_MENU_AUTH SET ACCESS_LEVEL='WRITE_PARTIAL',DATA_SCOPE='OWN' WHERE ROLE_ID=20 AND MENU_ID=1010");
        permissions.requireData(2,1010,AccessLevel.READ_ALL,new DataTarget(100,1000L,10000L,1L));
        permissions.requireData(2,1010,AccessLevel.WRITE_PARTIAL,new DataTarget(100,1000L,10000L,2L));
        code("ERR_ACCESS_DENIED", () -> permissions.requireData(2,1010,AccessLevel.WRITE_PARTIAL,new DataTarget(100,1000L,10000L,1L)));
        code("ERR_ACCESS_DENIED", () -> permissions.requireData(2,1010,AccessLevel.READ_ALL,new DataTarget(999,1000L,10000L,2L)));
    }
    @Test void overlappingAssignmentsAndInvalidPeriodsRejected() {
        code("ERR_CONFLICT", () -> masters.create("user-roles",req(Map.of("USER_ID",2,"ROLE_ID",50)),1));
        masters.create("user-menu-permissions",req(Map.of("USER_ID",2,"MENU_ID",1010,"ACCESS_LEVEL","READ_ALL")),1);
        code("ERR_CONFLICT", () -> masters.create("user-menu-permissions",req(Map.of("USER_ID",2,"MENU_ID",1010,"ACCESS_LEVEL","NONE")),1));
        code("ERR_INVALID_INPUT", () -> masters.create("user-roles",req(Map.of("USER_ID",2,"ROLE_ID",20,
            "VALID_FROM","2030-01-02T00:00:00","VALID_TO","2030-01-01T00:00:00")),1));
    }
    @Test void disabledMasterAndUserRevokePermissionsImmediately() {
        permissions.requireButton(1,"COMMON_ADMIN","BTN_SAVE",AccessLevel.WRITE_ALL);
        jdbc.update("UPDATE TB_MENU_BUTTON SET USE_YN='N' WHERE MENU_ID=9000 AND BUTTON_ID=3");
        code("ERR_ACCESS_DENIED", () -> permissions.requireButton(1,"COMMON_ADMIN","BTN_SAVE",AccessLevel.WRITE_ALL));
        jdbc.update("UPDATE TB_USER SET USE_YN='N' WHERE USER_ID=2");
        code("ERR_AUTH_REQUIRED", () -> permissions.get(2));
    }
    @Test void authenticatedAdminApiAndViewerDenial() throws Exception {
        when(identity.userId(any())).thenReturn(1L);
        mvc.perform(post("/api/common/admin/companies").contentType("application/json")
            .content("{\"values\":{\"COMPANY_CODE\":\"HTTP\",\"COMPANY_NAME\":\"HTTP Company\"}}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.CREATED_BY").value(1));
        mvc.perform(put("/api/common/admin/companies/100").contentType("application/json")
            .content("{\"values\":{\"COMPANY_NAME\":\"Changed\"}}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.messageCode").value("ERR_INVALID_INPUT"));
        when(identity.userId(any())).thenReturn(2L);
        mvc.perform(get("/api/common/admin/users")).andExpect(status().isForbidden());
        mvc.perform(get("/api/common/bootstrap")).andExpect(status().isOk())
            .andExpect(jsonPath("$.user.userId").value(2));
    }
    @Test void referenceDataIsLocalizedAndFilteredByEffectivePermissions() {
        var bootstrap = referenceData.bootstrap(2,"en");
        assertThat(bootstrap.menus()).noneMatch(m -> m.menuCode().equals("COMMON_ADMIN"));
        var project = bootstrap.menus().stream().filter(m -> m.menuId()==1010).findFirst().orElseThrow();
        assertThat(project.buttons()).extracting(b -> b.buttonCode()).containsExactly("BTN_SEARCH","BTN_EXCEL");
        assertThat(referenceData.codes(2,"USER_STATUS","en").getFirst().name()).isEqualTo("Active");
    }
    @Test void limitedScopeCannotAccessCentralAdministration() throws Exception {
        when(identity.userId(any())).thenReturn(1L);
        jdbc.update("UPDATE TB_ROLE_MENU_AUTH SET DATA_SCOPE='OWN' WHERE ROLE_ID=10 AND MENU_ID=9000");
        mvc.perform(get("/api/common/admin/users")).andExpect(status().isForbidden());
        mvc.perform(post("/api/common/admin/companies").contentType("application/json")
            .content("{\"values\":{\"COMPANY_CODE\":\"DENY\",\"COMPANY_NAME\":\"Denied\"}}"))
            .andExpect(status().isForbidden());
    }
    @Test void filterErrorsUseSameMessageAndStatus() throws Exception {
        var request = new MockHttpServletRequest("GET","/api/common/admin/users");
        request.addHeader("Accept-Language","en");
        var response = new MockHttpServletResponse();
        errorWriter.write(request,response,new ApiException("ERR_AUTH_REQUIRED"));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("ERR_AUTH_REQUIRED","Authentication is required.");
    }
    @Test void everyResourceSupportsCreateUpdateDeactivateAndReenable() {
        var newMenu = masters.create("menus",req(Map.of("MENU_CODE","TEST_MENU","MENU_NAME_MSG_CODE","MNU_PROJECT_LIST")),1);
        Object menuId = newMenu.get("MENU_ID");
        var inputs = new LinkedHashMap<String,Map<String,Object>>();
        inputs.put("languages",Map.of("LANG_CODE","fr","LANG_NAME","French","FALLBACK_LANG_CODE","en"));
        inputs.put("messages",Map.of("MESSAGE_CODE","LBL_TEST","MESSAGE_TYPE","LABEL"));
        inputs.put("message-translations",Map.of("MESSAGE_CODE","LBL_TEST","LANG_CODE","fr","MESSAGE_TEXT","Test"));
        inputs.put("companies",Map.of("COMPANY_CODE","ALL","COMPANY_NAME","All"));
        inputs.put("sites",Map.of("COMPANY_ID",100,"SITE_CODE","ALL","SITE_NAME","All"));
        inputs.put("departments",Map.of("COMPANY_ID",100,"SITE_ID",1000,"DEPT_CODE","ALL","DEPT_NAME","All"));
        inputs.put("users",Map.of("COMPANY_ID",100,"SITE_ID",1000,"DEPT_ID",10000,"LOGIN_ID","all","USER_NAME","All"));
        inputs.put("roles",Map.of("ROLE_CODE","ALL","ROLE_NAME_MSG_CODE","ROLE_VIEWER"));
        inputs.put("user-roles",Map.of("USER_ID",2,"ROLE_ID",20));
        inputs.put("menus",Map.of("MENU_CODE","ALL","MENU_NAME_MSG_CODE","MNU_PROJECT_LIST"));
        inputs.put("buttons",Map.of("BUTTON_CODE","ALL","BUTTON_NAME_MSG_CODE","BTN_SAVE"));
        inputs.put("menu-buttons",Map.of("MENU_ID",menuId,"BUTTON_ID",1));
        inputs.put("role-menu-permissions",Map.of("ROLE_ID",10,"MENU_ID",menuId,"ACCESS_LEVEL","READ_ALL"));
        inputs.put("user-menu-permissions",Map.of("USER_ID",2,"MENU_ID",menuId,"ACCESS_LEVEL","READ_ALL"));
        inputs.put("role-button-permissions",Map.of("ROLE_ID",10,"MENU_ID",menuId,"BUTTON_ID",1));
        inputs.put("user-button-permissions",Map.of("USER_ID",2,"MENU_ID",menuId,"BUTTON_ID",1,"ACCESS_YN","Y"));
        inputs.put("code-groups",Map.of("CODE_GROUP","ALL"));
        inputs.put("codes",Map.of("CODE_GROUP","ALL","CODE","TEST","CODE_NAME_MSG_CODE","COD_USER_STATUS_ACTIVE"));
        var createdRows = new LinkedHashMap<String,Map<String,Object>>();
        // Keep referenced rows active until every dependent row has been created.
        inputs.forEach((name,values) -> createdRows.put(name,masters.create(name,req(values),1)));
        createdRows.forEach((name,row) -> {
            var resource = MasterResource.named(name);
            String key = resource.keys().stream().map(k -> row.get(k).toString()).collect(java.util.stream.Collectors.joining("~"));
            masters.update(name,key,new MasterRequest(Map.of("USE_YN","Y"),0L),1);
            masters.deactivate(name,key,1,1);
            assertThat(masters.get(name,key).get("USE_YN")).isEqualTo("N");
            masters.update(name,key,new MasterRequest(Map.of("USE_YN","Y"),2L),1);
            assertThat(((Number)masters.get(name,key).get("VERSION_NO")).longValue()).isEqualTo(3);
        });
    }
    private MasterRequest req(Map<String,Object> values) { return new MasterRequest(values,null); }
    private void code(String expected, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(ApiException.class).extracting(e -> ((ApiException)e).messageCode()).isEqualTo(expected);
    }
}
