# Level 1 공통 API

## 기준정보 리소스

| 리소스 | 테이블 | URL key |
| --- | --- | --- |
| companies | TB_COMPANY | COMPANY_ID |
| sites | TB_SITE | SITE_ID |
| departments | TB_DEPT | DEPT_ID |
| users | TB_USER | USER_ID |
| roles | TB_ROLE | ROLE_ID |
| user-roles | TB_USER_ROLE | USER_ROLE_ID |
| menus | TB_MENU | MENU_ID |
| buttons | TB_BUTTON | BUTTON_ID |
| menu-buttons | TB_MENU_BUTTON | MENU_ID~BUTTON_ID |
| role-menu-permissions | TB_ROLE_MENU_AUTH | ROLE_MENU_AUTH_ID |
| user-menu-permissions | TB_USER_MENU_AUTH | USER_MENU_AUTH_ID |
| role-button-permissions | TB_ROLE_BUTTON_AUTH | ROLE_ID~MENU_ID~BUTTON_ID |
| user-button-permissions | TB_USER_BUTTON_AUTH | USER_BUTTON_AUTH_ID |
| languages | TB_LANGUAGE | LANG_CODE |
| messages | TB_MESSAGE | MESSAGE_CODE |
| message-translations | TB_MESSAGE_LANG | MESSAGE_CODE~LANG_CODE |
| code-groups | TB_COMMON_CODE_GROUP | CODE_GROUP |
| codes | TB_COMMON_CODE | CODE_GROUP~CODE |

복합 키는 위 순서로 ~로 연결합니다. 문자열 키에 / 또는 ~를 넣을 수 없습니다. 허용 필드, 필수 여부, 자료형, 길이와 기본값은 GET /api/common/admin/resources에서 조회합니다. 입력 필드 이름은 DB 컬럼 이름으로 통일합니다.

공통 관리자 권한은 COMMON_ADMIN + COMPANY 범위이며 조회는 READ_ALL/BTN_SEARCH, 등록·수정·비활성화는 WRITE_ALL과 각각 BTN_NEW/BTN_SAVE/BTN_DELETE입니다. 모든 요청에서 서버가 권한을 다시 검사합니다.

## 등록

POST /api/common/admin/companies

~~~json
{
  "values": {
    "COMPANY_CODE": "C200",
    "COMPANY_NAME": "Example Company"
  }
}
~~~

생성한 행을 HTTP 201로 반환합니다. 숫자 PK는 서버가 생성합니다. 자연/복합 PK는 values에 지정합니다. 기본 USE_YN은 Y, VERSION_NO는 0입니다. CREATED_BY는 인증 사용자의 USER_ID에서 가져옵니다.

PASSWORD_HASH, AUTH_TYPE, FAILED_LOGIN_COUNT, LOCKED_UNTIL, PASSWORD_CHANGED_AT, MUST_CHANGE_PASSWORD_YN, LAST_LOGIN_AT은 Level 2 전용이며 이 API의 조회/수정 필드에서 제외합니다. 고객 인증 방식 설정은 Level 2에서 수행합니다.

## 수정과 재활성화

PUT /api/common/admin/companies/100

~~~json
{
  "values": {
    "COMPANY_NAME": "Changed Company"
  },
  "expectedVersion": 0
}
~~~

PUT은 지정된 필드만 수정합니다. 키는 변경할 수 없습니다. UPDATED_AT/UPDATED_BY는 서버에서 설정하고 VERSION_NO를 1 증가시킵니다. expectedVersion이 누락되면 ERR_INVALID_INPUT, 현재 값과 다르면 ERR_CONFLICT입니다. 재활성화는 USE_YN을 Y로 수정하며 동일한 FK·기간·정합성 검증을 적용합니다.

## 비활성화와 목록

DELETE /api/common/admin/companies/100?expectedVersion=1

물리 삭제 없이 USE_YN=N으로 변경하고 204를 반환합니다. 하위 데이터의 사용 여부는 자동 변경하지 않습니다. 조직/사용자/메뉴/버튼 비활성화는 권한 조회 시 반영됩니다. 기본 언어는 DEFAULT_YN을 변경한 후 비활성화할 수 있습니다.

GET /api/common/admin/users?offset=0&limit=50&includeInactive=false

목록은 items, total, offset, limit을 반환합니다. 기본 limit은 50, 최대 200입니다. 정렬은 고정 PK 순서이고 임의 SQL 정렬/검색 표현식은 받지 않습니다. 관리자 단건 조회는 비활성 행도 조회할 수 있습니다.

## 기간과 NULL 상속

user-roles, user-menu-permissions, user-button-permissions는 VALID_FROM/VALID_TO를 지원합니다. 시간은 DB의 로컬 시간 기준 ISO 형식 2026-10-07T09:00:00으로 입력합니다. VALID_FROM 생략 시 DB 현재 시간을 사용하고 VALID_TO=NULL이면 종료 제한이 없습니다. 경계 시각은 포함합니다.

동일 사용자·ROLE 또는 동일 사용자·메뉴/버튼의 활성 기간이 겹치면 ERR_CONFLICT입니다. 사용자 행 잠금으로 같은 사용자의 등록/수정을 직렬화합니다.

사용자 메뉴 예외의 ACCESS_LEVEL과 DATA_SCOPE, 버튼 예외의 ACCESS_YN은 null을 명시할 수 있습니다. null은 ROLE 상속입니다. PUT에서 필드 자체를 생략하면 기존 값이 유지됩니다. ACCESS_LEVEL=NONE 및 ACCESS_YN=N은 해당 사용자에 대한 명시적 거부입니다.

## 언어와 오류

언어 우선순위는 활성 사용자의 LANG_CODE → 요청 lang → Accept-Language → DB 기본 언어입니다. 지원하지 않는 지역 언어는 지원하는 기본 언어로 축약하며, 번역이 없으면 FALLBACK_LANG_CODE를 따라가고 마지막으로 DB 기본 언어를 확인합니다. 순환 참조는 방문 목록으로 종료합니다.

~~~json
{
  "messageCode": "ERR_ACCESS_DENIED",
  "message": "You do not have permission to access this function.",
  "langCode": "en",
  "status": 403,
  "path": "/api/common/admin/users",
  "timestamp": "2026-10-07T00:00:00Z"
}
~~~

HTTP 상태와 번역은 MessageService가 DB에서 해석합니다. SQL 오류/스택/비밀번호를 응답에 포함하지 않습니다. 메시지 저장소 장애 시 최소 내부 안전 응답으로 종료합니다. Filter도 ApiErrorWriter를 사용하면 같은 형식입니다.

## 사용자 bootstrap

GET /api/common/bootstrap는 user, langCode, menus를 반환합니다. menus는 허용된 활성 메뉴의 평면 목록이며 각 항목에 parentMenuId, 표시명, 화면 경로, grants, buttons가 포함됩니다. 비허용 상위 메뉴를 자동으로 권한 부여하지 않습니다. Frontend는 parentMenuId로 트리를 구성하며, 반환되지 않은 부모가 있으면 화면 배치 정책을 적용합니다.

grants의 접근 수준과 범위는 한 쌍으로 해석합니다. 버튼 숨김은 UX이며, API 실행 시 RequiresPermission과 업무 데이터 범위 검사를 적용해야 합니다.
