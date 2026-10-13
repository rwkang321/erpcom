# Enterprise Application Development Standard

## 1. 목적

본 문서는 Java/Spring Boot + RDBMS + Web/Nexacro 기반 업무 시스템 개발 시 모든 프로젝트에서 공통으로 적용해야 하는 기본 설계 및 구현 규칙을 정의한다.

Codex 또는 개발자는 신규 기능, 테이블, Java 클래스, API, 화면을 생성하거나 기존 기능을 수정할 때 본 규칙을 우선 적용한다.

특별한 요구사항이 명시되지 않는 한 본 규칙을 임의로 생략하거나 다른 방식으로 변경하지 않는다.

---

# 2. 기본 설계 원칙

모든 업무 시스템은 최소한 다음 공통 기능을 고려한다.

1. 사용자 관리
2. 로그인 및 인증
3. ROLE 관리
4. 사용자-ROLE 관리
5. 메뉴 관리
6. ROLE별 메뉴 접근 권한
7. 버튼/기능 관리
8. ROLE별 버튼/기능 실행 권한
9. 다국어 관리
10. 공통 메시지 관리
11. 공통코드 관리
12. 예외 및 오류 처리
13. 감사 컬럼
14. 생성자/수정자 추적
15. 활성/비활성 관리
16. 로그 및 보안 감사
17. 캐시 정책
18. Validation
19. 사용자별 언어 설정
20. Backend 권한 재검증

Frontend의 메뉴/버튼 숨김 처리는 사용자 편의를 위한 UX일 뿐 보안 수단으로 간주하지 않는다.

실제 권한 검사는 반드시 Backend에서 다시 수행한다.

---

# 3. 모든 테이블 공통 컬럼 규칙

특별한 사유가 없는 모든 업무 및 Master 테이블에는 다음 5개 컬럼을 반드시 포함한다.

```sql
USE_YN      CHAR(1)       DEFAULT 'Y' NOT NULL,
CREATED_AT  TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
CREATED_BY  VARCHAR2(100) NOT NULL,
UPDATED_AT  TIMESTAMP,
UPDATED_BY  VARCHAR2(100)
```

의미:

- `USE_YN`
    - 논리적 사용 여부
    - 기본값 `Y`
    - 가능한 한 물리 DELETE보다 논리적 비활성화를 우선 고려한다.

- `CREATED_AT`
    - 최초 생성 일시
    - 최초 INSERT 이후 변경하지 않는다.

- `CREATED_BY`
    - 최초 생성 사용자

- `UPDATED_AT`
    - 마지막 수정 일시

- `UPDATED_BY`
    - 마지막 수정 사용자

업무 테이블에서는 특별한 이유가 없는 한 다음과 같은 별도 컬럼명을 사용하지 않는다.

```text
REG_DATE
REG_USER
MOD_DATE
MOD_USER
INSERT_DATE
UPDATE_DATE
CREATE_USER
```

전 시스템에서 다음 이름으로 통일한다.

```text
USE_YN
CREATED_AT
CREATED_BY
UPDATED_AT
UPDATED_BY
```

## 3.1 예외

다음과 같은 특수 목적 테이블은 공통 컬럼 일부를 생략할 수 있다.

- 순수 Mapping Table
- Immutable History Table
- Audit Log
- Interface 수신 원본
- Temporary/Staging Table
- 시스템 내부 Lock/Table Sequence 성격의 테이블

단, 생략할 경우 DDL 또는 설계 문서에 이유를 명시한다.

단순 Mapping Table이라도 관리 이력 또는 활성/비활성 관리가 필요하면 공통 컬럼을 포함한다.

---

# 4. 다국어 기본 원칙

언어별 컬럼을 생성하지 않는다.

금지:

```text
MENU_NAME_KO
MENU_NAME_EN
MENU_NAME_JA
MENU_NAME_ZH
```

새로운 언어가 추가되어도 DDL 변경이 발생하지 않아야 한다.

다국어는 반드시 다음 구조를 기본으로 한다.

```text
TB_LANGUAGE
TB_MESSAGE
TB_MESSAGE_LANG
```

---

# 5. TB_LANGUAGE

지원 언어를 관리한다.

주요 컬럼:

```text
LANG_CODE
LANG_NAME
NATIVE_NAME
FALLBACK_LANG_CODE
DEFAULT_YN
USE_YN
CREATED_AT
CREATED_BY
UPDATED_AT
UPDATED_BY
```

