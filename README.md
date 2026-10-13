# erpcom — Level 1 Common Core

AGENTS.md에 따른 기업 업무시스템 공통 백엔드입니다. Java 21, Spring Boot 4.0.8, MyBatis, Oracle, Flyway를 사용합니다.

## 구현 범위

- 기존 18개 공통 테이블을 재사용하는 기준정보 등록·조회·수정·비활성화 API
- 회사·사업장·부서·사용자 정합성 검증, 상위 조직/메뉴 순환 방지
- 기간성 ROLE 및 사용자 메뉴/버튼 예외권한
- ROLE 허용 합집합, 사용자 NULL 상속, 명시적 거부, 화면별 버튼 검사
- 읽기/쓰기와 데이터 범위를 함께 보존하는 권한 조회 및 데이터 접근 검사
- DB 메시지의 번역·HTTP 상태 해석, 언어 fallback, 공통 ApiError
- 인증 사용자별 메뉴·버튼 bootstrap, 다국어 공통코드 조회
- 감사 사용자 추적, VERSION_NO 충돌 검사, 논리적 비활성화

인증은 AGENTS.md의 Level 2 경계에 따라 별도 구현합니다. 이 프로젝트에는 Login Controller, SecurityFilterChain, JWT/Session, PasswordEncoder가 없습니다. 기본 CurrentUserProvider는 인증 사용자를 반환하지 않으므로 보호된 API는 401로 응답합니다.

## 실행

Oracle에 전용 erpcom 스키마를 준비하고 연결 설정을 지정합니다. 스키마 소유자는 Flyway의 테이블·인덱스·시퀀스 생성 권한이 필요합니다. 비밀번호는 저장소에 기록하지 않습니다.

개발 프로필은 `.idea/mysettings.ini`를 Properties 형식으로 읽습니다. 실제 파일명에는 앞쪽 점이 없으며, `.idea/.mysettings.ini`를 참조하지 않습니다. 프로젝트 루트에서 실행하면 해당 파일의 `ERPCOM_*` 값이 적용됩니다. 파일이 없으면 환경변수를 사용하고, 환경변수 또는 실행 인자로 로컬 값을 덮어쓸 수 있습니다.

| 환경변수 | 설정 |
| --- | --- |
| ERPCOM_DB_URL | JDBC 연결 주소. 로컬 INI 또는 환경변수로 지정 |
| ERPCOM_DB_USERNAME | 스키마 사용자 |
| ERPCOM_DB_PASSWORD | DB 비밀번호 |
| ERPCOM_API_PORT | 서버 포트. 기본 8082, dev 기본 8083 |
| ERPCOM_DB_MIN_IDLE / ERPCOM_DB_MAX_POOL_SIZE | dev 연결 풀 최소 대기/최대 연결 |
| ERPCOM_DB_CONNECTION_TIMEOUT_MS | dev 연결 대기 제한(ms) |
| ERPCOM_FLYWAY_ENABLED | dev Migration 활성화 |
| ERPCOM_FLYWAY_BASELINE_ON_MIGRATE / ERPCOM_FLYWAY_BASELINE_VERSION | dev 기존 스키마 기준 설정. 기준 버전 기본 0 |
| ERPCOM_SWAGGER_ENABLED | API 문서 활성화. 기본 false, dev 기본 true |
| ERPCOM_NEXACRO_ORIGIN | 허용할 정확한 Frontend Origin. dev 기본 http://localhost:9093 |
| ERPCOM_SETTINGS_LOCATION | dev 설정 파일 위치 변경. 기본 optional:file:./.idea/mysettings.ini[.properties] |

기존 `ERP_DB_URL`, `ERP_DB_USERNAME`, `ERP_DB_PASSWORD`, `ERP_PORT`, `ERP_API_DOCS_ENABLED`도 기본 설정의 호환 별칭으로 지원합니다. `ERPCOM_*`이 함께 지정되면 이를 우선합니다. `.idea`는 저장소에서 제외되며 INI는 JAR에 포함하지 않습니다.

로컬 INI를 사용하는 개발 실행은 다음과 같습니다.

~~~powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
~~~

