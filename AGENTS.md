# AGENTS.md — 기업 업무시스템 Level 1 공통 개발 표준

## 1. 목적

이 프로젝트의 Level 1은 특정 업무에 종속되지 않는 기업 업무시스템 공통 기반이다.
Codex는 신규 기능을 생성하거나 기존 기능을 수정할 때 이 문서를 최우선 개발 규칙으로 적용한다.

Level 1에는 다음 영역이 포함된다.

- 회사 / 사업장 / 부서
- 사용자
- 역할(ROLE) 및 사용자-역할
- 메뉴 및 메뉴 접근권한
- 버튼/기능 및 버튼 실행권한
- 사용자별 메뉴/버튼 예외권한
- 언어 / 다국어 메시지
- 공통코드
- 재고 / 자산 단위(UNIT) 기준정보
- 통화(CURRENCY) 기준정보
- 메뉴 / 금액 항목별 표시 단위 및 소수 자릿수 설정
- 공통 예외처리
- 권한 조회 서비스

Level 2는 실제 인증 구현체를 의미하며 별도로 구성한다.
예: Spring Security 설정, LOCAL 로그인, JWT, Session, Redis Session, SSO, LDAP, OAuth2/OIDC 등.

Codex는 Level 1의 공통 구조를 무시하고 유사한 테이블이나 클래스를 새로 만들지 않는다.
기존 구조를 재사용하거나 필요한 경우 명확한 이유를 설명하고 확장한다.

---

## 2. 모든 일반 테이블의 공통 5개 컬럼

특별한 목적의 테이블이 아닌 모든 Master 및 업무 테이블에는 다음 컬럼을 반드시 포함한다.

```sql
USE_YN      CHAR(1)       DEFAULT 'Y' NOT NULL,
CREATED_AT  TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
CREATED_BY  NUMBER(19)    NOT NULL,
UPDATED_AT  TIMESTAMP,
UPDATED_BY  NUMBER(19)
```

`CREATED_BY`, `UPDATED_BY`에는 로그인 ID 문자열보다 변경되지 않는 `TB_USER.USER_ID`를 저장하는 것을 기본으로 한다.
초기 Seed 데이터의 시스템 생성자는 `0`을 예약값으로 사용한다.

예외 가능 테이블:
- 변경되지 않는 History/Audit Log
- Temporary/Staging
- Interface Raw Data
- 기술적 Lock/Sequence 테이블

공통 컬럼을 생략하면 그 이유를 DDL 주석 또는 설계 문서에 반드시 기록한다.

동시 수정 가능성이 있는 업무 테이블은 다음 컬럼 추가를 검토한다.

```sql
VERSION_NO NUMBER(19) DEFAULT 0 NOT NULL
```

---

## 3. Level 1 기준 테이블

### 조직/사용자
- `TB_COMPANY`
- `TB_SITE`
- `TB_DEPT`
- `TB_USER`

### 역할
- `TB_ROLE`
- `TB_USER_ROLE`

### 메뉴/기능 권한
- `TB_MENU`
- `TB_BUTTON`
- `TB_MENU_BUTTON`
- `TB_ROLE_MENU_AUTH`
- `TB_USER_MENU_AUTH`
- `TB_ROLE_BUTTON_AUTH`
- `TB_USER_BUTTON_AUTH`

### 다국어
- `TB_LANGUAGE`
- `TB_MESSAGE`
- `TB_MESSAGE_LANG`

### 공통코드
- `TB_COMMON_CODE_GROUP`
- `TB_COMMON_CODE`

### 단위 / 통화 / 금액 표시 설정
- `TB_UNIT`
- `TB_CURRENCY`
- `TB_MENU_AMOUNT_FORMAT`

위 세 테이블은 Level 1 공통 기준정보에 포함한다. 2026-10-14 원본 소스 확인 기준으로
통합 V001에는 총 21개 테이블 정의, V002에는 테이블별 Seed가 있으며,
단위 / 통화 / 금액 표시 Java 구현은 `common/basis` 한 폴더에 반영되어 있다.
소스 반영, 실제 DB Migration 실행, 운영 화면 / Level 2 인증 연결 완료를 각각 구분한다.