언어 코드는 가능한 한 표준 Language Tag를 사용한다.

예:

```text
ko
en
en-US
en-GB
ja
zh-CN
zh-TW
fr
de
```

`japan`, `chinese`, `korean` 등의 임의 코드는 사용하지 않는다.

---

# 6. TB_MESSAGE

시스템에서 사용되는 모든 사용자 표시 문자열의 논리적 정의를 관리한다.

번역 문자열 자체는 TB_MESSAGE에 저장하지 않는다.

주요 컬럼:

```text
MESSAGE_CODE
MESSAGE_TYPE
HTTP_STATUS
SEVERITY
ARG_COUNT
USE_YN
CREATED_AT
CREATED_BY
UPDATED_AT
UPDATED_BY
```

대표 MESSAGE_TYPE:

```text
ERROR
INFO
WARNING
CONFIRM
VALIDATION

MENU
SCREEN
BUTTON
LABEL
FIELD
GRID_HEADER
TAB
TOOLTIP
PLACEHOLDER

CODE_NAME
STATUS

MAIL_SUBJECT
MAIL_BODY
PUSH_TITLE
PUSH_BODY

REPORT
HELP
```

---

# 7. TB_MESSAGE_LANG

실제 언어별 문자열을 저장한다.

기본 키:

```text
MESSAGE_CODE + LANG_CODE
```

주요 컬럼:

```text
MESSAGE_CODE
LANG_CODE
MESSAGE_TEXT
TRANSLATED_YN
USE_YN
CREATED_AT
CREATED_BY
UPDATED_AT
UPDATED_BY
```

예:

```text
BTN_SAVE + ko     → 저장
BTN_SAVE + en     → Save
BTN_SAVE + ja     → 保存

ERR_ACCESS_DENIED + ko
→ 해당 기능에 대한 접근 권한이 없습니다.

ERR_ACCESS_DENIED + en
→ You do not have permission to access this function.
```

---

# 8. 다국어 적용 범위

최소 다음 항목은 다국어 적용 여부를 검토한다.

```text
메뉴명
화면명
버튼명
Label
필드명
Grid Header
Tab 명
Tooltip
Placeholder
팝업 제목
Dialog 제목

공통코드명
상태명
업무 단계명
결재 상태명

오류 메시지
안내 메시지
경고 메시지
확인 메시지
Validation 메시지
Empty Data 메시지

알림 제목/본문
메일 제목/본문
Push 메시지

보고서 제목
보고서 컬럼
출력물 문구
Excel Header
Help 문구
```

개발자용 Log Message와 사용자용 Message를 구분한다.

Server Log를 무조건 다국어화하지 않는다.

---

# 9. Message Code Naming Convention

Message Code는 의미 없는 숫자만 사용하지 않는다.

권장 Prefix:

```text
ERR_    Error
MSG_    Information
WRN_    Warning
CFM_    Confirm
VAL_    Validation

MNU_    Menu
SCR_    Screen
BTN_    Button
LBL_    Label
FLD_    Field
GRD_    Grid Header
TAB_    Tab
TIP_    Tooltip
COD_    Common Code
```

예:

```text
ERR_AUTH_INVALID_LOGIN
ERR_ACCESS_DENIED
ERR_PROJECT_NOT_FOUND

MSG_SAVE_SUCCESS

CFM_DELETE

MNU_PROJECT_MANAGEMENT
SCR_PROJECT_LIST
BTN_SAVE
BTN_DELETE
FLD_PROJECT_NAME
```

---

# 10. 사용자 관리

기본 사용자 테이블은 `TB_USER`를 사용한다.

최소 고려 컬럼:

```text
USER_ID
LOGIN_ID
USER_NAME
PASSWORD_HASH
LANG_CODE
USER_STATUS
MUST_CHANGE_PASSWORD_YN
USE_YN
CREATED_AT
CREATED_BY
UPDATED_AT
UPDATED_BY
```

`LANG_CODE`는 로그인 사용자의 기본 표시 언어를 의미한다.

---

# 11. ROLE 관리

ROLE은 다음 구조를 기본으로 한다.

```text
TB_ROLE
TB_USER_ROLE
```

사용자와 ROLE은 N:M 관계를 허용한다.

TB_ROLE 주요 컬럼:

```text
ROLE_ID
ROLE_CODE
ROLE_NAME_MSG_CODE
DESCRIPTION
USE_YN
CREATED_AT
CREATED_BY
UPDATED_AT
UPDATED_BY
```