개발 기본 포트의 API 문서는 [Swagger UI](http://localhost:8083/swagger-ui.html)에서 확인할 수 있습니다. `ERPCOM_SWAGGER_ENABLED=false`이면 문서와 API 명세를 모두 비활성화합니다. 개발 프로필을 지정하지 않은 실행은 INI를 읽지 않으며 기본 포트는 8082입니다. IDE 실행도 dev 프로필과 프로젝트 루트 작업 디렉터리를 지정합니다.

CORS는 `/api/common/**`에 적용됩니다. 등록된 Origin의 GET/POST/PUT/DELETE/OPTIONS 및 인증 관련 헤더를 허용하고, 사전 요청에는 인증을 요구하지 않습니다. 실제 보호된 API에는 기존 인증·메뉴·버튼 검사가 그대로 적용됩니다. Origin에 와일드카드, 경로 또는 사용자 인증 정보를 지정할 수 없습니다.

개발 YAML의 로그인·SSO·Redis/세션 관련 INI 값·관리자 Bootstrap은 Level 2 연결용 예약 설정입니다. Level 1은 이를 이용해 세션을 만들거나 ADMIN을 자동 생성하지 않습니다. 실제 관리자 작업에는 Level 2 인증 연결과 운영 관리자 ROLE 배정이 필요합니다.

## DB 구성

src/main/resources/db/migration의 Flyway 파일만 실제 실행 원본입니다. assets의 SQL은 기존 참고 파일입니다.

- V001: 공통 테이블 18개
- V002: 지원 언어·공통 메시지·조직/ROLE/메뉴 예제·표준 버튼·공통코드
- V003: VERSION_NO, 조직 FK 보강, 언어 fallback FK/기본 언어 유일성, 공통 관리 메뉴와 오류 번역, API ID 시퀀스

개발 Flyway 기준 버전은 INI의 `ERPCOM_FLYWAY_BASELINE_VERSION`을 따르며 기본은 0입니다. 기준 버전 1로 V001을 건너뛰는 설정 불일치를 해소했습니다. 현재 존재하지 않는 `db/dev` 경로는 실행 위치에서 제외했습니다. 기존 스키마에 같은 테이블이 있다면 해당 스키마와 Migration 이력을 먼저 맞춰야 합니다.

기존 V001/V002 내용과 체크섬은 변경하지 않았습니다. 고객별 초기 사용자와 ROLE 배정은 후속 고객 Migration으로 관리합니다. 시작 시 Mapper로 Seed를 삽입하지 않습니다.

## 주요 API

| 경로 | 용도 / 접근 |
| --- | --- |
| GET /api/common/languages | 지원 언어 조회 / 공개 |
| GET /api/common/messages/{code}?lang=en | 메시지와 HTTP 상태 조회 / 공개 |
| GET /api/common/permissions/me | 내 유효 권한 / 인증 필요 |
| GET /api/common/bootstrap | 내 허용 메뉴·버튼·번역 표시명 / 인증 필요 |
| GET /api/common/codes/{group}?lang=en | 번역된 활성 공통코드 / 인증 필요 |
| GET /api/common/admin/resources | 관리 리소스/허용 필드 정의 / 공통 관리 권한 |
| GET /api/common/admin/{resource} | 목록·페이지 / 공통 관리 권한 |
| GET /api/common/admin/{resource}/{key} | 단건 / 공통 관리 권한 |
| POST /api/common/admin/{resource} | 등록 / BTN_NEW |
| PUT /api/common/admin/{resource}/{key} | 부분 수정 / BTN_SAVE |
| DELETE /api/common/admin/{resource}/{key}?expectedVersion=0 | 비활성화 / BTN_DELETE |

공통 관리 API는 회사 간 기준정보를 관리하는 중앙 운영자 API입니다. COMMON_ADMIN 메뉴의 필요한 접근 수준과 COMPANY 범위, 해당 화면의 버튼 권한을 함께 요구합니다. 회사별 담당자를 위한 업무 API는 PermissionService.requireData로 별도 데이터 범위를 적용해야 합니다.

자세한 요청 예시와 리소스 목록은 [API 가이드](docs/COMMON_API.md), 설계 결정은 [구현 설계](docs/LEVEL1_DESIGN.md), 인증 연결은 [Level 2 연결 계약](docs/LEVEL2_INTEGRATION.md)을 참고합니다. ARCHITECTURE.md와 AGENTS.md가 충돌하면 AGENTS.md를 우선합니다.

## 검증

~~~powershell
.\gradlew.bat test
.\gradlew.bat bootJar
~~~

테스트는 H2의 Oracle 모드에서 운영 Migration을 읽어 Oracle INSERT ALL을 단일 INSERT로 변환합니다. Oracle 함수 인덱스 중 로그인/기본 언어 유일성은 테스트용 생성 컬럼으로 대체하고, 기간권한 중복은 서비스 검증으로 확인합니다. 계층 잠금도 H2 전용 대체 문장을 사용합니다.

따라서 이 테스트는 실제 Oracle Flyway 적용과 Oracle 잠금 동작의 검증을 대체하지 않습니다. 설정 변경 검증은 가상 INI와 H2를 사용하며 실제 로컬 INI의 연결 정보로 DB에 접속하거나 Migration을 실행하지 않습니다. 개발 설정 테스트는 INI import, 환경 설정 덮어쓰기, Flyway 기준 버전, Swagger 토글, CORS와 인증 검사 유지를 확인합니다.

Windows에서 Java 임시 소켓 경로 때문에 Gradle 실행이 실패하면, 쓰기 가능한 로컬 임시 폴더를 지정한 동일한 셸에서 다시 실행할 수 있습니다.

~~~powershell
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp'
.\gradlew.bat test bootJar --offline --no-daemon
~~~