---

## 4. 다국어 규칙

언어별 컬럼을 절대 생성하지 않는다.

금지 예:
```text
MENU_NAME_KO
MENU_NAME_EN
MESSAGE_JA
MESSAGE_ZH
```

반드시 다음 구조를 사용한다.

```text
TB_LANGUAGE
TB_MESSAGE
TB_MESSAGE_LANG
```

언어 코드는 `ko`, `en`, `ja`, `zh-CN`, `zh-TW` 등 BCP-47 계열 표기를 사용한다.

사용자 표시 문자열은 다음을 포함하여 다국어 적용 여부를 검토한다.
- 메뉴명, 화면명, 버튼명
- Label, 필드명, Grid Header, Tab
- Tooltip, Placeholder, 팝업/Dialog 제목
- 공통코드명, 상태명, 업무단계명
- 단위명, 통화명, 금액 표시 단위 문구
- 오류/안내/경고/확인/Validation 메시지
- Empty Data 메시지
- 보고서/Excel Header
- Help 문구
- 알림/메일/Push 제목 및 본문

개발자용 Server Log는 사용자용 Message와 분리한다.

---

## 5. 메시지 코드 규칙

권장 Prefix:
- `ERR_` 오류
- `MSG_` 안내
- `WRN_` 경고
- `CFM_` 확인
- `VAL_` Validation
- `MNU_` 메뉴
- `SCR_` 화면
- `BTN_` 버튼
- `LBL_` Label
- `FLD_` 필드
- `GRD_` Grid Header
- `TAB_` Tab
- `TIP_` Tooltip
- `COD_` 공통코드명
- `ROLE_` 역할 표시명

사용자 표시 메시지를 Java에 직접 하드코딩하지 않는다.

금지:
```java
throw new ApiException("E403", HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
```

권장:
```java
throw new ApiException("ERR_ACCESS_DENIED");
```

---

## 6. 조직 구조

기본 조직 계층은 다음과 같다.

```text
TB_COMPANY
  └─ TB_SITE
       └─ TB_DEPT
            └─ TB_USER
```

사용자는 회사에 반드시 속하고, 사업장/부서는 시스템 특성에 따라 선택적으로 연결할 수 있다.
회사-사업장, 회사-부서의 정합성을 FK 또는 Service 검증으로 유지한다.

---

## 7. ROLE 및 사용자-ROLE

한 사용자는 여러 ROLE을 가질 수 있다.

```text
TB_USER
  ↓
TB_USER_ROLE
  ↓
TB_ROLE
```

`TB_USER_ROLE`은 `VALID_FROM`, `VALID_TO`를 이용해 기간성 ROLE 부여를 지원한다.
현재 활성 ROLE 조회 시 `USE_YN`, 유효기간을 모두 확인한다.

---

## 8. 메뉴 권한

ROLE 기본 메뉴권한은 `TB_ROLE_MENU_AUTH`에 저장한다.
사용자별 예외권한은 `TB_USER_MENU_AUTH`에 저장한다.

기본 메뉴 권한 수준:
```text
NONE
READ_PARTIAL
READ_ALL
WRITE_PARTIAL
WRITE_ALL
```

데이터 범위가 필요한 시스템은 다음 기본 값을 사용한다.
```text
OWN
DEPT
SITE
COMPANY
```

사용자 예외값이 NULL이면 ROLE 권한을 상속한다.
사용자 예외값이 명시되어 있으면 프로젝트에서 정한 Override 규칙에 따라 최종 권한을 계산한다.

ROLE이 여러 개인 경우 기본 정책은 허용 권한 합집합이다.
민감기능에서 DENY 우선 정책이 필요한 경우 별도 정책으로 명시한다.

---

## 9. 버튼/기능 권한

버튼 Master는 `TB_BUTTON`, 메뉴별 버튼 배치는 `TB_MENU_BUTTON`을 사용한다.

ROLE 버튼권한:
```text
TB_ROLE_BUTTON_AUTH
(ROLE_ID, MENU_ID, BUTTON_ID)
```