ROLE_CODE 예:

```text
SYSTEM_ADMIN
PROJECT_MANAGER
COST_MANAGER
PROJECT_USER
VIEWER
```

ROLE 이름 자체도 다국어 대상이 될 수 있으므로 `ROLE_NAME_MSG_CODE`를 사용한다.

---

# 12. 메뉴 관리

메뉴 Master는 `TB_MENU`를 사용한다.

최소 컬럼:

```text
MENU_ID
PARENT_MENU_ID
MENU_CODE
MENU_NAME_MSG_CODE
SCREEN_ID
MENU_LEVEL
SORT_ORDER
USE_YN
CREATED_AT
CREATED_BY
UPDATED_AT
UPDATED_BY
```

메뉴명 문자열을 직접 저장하지 않는다.

금지:

```text
MENU_NAME
MENU_NAME_KO
MENU_NAME_EN
```

권장:

```text
MENU_NAME_MSG_CODE = MNU_PROJECT_MANAGEMENT
```

실제 메뉴명은 TB_MESSAGE_LANG에서 사용자 언어로 조회한다.

---

# 13. ROLE별 메뉴 권한

기본 테이블:

```text
TB_ROLE_MENU
```

기본 관계:

```text
ROLE_ID + MENU_ID
```

단순 시스템이면 `ACCESS_YN`을 사용할 수 있으나 업무 시스템에서는 다음 단계의 `ACCESS_LEVEL` 사용을 우선 검토한다.

```text
NONE
READ_PARTIAL
READ_ALL
WRITE_PARTIAL
WRITE_ALL
```

여러 ROLE이 존재할 경우 기본 정책은 Allow 권한의 합집합으로 계산한다.

Deny 우선 정책이 필요한 민감 기능은 별도 요구사항으로 정의한다.

---

# 14. 버튼/기능 관리

버튼 Master:

```text
TB_BUTTON
```

최소 컬럼:

```text
BUTTON_ID
BUTTON_CODE
BUTTON_NAME_MSG_CODE
BUTTON_TYPE
USE_YN
CREATED_AT
CREATED_BY
UPDATED_AT
UPDATED_BY
```

대표 BUTTON_CODE:

```text
BTN_SEARCH
BTN_NEW
BTN_SAVE
BTN_DELETE
BTN_EXCEL
BTN_APPROVE
BTN_REJECT
BTN_CLOSE
```

동일 버튼은 여러 화면에서 재사용할 수 있어야 한다.

---

# 15. 메뉴-버튼 관계

어떤 메뉴/화면에서 어떤 버튼을 사용하는지 다음 테이블로 관리한다.

```text
TB_MENU_BUTTON
```

기본 Key:

```text
MENU_ID + BUTTON_ID
```

---

# 16. ROLE별 버튼 권한

기본 테이블:

```text
TB_ROLE_BUTTON
```

Primary Key 또는 Unique 기준은 반드시 다음 조합을 고려한다.

```text
ROLE_ID
MENU_ID
BUTTON_ID
```

`ROLE_ID + BUTTON_ID`만 사용하지 않는다.

같은 `BTN_DELETE`라도 화면에 따라 실행 권한이 달라질 수 있기 때문이다.

예:

```text
PROJECT_MANAGER
PM_PROJECT_LIST
BTN_DELETE
→ 허용

PROJECT_MANAGER
COST_CLOSE
BTN_DELETE
→ 금지
```

---

# 17. 권한 계층

기본 권한 모델은 다음 순서를 따른다.

```text
USER
 ↓
ROLE
 ↓
MENU
 ↓
BUTTON / FUNCTION
```

의미:

```text
ROLE
→ 큰 업무 역할

MENU
→ 화면 접근 권한

BUTTON
→ 실제 기능 실행 권한
```

---

# 18. Frontend와 Backend 권한 원칙

Frontend에서 다음과 같이 버튼을 숨길 수 있다.

```javascript
btnDelete.set_visible(false);
```

하지만 이것은 보안으로 인정하지 않는다.

Frontend:

```text
권한에 따른 메뉴 표시
버튼 Visible/Enable
사용자 UX
```

Backend:

```text
로그인 검증
ROLE 검증
MENU 접근 검증
BUTTON/FUNCTION 실행 검증
```

실제 API는 Backend에서 반드시 권한을 다시 검사한다.

---

# 19. 사용자별 Override 확장

향후 다음 테이블을 추가할 수 있도록 확장 가능하게 설계한다.

