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

표준에서 벗어나야 하면 구현 전에 이유를 명시한다.