사용자별 버튼 예외권한:
```text
TB_USER_BUTTON_AUTH
(USER_ID, MENU_ID, BUTTON_ID)
```

`ROLE_ID + BUTTON_ID`만으로 권한을 정의하지 않는다.
동일 버튼도 화면마다 허용 여부가 다를 수 있기 때문이다.

Frontend의 버튼 숨김/비활성화는 UX일 뿐 보안이 아니다.
실제 API 실행 권한은 Backend에서 반드시 다시 검사한다.

---

## 10. 공통코드

운영 중 증가/변경 가능한 상태, 분류, 업무코드는 Java enum만으로 고정하지 않는다.

기본 구조:
```text
TB_COMMON_CODE_GROUP
TB_COMMON_CODE
```

공통코드 표시명은 문자열을 직접 저장하지 않고 `CODE_NAME_MSG_CODE`로 `TB_MESSAGE`와 연결한다.

Java enum은 컴파일 시점에 고정되어야 하는 기술적 상수에 사용한다.
DB Master는 운영 중 관리되는 업무 기준정보에 사용한다.

단위 종류 등 단순 분류는 기존 공통코드를 재사용한다.
재고 단위의 환산 / 수량 정밀도와 통화의 기본 소수 자릿수는 전용 Master에서 관리하며,
`TB_COMMON_CODE.EXT_VALUE1~3` 문자열에 계산 규칙을 숨겨 넣지 않는다.
단위 / 통화 목록을 전용 Master와 공통코드 양쪽에 중복 관리하지 않는다.

---

## 11. ApiException / ApiError

`ApiException`은 원칙적으로 다음만 가진다.
- `messageCode`
- `args`

HTTP Status와 번역문은 `TB_MESSAGE`, `TB_MESSAGE_LANG`을 통해 결정한다.
Java와 DB에 동일 정책을 중복 정의하지 않는다.

Filter 단계 오류와 Controller/Service 오류 모두 동일한 `MessageService`와 `ApiError` 포맷을 사용한다.

메시지 DB/Cache 자체 장애에 대비해 최소한의 Java 내부 안전 Fallback은 유지한다.

---

## 12. Java Layer 규칙

기본 흐름:
```text
Controller
  ↓
Service Interface
  ↓
ServiceImpl
  ↓
Mapper / Repository
  ↓
Database
```

Controller에서 업무 Mapper를 직접 호출하지 않는다.

MyBatis는 다음 짝을 사용한다.
```text
XxxMapper.java
XxxMapper.xml
```

DB 조회 결과/DTO/불변 전달객체에는 `record` 사용을 적극 검토한다.
운영 중 계속 증가하는 Master 데이터에는 enum을 남용하지 않는다.

DB 테이블과 Java `record`는 대체 관계가 아니다. DB는 기준정보를 영속 저장하고,
`record`는 조회 / 요청 / 응답 자료를 전달한다. 단위 / 통화 목록을 Java의 고정 record 목록으로 관리하지 않는다.
금액 / 소수 수량 / 환산계수 계산은 `BigDecimal`을 사용하고 DB의 정밀도 / 소수 자릿수와 맞춘다.

`common`의 Java 파일은 기능별 폴더 하나에 함께 둔다.
기존 `common/master`, `common/message`, `common/permission`, `common/reference`, `common/web`,
`common/error`와 같은 구조를 유지한다. 새 `common/basis`도 같은 규칙을 적용한다.
`basis/controller`, `basis/service`, `basis/model`, `basis/mapper` 같은 역할별 하위 폴더를 만들지 않는다.

```text
src/main/java/com/erpcom/erpcom/common/basis/
  BasisController.java
  BasisService.java
  BasisServiceImpl.java
  BasisMapper.java
  BasisModels.java
```

위 다섯 파일의 package는 모두 `com.erpcom.erpcom.common.basis`이다.
Controller / Service / ServiceImpl / Mapper / model의 역할과
호출 순서는 그대로 유지하며, 역할 분리를 폴더 분리와 혼동하지 않는다.
`BasisMapper.xml`은 `src/main/resources/mapper/common/BasisMapper.xml`에 두고
namespace는 `com.erpcom.erpcom.common.basis.BasisMapper`, record resultType은
`com.erpcom.erpcom.common.basis.BasisModels$...`를 사용한다.
패키지가 바뀌면 관련 import / 테스트 / XML 경로를 함께 맞추고 옛 Java 파일을 중복으로 남기지 않는다.
기존 오류 / 다국어 / 권한 구현은 재사용한다. 이 규칙 때문에 다른 기능의 파일을 일괄 이동하지 않는다.