```text
TB_USER_MENU
TB_USER_BUTTON
```

최종 권한 개념:

```text
ROLE 기본권한
+
사용자별 Override
=
최종 권한
```

사용자 Override가 필요하지 않은 프로젝트에서는 처음부터 구현하지 않아도 된다.

---

# 20. 로그인 처리 표준

로그인 성공 시 최소 다음 과정을 수행한다.

```text
LOGIN
 ↓
TB_USER 조회
 ↓
Password 검증
 ↓
사용자 상태 검증
 ↓
사용자 언어 결정
 ↓
USER ROLE 조회
 ↓
MENU 권한 계산
 ↓
BUTTON 권한 계산
 ↓
다국어 메뉴명/버튼명 구성
 ↓
권한 Cache 저장
 ↓
Frontend Bootstrap 데이터 반환
```

---

# 21. 로그인 Bootstrap 데이터

Frontend에는 최소 다음 정보를 제공한다.

```text
사용자 정보
언어
ROLE
메뉴
버튼/기능 권한
```

권장 DTO 구조:

```java
public record LoginResponse(
        Long userId,
        String loginId,
        String userName,
        String langCode,
        List<String> roles,
        List<MenuPermission> menus,
        List<ButtonPermission> buttons
) {
}
```

---

# 22. 권한 Cache

ROLE/MENU/BUTTON 권한을 매 요청마다 Database에서 다시 계산하지 않는다.

로그인 또는 권한 변경 시 계산하여 Cache 사용을 고려한다.

권장 구조 예:

```text
AUTH:USER:{USER_ID}
```

내용:

```text
roles
menus
buttons
dataScope
```

Redis 또는 Local Cache 사용 여부는 시스템 규모와 운영 구조에 따라 결정한다.

권한 변경 시 Cache 무효화 정책을 반드시 함께 설계한다.

---

# 23. ApiException 표준

업무 예외에 사용자 표시 메시지를 직접 넣지 않는다.

금지:

```java
throw new ApiException(
    "E403",
    HttpStatus.FORBIDDEN,
    "접근 권한이 없습니다."
);
```

권장:

```java
throw new ApiException(
    "ERR_ACCESS_DENIED"
);
```

ApiException은 원칙적으로 다음 정보만 가진다.

```text
MESSAGE_CODE
MESSAGE_ARGUMENTS
```

예:

```java
public class ApiException extends RuntimeException {

    private final String messageCode;
    private final Object[] args;

    public ApiException(
            String messageCode,
            Object... args
    ) {
        super(messageCode);
        this.messageCode = messageCode;
        this.args = args == null
                ? new Object[0]
                : args.clone();
    }

    public String messageCode() {
        return messageCode;
    }

    public Object[] args() {
        return args.clone();
    }
}
```

---

# 24. HTTP Status 관리

업무 Error의 HTTP Status를 Java와 DB 양쪽에 중복 정의하지 않는다.

원칙적으로:

```text
MESSAGE_CODE
 ↓
TB_MESSAGE
 ↓
HTTP_STATUS
```

구조를 사용한다.

예:

```text
ERR_AUTH_INVALID_LOGIN → 401
ERR_ACCESS_DENIED      → 403
ERR_PROJECT_NOT_FOUND  → 404
```

---

# 25. ApiError 표준

클라이언트 최종 오류 응답은 공통 DTO를 사용한다.

권장:

```java
public record ApiError(
        String code,
        int status,
        String path,
        String message,
        Instant timestamp
) {
}
```

응답 예:

```json
{
  "code": "ERR_ACCESS_DENIED",
  "status": 403,
  "path": "/api/projects/100",
  "message": "해당 기능에 대한 접근 권한이 없습니다.",
  "timestamp": "2026-10-05T01:00:00Z"
}
```

---

# 26. MessageService

메시지 해석은 공통 `MessageService`를 통해 수행한다.

```java
public interface MessageService {

    MessageInfo get(
            String messageCode,
            String langCode
    );

    String text(
            String messageCode,
            String langCode,
            Object... args
    );
}
```

Controller, Service, ExceptionHandler가 임의로 TB_MESSAGE_LANG을 직접 SELECT하지 않는다.

---

# 27. MessageInfo

Database에서 조회한 메시지 정보는 DTO 또는 record로 표현한다.

권장:

```java
public record MessageInfo(
        String messageCode,
        String messageType,
        Integer httpStatus,
        String severity,
        String langCode,
        String messageText,
        int argCount
) {
}
```

