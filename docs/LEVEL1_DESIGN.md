# Level 1 구현 설계

## 표준 적용

AGENTS.md를 최우선으로 적용합니다. 기존 V001/V002와 18개 TB_* 테이블을 재사용했습니다. 생성자/수정자는 NUMBER(19) USER_ID이며 시스템 Seed만 0입니다. Master와 관계 테이블 모두 공통 5개 컬럼을 유지하고 V003에서 VERSION_NO를 추가했습니다.

표시 문자열은 Message Code로 연결합니다. 조직/사용자의 고유 명칭은 기존 NAME 컬럼을 유지하고 UI 메뉴·버튼·공통코드·오류는 번역 테이블을 사용합니다. 운영 중 늘어나는 USER_STATUS는 TB_COMMON_CODE에 두고 기존 고정 CHECK를 제거했습니다. API가 활성 USER_STATUS 공통코드를 검증합니다. 인증 가능한 상태의 해석은 Level 2와 공통 활성 사용자 정책에 연결하며 현재 ACTIVE 사용자만 권한을 조회할 수 있습니다.

Spring Security와 AI 자동설정 의존성은 Level 1 공통 코어에 필요하지 않아 제거했습니다. 기존 Java/Spring Boot/MyBatis 버전은 유지했습니다.

## 계층

~~~mermaid
flowchart TD
    C[Controller] --> S[Service Interface]
    S --> I[ServiceImpl]
    I --> M[Mapper.java + Mapper.xml]
    M --> D[Oracle]
    D --> F[Flyway Migration]
    L2[Level 2 인증] --> P[CurrentUserProvider]
    P --> W[공통 Backend 권한 검사]
    W --> C
~~~

MessageService, PermissionService, MasterService, ReferenceDataService는 인터페이스와 구현을 분리했습니다. Controller는 Mapper를 참조하지 않습니다.

권한·메시지·bootstrap·공통코드·요청/응답 객체는 record입니다. 18개 기준정보의 공통 관리 화면은 스키마가 달라 MasterResource record와 허용된 컬럼 Map을 사용합니다. 테이블별 유사 CRUD를 반복 생성하지 않도록 공통 관리 전용 API로 구성했습니다. 업무용 API는 도메인별 DTO/Service로 확장합니다.

MasterCatalog는 기존 DDL에서 개발 시 생성한 고정 허용 목록입니다. 런타임에 사용자 SQL이나 임의 테이블/컬럼을 허용하지 않습니다. MyBatis의 식별자 치환은 이 내부 목록에만 사용하며 값은 바인딩합니다. scripts/generate_master_catalog.py로 목록을 재생성할 수 있습니다. 후속 Migration에서 필드를 확장하면 생성기의 스키마 기준도 함께 갱신해야 합니다.

## 권한 정책

ROLE은 USE_YN과 양쪽 유효기간을 확인합니다. ROLE의 N/NONE은 허용을 추가하지 않으며 다른 ROLE의 허용을 취소하지 않습니다. 민감 버튼도 현재 기본 합집합 정책을 따릅니다. 별도 DENY 우선 정책을 도입하려면 명시적인 정책 확장이 필요합니다. SYSTEM_ADMIN이라는 ROLE 이름만으로 검사 우회를 하지 않습니다.

READ_PARTIAL은 부분 읽기, READ_ALL은 부분+전체 읽기, WRITE_PARTIAL은 부분 읽기+부분 쓰기, WRITE_ALL은 전체 읽기+전체 쓰기입니다. 서로 독립적인 접근 수준과 데이터 범위의 쌍을 유지합니다. READ_ALL/COMPANY와 WRITE_PARTIAL/OWN을 합쳐 WRITE_ALL/COMPANY로 계산하지 않습니다.

사용자 메뉴 예외는 지정한 필드만 기존 쌍에 덮어씁니다. 명시적인 NONE은 메뉴 접근과 버튼을 모두 차단합니다. ROLE이 없더라도 명시적 사용자 허용은 적용되며 범위가 비어 있으면 OWN입니다. 버튼 예외 NULL은 상속, Y는 허용, N은 거부입니다. 활성 메뉴·버튼·메뉴버튼 관계가 있어야 버튼 권한이 유효합니다. 겹치는 활성 사용자 예외를 직접 DB에 삽입하면 모호한 권한 계산을 중단하고 ERR_CONFLICT로 응답합니다.