---

## 13. Seed 데이터 규칙

공통 기준 데이터는 Application Startup Mapper 코드로 삽입하지 않는다.
Flyway Version Migration으로 관리한다.

기본:
```text
V001__create_common_tables.sql
V002__seed_common_reference_data.sql
```

V002에는 다음과 같은 공통 기준값을 넣을 수 있다.
- 지원 언어
- 기본 메시지
- 표준 버튼
- 기본 ROLE 예제
- 기본 메뉴/권한 예제
- 표준 공통코드 예제

시스템별/고객별로 달라지는 실제 운영 초기값은 별도 후속 Migration으로 분리한다.

Mapper/Service는 운영 중 관리자 화면에서 등록/수정/비활성화하는 용도다.

이미 DB에 적용된 Migration은 수정하지 않고 후속 Version Migration으로 확장한다.
DB 적용 전 최초 설치 구성을 재정리하는 경우 승인된 통합 V001 / V002를 사용할 수 있다.
현재 최초 설치 구성은 V003의 구조를 V001에, 데이터를 V002에 포함한 두 파일이다.
통합 구성을 적용할 때 같은 내용의 V003을 다시 실행하지 않는다.
V002는 테이블별로 초기 / 추가 자료를 한 구역에 모아 작성하되, 외래키 참조 테이블을 먼저 준비한다.
자기 참조는 부모 / 기준 행을 먼저 준비하고, INSERT ... SELECT는 관련 테이블 구역에 둔다.
표준 단위 / 지원 통화 / 관련 메시지와 공통 분류를 Migration으로 준비한다.
고객별 실제 메뉴의 금액 표시 설정은 고객별 후속 Migration 또는 권한 있는 관리자 등록으로 분리한다.

---

## 14. Level 2와의 경계

다음은 Level 1에 포함하지 않고 Level 2 Security Starter에서 구현한다.
- `SecurityFilterChain`
- `SecurityUser`
- `UserDetailsService`
- BCrypt/PasswordEncoder 구성
- 실제 Login Controller/AuthenticationService
- JWT Access/Refresh Token
- DB/Redis Session 구현
- AuthenticationEntryPoint
- Spring Security AccessDeniedHandler
- SSO/LDAP/OAuth2/OIDC 연계
- 로그인 이력/세션 저장 구현

단, Level 2는 Level 1의 `TB_USER`, ROLE, 메뉴/버튼 권한, MessageService를 재사용해야 한다.

단위 / 통화 / 금액 표시 기준정보도 Level 1 소관이다. Level 2 인증 Starter 안에
별도 단위 / 통화 Master나 동일 목적의 표시 설정 테이블을 만들지 않는다.
Level 2는 검증된 `USER_ID`를 제공하며, Level 1은 그 번호로 기준정보 조회 / 변경 권한을 확인한다.
품목별 포장 환산, 재고 수불, 환율 적용 원가 / 회계 계산은 해당 업무의 후속 구현 범위이다.

---

## 15. Codex 작업 전 필수 점검

