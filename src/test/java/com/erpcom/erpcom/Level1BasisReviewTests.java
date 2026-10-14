package com.erpcom.erpcom;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.erpcom.erpcom.common.master.*;
import com.erpcom.erpcom.common.basis.BasisService;
import com.erpcom.erpcom.common.error.ApiException;
import com.erpcom.erpcom.common.web.CurrentUserProvider;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
class Level1BasisReviewTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired MasterService masters;
    @Autowired BasisService basis;
    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider identity;
    @BeforeEach void users() {
        jdbc.update("INSERT INTO TB_USER (USER_ID,LOGIN_ID,USER_NAME,COMPANY_ID,SITE_ID,DEPT_ID,LANG_CODE,CREATED_BY) VALUES (1,'admin','Admin',100,1000,10000,'ko',0)");
        jdbc.update("INSERT INTO TB_USER (USER_ID,LOGIN_ID,USER_NAME,COMPANY_ID,SITE_ID,DEPT_ID,CREATED_BY) VALUES (2,'viewer','Viewer',100,1000,10000,0)");
        jdbc.update("INSERT INTO TB_USER_ROLE (USER_ID,ROLE_ID,VALID_FROM,CREATED_BY) VALUES (1,10,SYSDATE-1,0)");
        jdbc.update("INSERT INTO TB_USER_ROLE (USER_ID,ROLE_ID,VALID_FROM,CREATED_BY) VALUES (2,50,SYSDATE-1,0)");
    }
    private MasterRequest request(Map<String,Object> values) { return new MasterRequest(values,null); }
    private void error(String code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException)e).messageCode()).isEqualTo(code);
    }
    @Test void schemaHas21MastersAndZeroVersions() {
        assertThat(MasterCatalog.RESOURCES).hasSize(21);
        for (var resource: MasterCatalog.RESOURCES)
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + resource.table() + " WHERE VERSION_NO<>0",Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_UNIT",Long.class)).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_CURRENCY",Long.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_MENU_AMOUNT_FORMAT",Long.class)).isEqualTo(3);
    }
    @Test void anonymousReferenceQueriesAndHeaderSpoofingAreRejected() throws Exception {
        mvc.perform(get("/api/common/units").header("X-User-Id","1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/common/currencies")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/common/amount-formats/PM_TOTAL_ESTIMATED_COST")).andExpect(status().isUnauthorized());
    }
    @Test void quantitiesUseExactMassDecimalAndBinaryConversions() {
        assertThat(basis.convertQuantity(1,new BigDecimal("1.25"),"KG","G")).isEqualByComparingTo("1250");
        assertThat(basis.convertQuantity(1,BigDecimal.ONE,"GB","MB")).isEqualByComparingTo("1000");
        assertThat(basis.convertQuantity(1,BigDecimal.ONE,"GIB","MIB")).isEqualByComparingTo("1024");
        error("ERR_UNIT_INCOMPATIBLE", () -> basis.convertQuantity(1,BigDecimal.ONE,"KG","EA"));
        error("ERR_UNIT_INCOMPATIBLE", () -> basis.convertQuantity(1,BigDecimal.ONE,"EA","PCS"));
        error("ERR_UNIT_INCOMPATIBLE", () -> basis.convertQuantity(1,BigDecimal.ONE,"GB","GIB"));
        error("ERR_UNIT_INCOMPATIBLE", () -> basis.convertQuantity(1,BigDecimal.ONE,"BOX","EA"));
        error("ERR_INVALID_INPUT", () -> basis.convertQuantity(1,new BigDecimal("0.5"),"EA","EA"));
    }
    @Test void amountDisplayPreservesOriginalAndUsesFieldThenMenuThenSystemFallback() {
        var original=new BigDecimal("59230000000");
        var total=basis.displayAmount(1,"PM_TOTAL_ESTIMATED_COST","TOTAL_ESTIMATED_COST",original,"KRW","ko");
        assertThat(total.displayValue()).isEqualTo("592.3");
        assertThat(total.originalAmount()).isEqualTo("59230000000");
        assertThat(total.unitLabel()).isEqualTo("억");
        assertThat(basis.displayAmount(1,"PM_TOTAL_ESTIMATED_COST","OTHER_TOTAL",original,"KRW","ko").displayValue()).isEqualTo("592.3");
        assertThat(basis.displayAmount(1,"PM_TOTAL_ESTIMATED_COST","UNIT_PRICE",new BigDecimal("1250.5"),"KRW","ko").displayValue()).isEqualTo("1251");
        assertThat(basis.displayAmount(1,"PM_PROJECT_LIST","ANY_AMOUNT",new BigDecimal("1250.567"),"USD","en").displayValue()).isEqualTo("1250.57");
        error("ERR_CURRENCY_MISMATCH", () -> basis.displayAmount(1,"PM_TOTAL_ESTIMATED_COST","TOTAL_ESTIMATED_COST",original,"USD","ko"));
    }
    @Test void policyLookupRequiresItsMenuAndAdminWritesRequireButtons() throws Exception {
        error("ERR_ACCESS_DENIED", () -> basis.amountFormats(2,"COMMON_ADMIN","en"));
        when(identity.userId(any())).thenReturn(2L);
        mvc.perform(post("/api/common/admin/currencies").contentType("application/json")
            .content("{\"values\":{\"CURRENCY_CODE\":\"GBP\",\"CURRENCY_NAME_MSG_CODE\":\"COD_CURRENCY_USD\",\"MINOR_UNIT_DIGITS\":2}}"))
            .andExpect(status().isForbidden());
        when(identity.userId(any())).thenReturn(1L);
        mvc.perform(get("/api/common/currencies")).andExpect(status().isOk());
    }
    @Test void decimalFactorsRemainDecimalsAndRejectPrecisionLoss() {
        var fields=new java.util.HashMap<String,Object>();
        fields.put("UNIT_CODE","MG"); fields.put("UNIT_NAME_MSG_CODE","COD_UNIT_G"); fields.put("UNIT_TYPE_CODE","MASS");
        fields.put("BASE_UNIT_CODE","G"); fields.put("TO_BASE_FACTOR","0.001");
        var mg=masters.create("units",request(fields),1);
        assertThat(new BigDecimal(mg.get("TO_BASE_FACTOR").toString())).isEqualByComparingTo("0.001");
        fields.put("UNIT_CODE","TOO_FINE"); fields.put("TO_BASE_FACTOR","0.0000000000000000001");
        error("ERR_INVALID_INPUT", () -> masters.create("units",request(fields),1));
        fields.put("TO_BASE_FACTOR","100000000000000000000");
        error("ERR_INVALID_INPUT", () -> masters.create("units",request(fields),1));
    }
    @Test void sameDimensionCanonicalRootsAndStableUnitDefinitionsAreEnforced() {
        error("ERR_INVALID_INPUT", () -> masters.create("units",request(Map.of("UNIT_CODE","BAD","UNIT_NAME_MSG_CODE","COD_UNIT_KG",
            "UNIT_TYPE_CODE","MASS","BASE_UNIT_CODE","EA","TO_BASE_FACTOR","1")),1));
        error("ERR_INVALID_INPUT", () -> masters.create("units",request(Map.of("UNIT_CODE","CHAIN","UNIT_NAME_MSG_CODE","COD_UNIT_KG",
            "UNIT_TYPE_CODE","MASS","BASE_UNIT_CODE","KG","TO_BASE_FACTOR","2")),1));
        error("ERR_CONFLICT", () -> masters.update("units","KG",new MasterRequest(Map.of("TO_BASE_FACTOR","999"),0L),1));
        error("ERR_CONFLICT", () -> masters.deactivate("units","G",0,1));
        error("ERR_CONFLICT", () -> masters.deactivate("codes","UNIT_TYPE~MASS",0,1));
        error("ERR_CONFLICT", () -> masters.deactivate("currencies","KRW",0,1));
    }
    @Test void newMasterCrudVersionsAndAuditUseActor() {
        masters.create("units",request(Map.of("UNIT_CODE","TEST_EA","UNIT_NAME_MSG_CODE","COD_UNIT_EA","UNIT_TYPE_CODE","COUNT")),1);
        var unit=masters.update("units","TEST_EA",new MasterRequest(Map.of("SORT_ORDER",20),0L),2);
        assertThat(((Number)unit.get("UPDATED_BY")).longValue()).isEqualTo(2);
        error("ERR_CONFLICT", () -> masters.update("units","TEST_EA",new MasterRequest(Map.of("SORT_ORDER",30),0L),1));
        masters.deactivate("units","TEST_EA",1,1);
        assertThat(masters.get("units","TEST_EA").get("USE_YN")).isEqualTo("N");
        masters.update("units","TEST_EA",new MasterRequest(Map.of("USE_YN","Y"),2L),1);
        masters.create("currencies",request(Map.of("CURRENCY_CODE","GBP","CURRENCY_NAME_MSG_CODE","COD_CURRENCY_USD","MINOR_UNIT_DIGITS",2,"NUMERIC_CODE","826")),1);
        masters.update("currencies","GBP",new MasterRequest(Map.of("SORT_ORDER",90),0L),1);
        masters.deactivate("currencies","GBP",1,1);
        masters.create("menu-amount-formats",request(Map.of("MENU_ID",1010,"FIELD_CODE","DEFAULT","DISPLAY_DIVISOR","1000",
            "DECIMAL_PLACES",1,"ROUNDING_MODE","HALF_EVEN","UNIT_LABEL_MSG_CODE","LBL_AMOUNT_UNIT_THOUSAND")),1);
        masters.update("menu-amount-formats","1010~DEFAULT",new MasterRequest(Map.of("DECIMAL_PLACES",2),0L),1);
        masters.deactivate("menu-amount-formats","1010~DEFAULT",1,1);
    }
    @Test void invalidCodesScalesDivisorsAndRoundingAreRejected() {
        error("ERR_INVALID_INPUT", () -> masters.create("currencies",request(Map.of("CURRENCY_CODE","usd","CURRENCY_NAME_MSG_CODE","COD_CURRENCY_USD","MINOR_UNIT_DIGITS",2)),1));
        error("ERR_INVALID_INPUT", () -> masters.update("currencies","USD",new MasterRequest(Map.of("MINOR_UNIT_DIGITS",10),0L),1));
        error("ERR_INVALID_INPUT", () -> masters.update("menu-amount-formats","1030~DEFAULT",new MasterRequest(Map.of("DISPLAY_DIVISOR","0"),0L),1));
        error("ERR_INVALID_INPUT", () -> masters.update("menu-amount-formats","1030~DEFAULT",new MasterRequest(Map.of("DISPLAY_DIVISOR","1.1"),0L),1));
        error("ERR_INVALID_INPUT", () -> masters.update("menu-amount-formats","1030~DEFAULT",new MasterRequest(Map.of("ROUNDING_MODE","UNNECESSARY"),0L),1));
    }
    @Test void databaseRejectsMissingHalfOfUnitConversionPair() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO TB_UNIT (UNIT_CODE,UNIT_NAME_MSG_CODE,UNIT_TYPE_CODE,BASE_UNIT_CODE,CREATED_BY) VALUES ('BAD_PAIR','COD_UNIT_G','MASS','G',0)"))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO TB_UNIT (UNIT_CODE,UNIT_NAME_MSG_CODE,UNIT_TYPE_CODE,TO_BASE_FACTOR,CREATED_BY) VALUES ('BAD_PAIR2','COD_UNIT_G','MASS',1,0)"))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