requireData는 사용자 회사와 대상 회사가 같은지 먼저 확인합니다. OWN은 대상 소유 USER_ID, DEPT/SITE는 사용자의 NULL이 아닌 소속 ID, COMPANY는 같은 회사를 확인합니다. 대상 ID는 Backend가 DB에서 얻어야 하며 Client가 주장한 소속을 그대로 사용하지 않습니다.

COMMON_ADMIN은 회사 간 공통 기준정보를 운영하는 중앙 관리 메뉴입니다. 이 특별한 관리 API는 COMPANY 범위 및 해당 수준·버튼의 명시적 부여를 요구합니다. 일반 업무 API의 COMPANY 범위는 해당 사용자의 회사에만 적용됩니다.

## 조직·동시성·트랜잭션

회사-사업장, 회사-부서, 부서의 상위 회사, 사용자 회사/사업장/부서의 연결을 FK와 Service에서 함께 검증합니다. 사용자가 사업장이 있는 부서에 속하면 동일 사업장을 지정해야 합니다. 계층 부모는 활성 상태와 같은 회사/사업장이어야 하며 자기 참조와 간접 순환은 거부합니다.

MasterService 등록·수정·비활성화는 각각 하나의 트랜잭션입니다. VERSION_NO 조건을 만족하는 1행만 수정하며 불일치는 충돌입니다. 계층/언어 fallback 변경은 Oracle 테이블 잠금으로 동시 순환 생성도 직렬화합니다. 기간권한은 사용자 행 잠금 후 겹치는 구간을 확인합니다.

API가 생성하는 숫자 ID는 SQ_COMMON_ID를 사용하며 1,000,000부터 시작합니다. 기존 Seed ID는 그보다 작습니다. 별도 고객 Migration이나 외부 쓰기 경로는 같은 시퀀스를 사용하고 임의로 이 범위의 ID를 예약하지 않아야 합니다. 이미 운영 중인 DB에서 큰 ID가 존재한다면 V003 적용 전 시퀀스 시작 범위와 충돌을 검토해야 합니다.

물리 삭제는 제공하지 않습니다. 하위 행의 감사 이력과 관계는 유지합니다. 상위 조직 비활성화는 사용자의 유효성을 차단하고 비활성 ROLE/메뉴/버튼/관계는 조회에서 제외합니다.

## 캐시

초기 구현은 권한과 메시지를 장기 캐시하지 않습니다. 기간 만료 및 비활성화가 즉시 적용되며 복잡한 분산 무효화 의존성을 만들지 않기 위한 선택입니다. MyBatis 세션 조회 캐시도 STATEMENT로 제한합니다.

장기 캐시를 추가할 때는 모든 메시지·언어·사용자·ROLE·메뉴·버튼·관계/예외 변경 후 커밋을 기준으로 무효화하고, 기간 경계보다 짧은 만료와 여러 인스턴스 간 무효화를 함께 구현해야 합니다. 성능 최적화는 실제 조회 부하를 측정한 뒤 적용합니다.

## 테스트 범위

H2 Oracle 모드의 테스트는 실제 운영 SQL을 읽어 최소 문법 호환 변환을 수행합니다. 별도의 손으로 만든 테스트 스키마로 운영 테이블을 대체하지 않습니다. 다만 Oracle 고유 함수 인덱스 일부와 계층 잠금은 H2용 대체이므로 실제 Oracle Migration/잠금 검증이 추가로 필요합니다.

테스트는 18개 리소스 전체 CRUD/재활성화, FK/조직 순환, 권한 합집합과 데이터 범위, 예외 거부/NULL 상속, 기간 만료/중복, 인증 없는 접근/사용자 헤더 위장, 인증된 관리자와 제한 범위 거부, 다국어와 DB 장애 fallback, Filter 오류 형식을 확인합니다. 테스트에만 인증 제공자를 대체하며 운영 코드에 개발용 인증 우회를 넣지 않았습니다.