신규 기능을 만들기 전에 반드시 확인한다.
1. 기존 공통 테이블로 해결 가능한가?
2. 새 테이블에 공통 5개 컬럼이 있는가?
3. 사용자 표시 문자열이 하드코딩되어 있지 않은가?
4. 다국어 Message Code가 필요한가?
5. ROLE 권한이 필요한가?
6. MENU 접근권한이 필요한가?
7. BUTTON/FUNCTION 실행권한이 필요한가?
8. 사용자별 예외권한이 필요한가?
9. Backend 권한 검사가 존재하는가?
10. 공통코드로 관리할 값인가?
11. Validation을 Backend에서도 수행하는가?
12. Cache 변경/무효화 정책이 필요한가?
13. Transaction 경계가 적절한가?
14. 동시성 제어 또는 VERSION_NO가 필요한가?
15. 수량의 UNIT과 금액의 CURRENCY / 표시 배율을 구분했는가?
16. 통화 기본 소수 자릿수 / 화면 표시 자릿수 / 내부 계산 정밀도를 구분했는가?
17. 원래 금액과 통화를 보존하고 화면 / 보고서 / Excel에 같은 표시 정책을 적용하는가?
18. 환산 가능한 단위 종류와 품목별 포장 환산의 경계를 확인했는가?
19. common 기능 폴더 안에 Java 파일을 함께 두고 package / import / Mapper XML을 일치시켰는가?
20. Seed가 테이블별로 모여 있고 테이블 간 / 자기 참조 입력 순서가 맞는가?

표준에서 벗어나야 하면 구현 전에 이유를 명시한다.

---

## 16. 재고 / 자산 단위(UNIT)

단순 선택 목록만 필요하면 공통코드 사용을 검토한다. 이 프로젝트의 재고 / 자산 공통 기준은
환산과 수량 정밀도가 필요하므로 `TB_UNIT`을 기준정보 원본으로 사용한다.

주요 컬럼 기준:

| 컬럼 | 역할 | 예 |
|---|---|---|
| `UNIT_CODE` | 단위 기본키 | `EA`, `PCS`, `KG`, `G`, `MB`, `GB` |
| `UNIT_NAME_MSG_CODE` | `TB_MESSAGE`와 연결할 표시명 코드 | `COD_UNIT_KG` |
| `UNIT_TYPE_GROUP` | 공통코드 복합 FK의 고정 분류, API 편집 제외 | `UNIT_TYPE` |
| `UNIT_TYPE_CODE` | 공통코드 `UNIT_TYPE`의 분류 | 수량 / 무게 / 데이터 용량 |
| `QUANTITY_SCALE` | 기본 허용 수량 소수 자릿수 | 업무에서 정한 KG의 소수 3자리 |
| `BASE_UNIT_CODE` | 같은 종류의 기준 단위, `TB_UNIT` 참조 | `G` |
| `TO_BASE_FACTOR` | 기준 단위로 바꿀 때 곱하는 양수 십진수 | KG → G는 `1000` |
| `SORT_ORDER` | 목록 표시 순서 | `10` |

공통 5개 컬럼과 `VERSION_NO`를 포함한다. 표시명은 기존 다국어 테이블을 재사용한다.
환산을 지원하는 단위는 기준 단위와 계수를 함께 정의하고 기준 단위 자신의 계수는 1로 둔다.
환산을 지원하지 않는 단위는 기준 단위 / 계수를 모두 비워 두는 정책을 사용할 수 있다.
계수는 양수이며 현재 `NUMBER(38,18)`을 사용한다. 수량 소수 자릿수는 현재 0~9의 정수이다.
기준 단위는 계수 1인 최종 기준 단위를 직접 참조하며 종류 / 기준 / 환산계수의 등록 후 변경은 제한한다.
기준 단위는 같은 종류여야 하며, 환산 관계의 모순을 검사한다.
계수가 1인 기준 단위의 자기 참조는 허용하되, 그 외 환산 관계의 순환은 허용하지 않는다.

예:
```text
G:  기준 단위 G, 계수 1
KG: 기준 단위 G, 계수 1000
1.25 KG × 1000 = 1250 G
```

`KG → EA`처럼 종류가 다른 단위는 공통 환산으로 처리하지 않는다.
`EA`와 `PCS`도 업무에서 같은 의미로 정의한 경우에만 동일하게 취급한다.
기본 수량 정밀도는 업무 기준으로 정하며, 필요하면 후속 품목 설정으로 구체화한다.

`1 BOX = 12 EA`인 볼펜과 `1 BOX = 24 EA`인 음료처럼 품목마다 다른 포장 환산은
품목별 단위 관계에서 관리한다. 이를 공통 UNIT의 고정 계수로 넣지 않는다.

데이터 용량은 `1 GB = 1000 MB`, `1 GiB = 1024 MiB`를 구분한다.
MB / GB에 1024 기준을 암묵적으로 적용하지 않는다.