DB 조회 결과를 표현하는 데이터 객체에는 record 사용을 적극 고려한다.

---

# 28. 언어 결정 순서

사용자 표시 언어는 명확한 우선순위를 정의한다.

기본 권장:

```text
1. 인증된 사용자의 TB_USER.LANG_CODE
2. Request에서 명시적으로 허용된 Language
3. Accept-Language
4. TB_LANGUAGE의 Default Language
```

지원하지 않는 언어가 요청되면 Fallback Language를 사용한다.

---

# 29. Message Fallback

요청 언어 메시지가 존재하지 않는 경우 오류를 발생시키지 않는다.

예:

```text
ja
 ↓
일본어 없음
 ↓
FALLBACK_LANG_CODE
 ↓
없음
 ↓
시스템 DEFAULT LANGUAGE
```

최종적으로 Message 자체를 찾지 못할 경우 시스템 안전 메시지를 반환한다.

---

# 30. 오류 처리 중 장애에 대한 최종 Fallback

MessageService, Database 또는 Redis 자체가 장애일 수 있으므로 오류 처리 시스템이 다시 오류를 발생시키지 않도록 최소한의 Java 내부 Fallback을 유지한다.

최소 고려:

```text
ERR_INTERNAL_SERVER
ERR_MESSAGE_NOT_FOUND
ERR_SERVICE_UNAVAILABLE
```

사용자 메시지 Database가 장애라고 해서 무한 재귀적으로 메시지 Database를 다시 조회하지 않는다.

---

# 31. Filter 오류와 Controller 오류 통합

Controller 이전 Security Filter에서 발생한 오류와 Controller 이후 발생한 업무 오류는 동일한 MessageService와 ApiError 형식을 사용한다.

```text
Security Filter
      │
ApiErrorWriter
      │
      ├──────── MessageService
      │
Controller
      │
Service
      │
ApiExceptionHandler
      │
      └──────── MessageService
```

두 경로의 JSON 오류 포맷을 다르게 만들지 않는다.

---

# 32. Java Package 기본 구조

특별한 이유가 없으면 다음 구조를 기본으로 고려한다.

```text
common
 ├─ error
 │   ├─ ApiError.java
 │   ├─ ApiException.java
 │   ├─ ApiExceptionHandler.java
 │   └─ ApiErrorWriter.java
 │
 ├─ i18n
 │   ├─ MessageInfo.java
 │   ├─ MessageMapper.java
 │   ├─ MessageService.java
 │   ├─ MessageServiceImpl.java
 │   ├─ LanguageService.java
 │   ├─ RequestLanguageResolver.java
 │   └─ MessageCache.java
 │
 ├─ security
 │   ├─ SecurityUser.java
 │   ├─ SecurityConfig.java
 │   └─ ...
 │
 └─ authorization
     ├─ AuthorizationMapper.java
     ├─ AuthorizationService.java
     ├─ AuthorizationServiceImpl.java
     ├─ MenuPermission.java
     ├─ ButtonPermission.java
     └─ UserAuthorization.java
```

---

# 33. Service 구현 규칙

업무 Service는 특별한 이유가 없으면 Interface와 Implementation을 분리한다.

예:

```text
AuthorizationService
AuthorizationServiceImpl

MessageService
MessageServiceImpl

ProjectService
ProjectServiceImpl
```

Controller가 Mapper를 직접 호출하지 않는다.

기본 호출 구조:

```text
Controller
 ↓
Service Interface
 ↓
Service Implementation
 ↓
Repository / Mapper
 ↓
Database
```

---

# 34. Mapper 규칙

MyBatis 사용 시 Mapper Interface와 SQL Mapper를 명확하게 연결한다.

예:

```text
AuthorizationMapper.java
AuthorizationMapper.xml
```

SQL은 업무 의미가 드러나는 이름을 사용한다.

권장:

```text
selectUserRoles
selectMenuPermissions
selectButtonPermissions
selectMessage
```

지나치게 일반적인 이름은 피한다.

---

# 35. Validation

Frontend Validation만 신뢰하지 않는다.

Validation은 Backend에서도 수행한다.

사용자에게 표시되는 Validation 메시지는 가능한 한 Message Code로 관리한다.

예:

```text
VAL_PROJECT_NAME_REQUIRED
VAL_START_DATE_REQUIRED
```

---

# 36. 공통코드

