# Level 2 인증 연결 계약

## 인증 사용자 제공

Level 2 Security Starter는 CurrentUserProvider 타입의 Bean을 하나 제공합니다. 이 Bean은 검증된 SecurityContext, 세션 또는 토큰에서 불변 TB_USER.USER_ID를 반환해야 합니다. 미인증 상태에서는 null을 반환합니다. 로그인 ID 문자열이나 X-User-Id 헤더를 인증 근거로 사용하지 않습니다.

~~~java
@Bean
CurrentUserProvider currentUserProvider(VerifiedAuthenticationContext authentication) {
    return request -> authentication.currentUserId();
}
~~~

위 VerifiedAuthenticationContext는 Level 2 구현체의 검증된 인증 컨텍스트를 나타내는 계약 예시입니다. 이 프로젝트에 인증 기능이 자동 생성되었다는 의미는 아닙니다. 같은 타입의 Bean이 등록되면 Level 1의 기본 미인증 제공자를 대체합니다. 등록된 USER_ID는 활성 사용자/회사/선택 소속 검사를 통과해야 합니다.

## 관리자 준비

V003은 ROLE 10(SYSTEM_ADMIN)에 COMMON_ADMIN 메뉴와 화면별 버튼 권한을 부여합니다. 로그인 가능한 계정과 TB_USER_ROLE의 사용자 배정은 생성하지 않습니다.

고객별 후속 Migration이나 승인된 운영 등록 절차에서 사용자를 만들고 ROLE을 배정합니다. 비밀번호 해시는 Level 2의 PasswordEncoder로 생성해야 합니다. Startup Mapper Seed나 평문 비밀번호를 사용하지 않습니다. 비밀번호/실패 횟수/잠금/인증 방식 등은 공통 관리 API에서 노출하거나 수정할 수 없습니다.

## 업무 API 보호

~~~java
@RequiresPermission(menu = "PM_PROJECT_LIST", button = "BTN_SAVE",
                    level = AccessLevel.WRITE_PARTIAL)
@PutMapping("/api/common/example/{id}")
public ExampleResponse update(...) {
    // 업무 Service가 DB에서 대상의 실제 회사/사업장/부서/소유자 ID를 조회한 다음 검사합니다.
    permissions.requireData(userId, menuId, AccessLevel.WRITE_PARTIAL,
        new DataTarget(companyId, siteId, deptId, ownerUserId));
    // 검증 및 변경을 같은 업무 트랜잭션 안에서 처리합니다.
}
~~~

RequiresPermission 인터셉터는 /api/common/**에 적용됩니다. 다른 경로에 업무 API를 추가하면 인터셉터 경로를 확장하거나 Service에서 requireButton/requireData를 호출해야 합니다. 권한 어노테이션만 붙이고 경로 연결을 생략하지 않습니다.

목록 조회는 전체 데이터를 먼저 반환한 후 Frontend가 필터링하는 방식으로 구현하지 않습니다. 유효한 grants를 기준으로 DB 조회 조건을 제한합니다. 메뉴/버튼 검사만으로 데이터 범위 검사를 대신하지 않습니다.

## Filter 오류

Level 2의 AuthenticationEntryPoint/AccessDeniedHandler 등은 공통 ApiErrorWriter를 주입받아 write(request,response,new ApiException(messageCode))를 호출합니다.

- 미인증: ERR_AUTH_REQUIRED
- 인증되었지만 기능 거부: ERR_ACCESS_DENIED
- 내부 처리 오류: ERR_INTERNAL_SERVER

HTTP 상태와 번역문은 MessageService가 TB_MESSAGE/TB_MESSAGE_LANG에서 해석합니다. Java Filter에 번역문과 HTTP 정책을 중복 하드코딩하지 않습니다. 비밀번호/토큰이나 내부 예외 문자열을 args에 전달하지 않습니다.

## 운영 연결

Oracle URL·계정·비밀번호는 ERPCOM_DB_URL, ERPCOM_DB_USERNAME, ERPCOM_DB_PASSWORD로 전달합니다. 기존 ERP_* 별칭도 지원합니다. dev 프로필에서만 .idea/mysettings.ini를 읽으며 파일은 저장소·JAR에 포함하지 않습니다. Level 1 단독 실행 시 언어·메시지 공개 조회만 가능하며 보호된 API는 401입니다. 인증 연결과 고객 데이터/ROLE 배정 후 관리자 API를 사용할 수 있습니다.

API 문서는 ERPCOM_SWAGGER_ENABLED로 제어합니다. 기본 프로필에서는 false, dev에서는 true가 기본이며 로컬 INI/환경 설정으로 변경합니다. 기본 프로필에서는 이전 ERP_API_DOCS_ENABLED 별칭도 지원합니다. Nexacro CORS는 ERPCOM_NEXACRO_ORIGIN의 정확한 Origin을 사용합니다. 관리 화면의 실제 Frontend는 이 백엔드의 resources와 bootstrap API를 사용해 별도 구축합니다.

application-dev.yaml의 erpcom.auth, erpcom.sso, erpcom.bootstrap은 Level 2용 예약 설정입니다. 세션 TTL은 INI의 ERPCOM_SESSION_TTL_HOURS와 맞춘 session-ttl-hours를 사용합니다. 실패 횟수·잠금 시간·BCrypt 설정과 관리자 식별 정보도 해당 ERPCOM_* 키에서 가져옵니다. 이 값들을 검증하고 실제 로그인·SSO·세션·Bootstrap을 수행하는 책임은 Level 2에 있습니다. 현재 ERPCOM_BOOTSTRAP_ENABLED=true가 지정되어도 Level 1은 계정을 자동 생성하지 않습니다.