이미 사용된 단위의 의미 / 환산계수 변경이 과거 수량을 재해석하지 않도록 변경 정책을 정한다.
비활성화 시 과거 참조는 보존하고 신규 사용 가능 여부를 별도 검사한다.

---

## 17. 통화(CURRENCY)

지원 통화는 ISO 4217을 기준으로 `TB_CURRENCY`에서 관리한다.
운영 중 추가 / 비활성화할 수 있으므로 enum이나 Java 고정 객체 목록을 기준정보 원본으로 사용하지 않는다.

| 컬럼 | 역할 | 예 |
|---|---|---|
| `CURRENCY_CODE` | 통화 기본키, 3자리 영문 코드 | `KRW`, `USD`, `EUR` |
| `CURRENCY_NAME_MSG_CODE` | `TB_MESSAGE`와 연결할 표시명 코드 | `COD_CURRENCY_KRW` |
| `MINOR_UNIT_DIGITS` | 통화의 기본 소수 자릿수 | KRW=0, USD=2, EUR=2 |
| `NUMERIC_CODE` | 선택적 ISO 숫자 코드, 3자리 문자열 | `410`, `840`, `978` |
| `SORT_ORDER` | 목록 표시 순서 | `10` |

공통 5개 컬럼과 `VERSION_NO`를 포함한다. 숫자 코드는 선행 0을 보존할 수 있어야 한다.
지원할 일반 통화의 기본 소수 자릿수는 유효한 0 이상 정수로 관리한다.
기본 소수 자릿수가 정의되지 않은 특수 코드를 지원하려면 별도 정책을 먼저 정의한다.
통화 기호를 추가하더라도 `$` 같은 기호를 통화의 고유 식별자로 사용하지 않는다.

조회 / 응답 객체는 다음과 같은 `record`로 전달할 수 있다. 이 선언은 DB를 만들거나 저장하지 않는다.

```java
public record CurrencyInfo(String currencyCode, String name, int minorUnitDigits, String numericCode) {}
```

현재 구현은 `common/basis/BasisModels.java` 안의 `CurrencyInfo` record이다.
통화 기본 소수 자릿수와 화면 표시 자릿수는 현재 각각 0~9의 정수로 검증한다.
환율은 통화 목록과 별개이다. USD → KRW 환산에는 적용 날짜 / 환율 등 업무 정책이 필요하다.
통화명 또는 화면 표시 설정만 바꾸어 서로 다른 통화의 금액을 변환 / 합산하지 않는다.

---

## 18. 메뉴 / 금액 항목별 표시 설정

금액의 통화, 표시 배율, 표시 소수 자릿수, 반올림 방식을 분리한다.
`KRW 억`을 하나의 불투명한 문자열로 저장하여 계산 규칙으로 사용하지 않는다.
실제 금액과 통화는 업무 DB에 보존하고, 표시할 때만 배율 / 반올림을 적용한다.

기존 `TB_MENU`를 참조하는 `TB_MENU_AMOUNT_FORMAT`을 사용한다.
동일한 메뉴 / 화면 기준정보 테이블을 새로 만들지 않는다.

| 컬럼 | 역할 | 예 |
|---|---|---|
| `MENU_ID` | `TB_MENU`의 메뉴 번호 | 총예정원가 메뉴 |
| `FIELD_CODE` | 금액 항목 코드 | `TOTAL_ESTIMATED_COST` |
| `CURRENCY_CODE` | 고정 통화 화면에 필요한 선택적 통화, `TB_CURRENCY` 참조 | `KRW` |
| `DISPLAY_DIVISOR` | 원래 금액을 나눌 양수 | `100000000` |
| `DECIMAL_PLACES` | 화면 표시 소수 자릿수 | `1` |
| `ROUNDING_MODE` | 허용된 기술적 반올림 정책 | `HALF_UP` |
| `UNIT_LABEL_MSG_CODE` | 표시 배율 문구의 메시지 코드 | 억 / 백만 등 |