상태값이나 분류값이 운영 중 추가/변경될 수 있다면 Java enum만 사용하지 않고 공통코드 테이블을 우선 검토한다.

공통코드명 역시 다국어 메시지와 연결한다.

예:

```text
TB_COMMON_CODE
CODE_GROUP
CODE
CODE_NAME_MSG_CODE
```

Java enum은 컴파일 시점에 고정되어야 하는 기술적 상수에 적합하다.

운영 데이터는 Database Master를 우선 고려한다.

---

# 37. record와 enum 사용 기준

`enum`:

```text
컴파일 시점에 제한된 상수
개발자가 정의하고 배포를 통해 변경
```

예:

```text
내부 기술 상태
Framework 전략
고정 알고리즘 타입
```

`record`:

```text
DB SELECT 결과
API Request/Response DTO
값 전달 객체
```

운영 중 계속 증가하는 업무 Master를 Java enum에 무조건 넣지 않는다.

---

# 38. 물리 DELETE

Master 및 업무 데이터는 무조건 DELETE하지 않는다.

다음 사항을 먼저 검토한다.

```text
USE_YN = 'N'
상태 변경
History 저장
```

실제 DELETE가 필요한 데이터인지 업무적으로 판단한다.

---

# 39. 날짜/사용자 감사 추적

INSERT 시:

```text
CREATED_AT
CREATED_BY
```

를 설정한다.

UPDATE 시:

```text
UPDATED_AT
UPDATED_BY
```

를 설정한다.

Client가 전달한 사용자 ID를 그대로 신뢰하지 않는다.

가능한 한 인증된 Server Security Context의 사용자 정보를 사용한다.

---

# 40. Codex 생성 규칙

Codex가 새로운 기능을 생성할 경우 다음 순서로 검토한다.

1. 기존 공통 테이블로 해결 가능한가?
2. 기존 Message Code를 재사용할 수 있는가?
3. 새 테이블에 공통 5개 컬럼이 포함되었는가?
4. 문자열이 다국어 대상인가?
5. ROLE 권한이 필요한가?
6. MENU 권한이 필요한가?
7. BUTTON/FUNCTION 권한이 필요한가?
8. Backend 권한 검사가 존재하는가?
9. Validation이 Backend에도 존재하는가?
10. 사용자 표시 Error가 하드코딩되어 있지 않은가?
11. MessageService를 사용하고 있는가?
12. Cache 무효화가 필요한가?
13. Audit/History가 필요한가?
14. 동시성 및 낙관적 Lock이 필요한가?
15. Transaction 경계가 적절한가?

---

# 41. 금지 사항

다음 구현은 특별한 요구가 없는 한 금지한다.

```text
언어별 컬럼 추가
MESSAGE_KO / MESSAGE_EN 방식

Controller에서 Mapper 직접 호출

Java 코드에 사용자 표시 한국어/영어 Error Message 하드코딩

Frontend 버튼 숨김만으로 권한 처리

ROLE만 확인하고 실제 기능 권한을 무시

모든 API 요청마다 ROLE/MENU/BUTTON 전체 DB 재조회

업무 Master 전체를 enum으로 고정

CREATED_BY / UPDATED_BY를 Client 요청 값 그대로 신뢰

동일 정책을 Java와 DB 양쪽에 중복 관리
```

---

# 42. 신규 기능 생성 시 기본 산출물

Codex는 업무 기능을 생성할 때 필요에 따라 최소 다음 산출물을 검토한다.

```text
DDL / Migration SQL

Domain/Row/DTO/record

Mapper.java
Mapper.xml

Service.java
ServiceImpl.java

Controller.java

Validation

Message Code Seed SQL

ROLE/MENU/BUTTON Seed SQL

Exception 처리

Test
```

UI가 있는 경우:

```text
Menu 등록
Button 등록
Role Menu Permission
Role Button Permission
다국어 Message
Frontend Permission 처리
```

까지 함께 검토한다.

---

# 43. 최우선 원칙

새로운 기능을 개발할 때 개별 화면과 업무 기능만 구현하지 않는다.

항상 다음 공통 관점을 동시에 검토한다.

```text
사용자
인증
ROLE
메뉴
기능권한
다국어
메시지
Validation
감사추적
예외처리
Cache
보안
```

특별한 이유로 본 표준에서 벗어나는 경우 개발자는 그 이유를 명시한다.

Codex는 기존 표준과 충돌하는 새로운 구조를 임의로 만들지 말고 기존 공통 구조를 재사용하거나 확장한다.