`MENU_ID + FIELD_CODE`는 유일해야 한다. `FIELD_CODE=DEFAULT`를 메뉴 기본 설정의 예약값으로 두고,
실제 금액 항목 코드로 DEFAULT를 사용하지 않는다. 적용 순서는 다음과 같다.

```text
금액 항목별 설정 → 없으면 메뉴 DEFAULT 설정 → 없으면 시스템 기본 설정
```

현재 시스템 기본값은 실제 통화 유지 / 배율 1 / 해당 통화의 기본 소수 자릿수 / HALF_UP이다.
공통 5개 컬럼과 `VERSION_NO`를 포함한다. 메뉴 / 통화 / 메시지 참조를 검증한다.
소수 자릿수는 0 이상 정수이며 허용 상한을 정한다. 표시 배율은 양수,
반올림 방식은 허용 목록으로 검증한다. 이 정책과 실제 계산을 Backend에서도 검증한다.

예:
```text
실제 금액 = 59,230,000,000 KRW
표시 배율 = 100,000,000, 표시 소수 자릿수 = 1
59,230,000,000 ÷ 100,000,000 = 592.3
화면: 총예정원가 592.3 (단위: KRW 억)
뜻: 592억 3천만원
```

통화 기본 소수 자릿수 / 화면 표시 소수 자릿수 / 내부 계산 정밀도는 각각 별개이다.
KRW의 기본 소수 자릿수 0이 억원 화면의 소수 1자리 표시를 금지하지 않는다.
단가 / 계산 중간값의 DB 정밀도를 통화 기본 소수 자릿수로 일괄 제한하지 않는다.
DB `NUMBER(p,s)`는 최대 금액 / 필요 정밀도를 검토하여 결정하고 Java `BigDecimal`과 맞춘다.

반올림은 정해진 최종 표시 단계에서 적용한다. 화면 / 보고서 / Excel에 같은 정책을 적용한다.
반올림한 화면 값을 원래 금액에 그대로 덮어쓰지 않는다. 금액 입력 / 수정 단위도 명확히 표시한다.
실제 통화가 USD인 자료에 KRW 설정을 붙여 원화처럼 보이게 하지 않는다.
고정 통화 화면은 자료의 통화 일치를 확인하고, 다중 통화 합산은 별도 환율 정책을 따른다.
메뉴의 한국어 / 영어 표시명과 숫자 구분 기호는 언어 설정을 따르되, 언어 변경만으로 통화를 바꾸지 않는다.

---

## 19. 확장 구현 / 연동 원칙

단위 / 통화 / 금액 표시 설정의 등록 / 변경 / 비활성화는 기존 메뉴 / 버튼 권한 정책으로 보호한다.
중앙 공통 관리에서는 `COMMON_ADMIN`과 해당 기능 버튼 권한을 재사용하며,
화면에서 버튼을 숨기는 것만으로 Backend 검사를 대신하지 않는다.
감사 컬럼에는 Level 2가 검증하고 Level 1이 확인한 실제 `USER_ID`를 기록한다.

현재 `MasterCatalog` / `MasterServiceImpl`은 새 세 테이블의 공통 관리를 처리한다.
`BasisController` / `BasisService` / `BasisServiceImpl` / `BasisMapper` / `BasisModels`는
`common/basis` 한 폴더에서 단위 / 통화 / 금액 표시 조회와 계산을 제공한다.
수량 환산은 `convertQuantity()`, 금액 표시는 `displayAmount()`를 사용한다.
`ReferenceDataServiceImpl.bootstrap()`의 응답은 유지하고, 기준정보는 별도
`GET /api/common/units`, `/api/common/currencies`, `/api/common/amount-formats/{menuCode}`로 조회한다.
실제 인증 사용자와 해당 메뉴 권한을 검증하며 원가 자료 자체의 범위 검사는 업무 서비스에서 수행한다.

장기 캐시를 도입하면 기준정보 변경 커밋 후 관련 조회 / 표시 정책 캐시를 무효화한다.
로그인 토큰 / 세션에 단위 / 통화 / 표시 정책 전체를 고정하여 변경이 반영되지 않게 하지 않는다.
현재 문서 변경만으로 캐시나 새 기준정보 API가 구현된 것은 아니다.
