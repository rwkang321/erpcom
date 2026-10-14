# ErpCom Level 1 — 처음부터 따라 읽는 공통 시스템 설명서

작성 기준: 2026-10-14, 현재 원본 SQL·Java와 갱신한 [AGENTS.md](F:/java/workspace/erpcom/AGENTS.md).

**현재 상태:** 원본 V001에는 21개 테이블 정의, V002에는 테이블별 초기 자료가 있습니다.
단위·통화·금액 표시 소스와 `common/basis`의 Java 파일 5개도 원본에 반영되어 있습니다.
이는 소스가 준비됐다는 뜻이며 실제 Oracle DB 초기화나 Level 2 로그인·운영 화면 실행을 확인했다는 뜻은 아닙니다.
2026-10-07의 MasterCatalog 124줄 설명은 학습용 이전 버전으로 구분해서 보존했습니다.
현재의 소수형·21개 기준정보·basis 연결은 6.1, 15.10, 17장에서 설명합니다.

이 문서는 Java를 잘 모르는 사람도 읽을 수 있도록 작성한 설명서입니다. 먼저 프로그램이 하는 일을 일상적인 말로 설명하고, 그 일을 맡은 **실제 Java 파일과 메서드**를 바로 연결합니다. 기존 문서를 먼저 읽을 필요는 없습니다.

예제에 나오는 새 회사명·새 사용자명·새 번호는 설명용입니다. 실제 개발 DB의 접속 정보나 개인 설정의 비밀번호는 적지 않습니다. 테스트용 사용자 1번과 2번은 자동 테스트가 준비하는 가짜 사용자이며, 실제 시스템에 기본 로그인 계정이 있다는 뜻은 아닙니다.

## 읽는 순서

처음에는 1~5장을 읽으세요. “무엇을 만들었는가”, “파일들이 왜 나뉘었는가”, “회사 하나를 저장하면 무슨 일이 일어나는가”를 이해할 수 있습니다. 그다음 6~10장에서 입력 검사·권한·다국어·화면 준비 데이터를 배웁니다. 설정은 11장, DB 변경은 12장, 테스트는 13장입니다. 특정 파일을 찾을 때는 15장의 전체 파일 목록을 보세요.

현재 추가된 단위·통화·금액 표시 기준과 구현은 17장에 있습니다. 인증 방식과 이 기준정보가 어떻게 연결되는지는
[ErpComLevel2.md](F:/java/workspace/erpcom/docs/ErpComLevel2.md)를 함께 읽으세요.

- [1. 지금 만든 것은 무엇인가](#chapter-1)
- [2. 꼭 필요한 말부터 배우기](#chapter-2)
- [3. 프로그램이 켜질 때와 요청이 들어올 때](#chapter-3)
- [4. 21개 테이블은 각각 무엇을 보관하는가](#chapter-4)
- [5. 회사 등록·조회·수정·비활성화를 끝까지 따라가기](#chapter-5)
- [6. 잘못된 입력을 막는 메서드들](#chapter-6)
- [7. 누가 어떤 기능을 사용할 수 있는가](#chapter-7)
- [8. 한국어·영어 메시지를 고르는 과정](#chapter-8)
- [9. 오류가 같은 모양으로 나오는 이유](#chapter-9)
- [10. 화면을 시작할 때 필요한 데이터](#chapter-10)
- [11. application-dev.yaml과 mysettings.ini의 역할](#chapter-11)
- [12. DB를 만들고 초기 자료를 넣는 두 개의 SQL](#chapter-12)
- [13. 자동 테스트가 확인하는 것](#chapter-13)
- [14. 실제로 읽고 추적하는 연습](#chapter-14)
- [15. 모든 Java 파일과 메서드 찾기](#chapter-15)
- [16. 자주 생기는 오해와 용어 사전](#chapter-16)
- [17. 재고 단위·통화·금액 표시와 basis 구현](#chapter-17)

특정 코드를 자세히 이해하려면 아래 설명으로 바로 이동할 수 있습니다.

- [@Valid·@NotNull·@Size·@Min의 실제 요청 예제](#validation-examples)
- [POST companies의 JSON을 MasterRequest로 읽는 이유와 순서](#request-binding-examples)
- [이전 MasterCatalog 124줄의 학습용 설명](#catalog-line-guide)
- [직접 만든 @RequiresPermission과 preHandle()의 실제 실행 예제](#permission-annotation-examples)

<a id="chapter-1"></a>
## 1. 지금 만든 것은 무엇인가

### 1.1 회사 업무 프로그램을 하나의 건물이라고 생각해 보기

회사에서 사용할 프로그램을 건물이라고 생각해 봅시다. 건물에는 회계실, 인사실, 구매실처럼 여러 방이 있을 수 있습니다. 방마다 하는 일은 다르지만, 건물 전체가 함께 사용하는 것이 있습니다.

“어느 회사의 직원인가”, “어느 부서인가”, “어떤 방에 들어갈 수 있는가”, “그 방에서 저장 버튼을 눌러도 되는가”, “안내문을 한국어로 보여 줄 것인가 영어로 보여 줄 것인가”는 어느 업무에도 필요합니다.

**Level 1은 이런 공통 기반을 만든 단계입니다.** 회계 계산이나 프로젝트 일정 계산 같은 실제 업무 처리는 아직 이 단계의 중심이 아닙니다. 여러 업무가 같은 사용자·조직·권한·메시지를 사용할 수 있도록 기초를 준비했습니다.

현재 구현한 내용은 다음과 같습니다.

| 하고 싶은 일 | 현재 맡은 코드 |
|---|---|
| 회사·사업장·부서·사용자 등의 기준정보를 관리한다 | `MasterController.java`, `MasterServiceImpl.java` |
| ROLE과 사용자별 예외를 합쳐 최종 권한을 계산한다 | `PermissionServiceImpl.java` |
| 메뉴와 버튼 실행 전에 권한을 확인한다 | `CommonWebConfiguration.java`의 `preHandle()` |
| 한국어·영어 등 알맞은 메시지를 찾는다 | `MessageServiceImpl.java` |
| 오류를 같은 JSON 모양으로 돌려준다 | `ApiExceptionHandler.java`, `ApiErrorWriter.java` |
| 화면에서 쓸 메뉴·버튼·공통코드를 준비한다 | `ReferenceDataServiceImpl.java` |
| 필요한 테이블과 기본값을 만든다 | 통합 `V001`, 테이블별 Seed `V002` SQL 파일 |
| 개발용 설정 파일을 읽고 CORS를 적용한다 | `application-dev.yaml`, `CommonCorsProperties.java`, `CommonWebConfiguration.java` |

단위·통화·금액 표시의 세 영역도 현재 원본 SQL·Java에 반영되어 있습니다.

| 공통 기능 | 기준정보 원본 | 설명 |
|---|---|---|
| 재고·자산 단위를 관리한다 | `TB_UNIT` | EA·KG·G 등 단위와 수량 정밀도·환산 기준 |
| 지원 통화를 관리한다 | `TB_CURRENCY` | KRW·USD·EUR 등 통화와 기본 소수 자릿수 |
| 메뉴·금액 항목마다 표시 방법을 정한다 | `TB_MENU_AMOUNT_FORMAT` | 원·천원·억원 표시 배율, 소수 자릿수, 반올림 |

현재 `MasterCatalog.java`와 공통 관리 API는 기존 18개와 새 3개를 합친 21개 기준정보를 처리합니다.
화면용 조회와 계산은 `common/basis`의 파일 5개가 담당합니다. 실제 DB에 표가 생성되려면 통합 Migration을 적용해야 합니다.

### 1.2 Level 2는 신분을 확인하는 단계

건물 입구에서 “정말 이 직원이 맞나요?”라고 확인하는 일은 **인증**입니다. 아이디·비밀번호를 확인하거나, 회사 SSO로 로그인하거나, 토큰이나 세션을 검사하는 일이 여기에 속합니다.

확인된 직원에게 “이 방에 들어갈 수 있나요?”라고 묻는 일은 **인가**, 즉 권한 검사입니다.

현재 Level 1에는 권한 검사와 사용자 공통 정보가 있습니다. 실제 로그인·비밀번호 검사·JWT·세션·SSO·Spring Security 필터는 Level 2에서 연결해야 합니다. 따라서 지금 코드만 실행했다고 관리자 로그인이 생기지는 않습니다.

그 연결 구멍이 `CurrentUserProvider.java`의 `userId(HttpServletRequest request)`입니다. 나중에 Level 2가 검증한 사용자 번호를 돌려주면, Level 1이 그 번호로 조직과 권한을 조회합니다. Level 2가 없는 현재 기본 구현은 `null`을 돌려줍니다. 인증이 필요한 API는 이 경우 `ERR_AUTH_REQUIRED`를 응답합니다.

### 1.3 메뉴 정보가 있다고 화면까지 만들어진 것은 아니다

DB에는 프로젝트 메뉴 예제와 `pm/PM_PJ010.xfdl` 같은 화면 경로가 들어 있습니다. 이것은 “화면 주소를 적은 목록”입니다. 이 프로젝트가 그 Nexacro 화면 파일이나 프로젝트 업무 API까지 만든 것은 아닙니다.

`ReferenceDataServiceImpl.bootstrap()`은 메뉴 목록을 내려주지만 화면을 그리지 않습니다. 화면 프로그램이 그 목록을 받아 메뉴를 표시해야 합니다. `BTN_EXCEL`이라는 버튼 정보가 있어도 실제 Excel 파일 생성 코드는 별도 업무에서 만들어야 합니다.

<a id="chapter-2"></a>
## 2. 꼭 필요한 말부터 배우기

### 2.1 파일, 클래스, 메서드

`MasterController.java`는 Java 소스가 들어 있는 **파일 이름**입니다. 그 안의 `MasterController`는 관련 작업을 묶어 놓은 **클래스**입니다. 클래스 안의 `create()`는 회사 등 기준정보 등록 요청을 받는 **메서드**입니다. 메서드는 “특정 일을 하는 작은 작업 단위”라고 생각하면 됩니다.

이 문서의 `MasterServiceImpl.create()`라는 표기는 “`MasterServiceImpl.java` 안의 `create` 메서드”라는 뜻입니다. 괄호 안의 값은 일을 부탁하면서 함께 건네는 재료입니다.

예를 들어 `get("companies", "100")`은 “회사 목록에서 회사 번호 100의 자료를 가져와 주세요”라는 부탁입니다. 같은 `get()`이라도 어느 클래스에 있는지에 따라 역할이 다르므로 파일 이름을 함께 봐야 합니다.

### 2.2 API, HTTP, URL, JSON

**API**는 화면 프로그램이 서버에 일을 부탁하는 창구입니다. **URL**은 그 창구의 주소입니다. `/api/common/admin/companies`는 회사 기준정보 창구의 주소입니다.

**HTTP 방식**은 부탁의 종류입니다.

| 방식 | 쉬운 뜻 | 이 프로젝트의 대표 메서드 |
|---|---|---|
| GET | 읽어 주세요 | `MasterController.list()`, `get()` |
| POST | 새로 만들어 주세요 | `MasterController.create()` |
| PUT | 기존 내용을 고쳐 주세요 | `MasterController.update()` |
| DELETE | 사용하지 않게 해 주세요 | `MasterController.deactivate()` |
| OPTIONS | 브라우저가 먼저 통신 가능 여부를 물어본다 | Spring MVC의 CORS 처리 |

여기서 DELETE는 DB의 행을 지우지 않습니다. `USE_YN`을 `N`으로 바꾸는 방식입니다. 이 프로젝트의 PUT도 모든 항목을 새 값으로 교체하지 않고, 보낸 항목만 바꾸는 방식입니다.

**JSON**은 데이터를 주고받는 글자 형식입니다. `{ "COMPANY_NAME": "새 회사" }`는 “회사 이름이라는 칸에 새 회사라는 값이 들어 있다”는 뜻입니다. `[]`는 여러 항목을 담은 목록입니다.

### 2.3 DB, 테이블, 행, 컬럼, 키

**DB**는 자료를 오래 보관하는 저장소입니다. **테이블**은 엑셀의 표 하나처럼 생각하면 됩니다. 회사 표인 `TB_COMPANY`에는 여러 회사가 들어갑니다. **행**은 회사 한 곳의 자료이고, **컬럼**은 회사 번호나 회사 이름 같은 칸입니다.

**기본키(PK)**는 행을 정확히 찾는 이름표입니다. 회사에서는 `COMPANY_ID`가 기본키입니다. 회사 이름이 바뀌어도 번호는 그대로 두기 때문에 같은 회사를 계속 찾을 수 있습니다.

**외래키(FK)**는 다른 표의 이름표를 가리키는 연결입니다. 사업장의 `COMPANY_ID`는 “이 사업장이 어느 회사에 속하는가”를 가리킵니다. 없는 회사 번호를 적지 못하게 DB가 검사합니다.

기본키가 두 칸 이상이면 **복합키**입니다. 번역 표의 `MESSAGE_CODE + LANG_CODE`가 그렇습니다. 같은 메시지라도 한국어 번역과 영어 번역은 다른 행이기 때문입니다.

### 2.4 Controller → Service → Mapper → DB

식당에 주문한다고 생각하면 각 역할이 이해하기 쉽습니다.

| 프로그램 역할 | 식당 비유 | 실제 예 |
|---|---|---|
| Controller | 주문을 받는 직원 | `MasterController.create()` |
| Service | 주문을 검사하고 조리 순서를 정하는 담당자 | `MasterServiceImpl.create()` |
| Mapper | 창고에서 재료를 읽고 기록하는 담당자 | `MasterMapper.insert()` |
| XML의 SQL | 창고 담당자가 실행할 구체적인 작업 지시 | `MasterMapper.xml`의 `insert` |
| DB | 자료가 보관된 창고 | `TB_COMPANY` |

Controller가 곧바로 SQL을 실행하지 않습니다. 서비스가 먼저 “허용된 항목인가”, “필수값이 있는가”, “회사와 부서가 맞는가”를 확인한 뒤 Mapper를 부릅니다.

**Service 인터페이스**와 **ServiceImpl**이 나뉜 이유도 알아두세요. `MasterService.java`는 가능한 작업 이름을 적은 약속입니다. `MasterServiceImpl.java`는 그 약속을 실제로 수행하는 내용입니다. Controller는 `MasterService`라는 약속을 받지만, 실행할 때 Spring이 현재 구현체인 `MasterServiceImpl`을 연결합니다.

### 2.5 record, Map, List, null

`record`는 자료를 묶어 전달하는 작은 상자입니다. `MasterRequest`는 입력 상자이고, `ApiError`는 오류 결과 상자입니다. record의 `expectedVersion()`은 그 상자에서 `expectedVersion` 값을 꺼내는 메서드입니다. 이런 꺼내기 메서드는 Java가 자동으로 만듭니다.

`Map<String, Object>`는 “이름과 값의 짝”을 담습니다. 회사 행에서 `row.get("COMPANY_NAME")`으로 이름을 꺼낼 수 있습니다. 서로 다른 21개 테이블을 공통 처리하기 위해 관리 API의 행은 Map으로 전달합니다.

`List`는 순서가 있는 목록입니다. `List<LanguageInfo>`는 언어 정보 상자를 여러 개 담은 목록입니다. `Set`은 같은 값이 중복되지 않게 모으는 주머니입니다. 언어 순환이나 부서 순환을 확인할 때 사용합니다.

`null`은 “값이 없음”입니다. `ACCESS_LEVEL=null`은 이 프로젝트에서 “ROLE의 값을 그대로 물려받는다”는 뜻입니다. `ACCESS_LEVEL="NONE"`은 “메뉴를 허용하지 않는다”는 명시적인 값입니다. 두 값은 다릅니다.

### 2.6 @로 시작하는 표시는 무엇인가

`@RestController` 같은 표시는 Spring에게 알려주는 꼬리표입니다. 보통 메서드 본문처럼 직접 실행할 코드는 아닙니다.

| 표시 | 쉬운 뜻 |
|---|---|
| `@SpringBootApplication` | 이곳에서 프로그램을 시작하고 구성 요소를 찾는다 |
| `@RestController` | HTTP 요청을 받고 자료를 응답하는 창구다 |
| `@RequestMapping`, `@GetMapping` 등 | 어느 주소와 방식으로 이 메서드를 부를지 정한다 |
| `@Service` | 실제 처리 담당자다 |
| `@Mapper` | MyBatis가 연결할 DB 작업 약속이다 |
| `@Configuration`, `@Bean` | 프로그램에서 함께 사용할 구성 요소를 준비한다 |
| `@Valid` | 이 입력 객체 안의 검사표를 실행하게 한다. 자세한 두 요청 예제는 2.8 |
| `@NotNull` | 지정한 값이 null, 즉 없는 값이면 거절한다. 두 요청 예제는 2.9 |
| `@Size` | 지정한 문자열·목록·Map의 크기를 검사한다. 현재 values Map의 항목 수를 검사하며 예제는 2.10 |
| `@Min` | 지정한 숫자가 최소값 이상인지 검사한다. 현재 expectedVersion은 0 이상이며 예제는 2.11 |
| `@Transactional` | DB 변경을 한 묶음으로 처리한다 |
| `@RestControllerAdvice`, `@ExceptionHandler` | 오류를 모아서 공통 방식으로 응답한다 |
| `@RequiresPermission` | 이 창구에 필요한 메뉴·버튼·권한 범위를 표시한다 |

`@RequiresPermission`은 우리가 만든 표시입니다. 표시만 붙인다고 저절로 보안 검사가 생기지는 않습니다. `CommonWebConfiguration`의 `preHandle()`이 그 표시를 읽고 검사하기 때문에 작동합니다.

<a id="validation-examples"></a>
### 2.7 먼저 실제 검사표를 눈으로 보기

현재 [MasterRequest.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/master/MasterRequest.java)의 입력 상자는 다음과 같습니다. 실제 어노테이션 철자는 `@NotNull`입니다.

```java
public record MasterRequest(
        @NotNull @Size(min = 1, max = 40) Map<String, Object> values,
        @Min(0) Long expectedVersion) {}
```

학교에 신청서를 낸다고 생각해 봅시다. 신청서의 큰 칸이 values이고, 수정할 때 확인할 번호 칸이 expectedVersion입니다. 신청서를 검사하는 선생님이 항목별 검사표를 보고 확인하는 것처럼, 서버의 검증 도구가 어노테이션을 읽습니다.

- `@NotNull`: “values 칸 자체가 없어서는 안 된다.”
- `@Size(min=1,max=40)`: “values 안에 적은 이름-값 짝은 1개부터 40개까지여야 한다.”
- `@Min(0)`: “expectedVersion을 적었다면 0 이상의 숫자여야 한다.”
- Controller의 `@Valid`: “이 신청서에 붙은 검사표를 지금 확인해라.”

각 표시는 다른 일을 합니다. 그 차이를 실제 요청으로 보겠습니다. **아래 관리 API 예제는 신분 확인과 해당 작업의 권한 검사를 이미 통과했다고 가정합니다.** 현재 Level 1 기본 인증 제공자는 null을 반환하므로, 로그인 연결 없이 실제 API를 호출하면 이 입력 검사보다 먼저 401이 나올 수 있습니다.

### 2.8 @Valid — 검사표를 실행하도록 요청하기: 실제 요청 두 가지

**사용되는 자리부터 확인합니다.** [MasterController.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/master/MasterController.java)의 등록과 수정 메서드 모두 다음 매개변수를 갖습니다.

```java
@Valid @RequestBody MasterRequest body
```

`@RequestBody`는 “HTTP 본문의 JSON을 body라는 Java 객체로 읽어라”, `@Valid`는 “읽은 객체의 검사 조건을 확인해라”입니다. `@Valid` 자체가 “필수”나 “0 이상”이라는 조건은 아닙니다. 그 조건은 MasterRequest 안의 다른 표시들에 적혀 있습니다.

**예 1 — 회사 신규 등록에서 values가 null인 경우**

```http
POST /api/common/admin/companies
Content-Type: application/json
```

```json
{ "values": null }
```

JSON을 읽으면 `body.values()`가 null입니다. create()의 매개변수에 붙은 @Valid 때문에 검증 도구가 MasterRequest의 검사표를 확인하고, values의 @NotNull 위반을 발견합니다. **Controller의 create() 본문은 실행되지 않고**, `ApiExceptionHandler.validation()`이 ERR_INVALID_INPUT 오류를 응답합니다. 정상 메시지 DB 기준 HTTP 400입니다. SQL insert까지 가지 않습니다.

**예 2 — 회사 수정에서 expectedVersion이 음수인 경우**

```http
PUT /api/common/admin/companies/100
Content-Type: application/json
```

```json
{
  "values": { "COMPANY_NAME": "새 회사 이름" },
  "expectedVersion": -1
}
```

이번에는 values가 있고 항목도 하나입니다. 하지만 update()의 @Valid가 expectedVersion의 @Min(0) 검사까지 실행하므로 -1을 거절합니다. 역시 update() 본문과 MasterServiceImpl.update()까지 들어가지 않습니다.

**비교 — 검사표를 통과하는 회사 신규 등록**

```json
{
  "values": {
    "COMPANY_CODE": "VALID01",
    "COMPANY_NAME": "배움 회사"
  }
}
```

values가 있고 항목 수가 2이며 expectedVersion은 null이므로 세 검사 조건을 통과합니다. 이후 Controller가 Service를 호출합니다. 이것이 전체 저장 성공을 보장하지는 않습니다. 회사 코드가 중복되는지, 컬럼 길이가 맞는지 등 다음 단계의 검사도 필요합니다.

@Valid는 **Map 안의 모든 업무 항목을 저절로 이해하는 기능이 아닙니다.** 현재 values는 `Map<String,Object>`이고 그 안의 COMPANY_NAME에 개별 @NotNull이 붙어 있지 않습니다. 회사 이름 필수 여부는 `MasterCatalog`의 required와 `MasterServiceImpl.convert()`가 따로 검사합니다.

### 2.9 @NotNull — “칸 자체가 있는가”: 실제 요청 두 가지

현재 @NotNull은 `MasterRequest.values`에 붙어 있습니다. null은 “내용이 없는 신청서 칸”이라고 생각하면 됩니다. values를 안 보내는 것과 null이라고 명시하는 것은 현재 record 입력에서 모두 값이 없는 상태가 됩니다.

**예 1 — values를 통째로 빼먹었다**

```http
POST /api/common/admin/companies
Content-Type: application/json
```

```json
{}
```

읽을 values 값이 없으므로 `body.values()`는 null입니다. @NotNull이 거절합니다. “COMPANY_NAME이 없다”를 검사한 것이 아니라 **values라는 큰 상자 자체가 없다**를 검사한 것입니다.

**예 2 — 수정 신청서에 values=null이라고 적었다**

```http
PUT /api/common/admin/companies/100
Content-Type: application/json
```

```json
{ "values": null, "expectedVersion": 0 }
```

버전이 0이라도 수정할 values가 null이므로 @NotNull이 거절합니다. expectedVersion이 올바른지와 values가 있는지는 별개 검사입니다.

**중요한 비교 — 빈 상자와 없는 상자는 다릅니다.**

```json
{ "values": {} }
```

빈 Map은 존재하므로 @NotNull만 놓고 보면 통과합니다. 하지만 항목 수 0이라 @Size(min=1)가 거절합니다. “null이면 실패”, “안에 아무것도 없어도 실패”를 두 표시가 나누어 맡습니다.

또한 다음 자료는 values Map이 존재하므로 @NotNull을 통과합니다.

```json
{ "values": { "COMPANY_CODE": "NULL01", "COMPANY_NAME": null } }
```

현재 @NotNull은 Map 자체를 검사하지 COMPANY_NAME 값을 검사하지 않습니다. 이 자료는 @Size도 통과하지만, 나중에 Service의 convert()가 회사 이름 required=true 규칙으로 거절합니다. @NotNull만으로 빈 문자열·공백·Map 내부 값을 모두 막는다고 이해하면 안 됩니다.

### 2.10 @Size — “이 상자 안의 항목이 몇 개인가”: 실제 요청 예제

현재 `@Size(min=1,max=40)`는 **values Map의 이름-값 짝 개수**를 셉니다. COMPANY_NAME이라는 문장의 글자 수를 세는 표시가 아닙니다. 회사 이름의 바이트 길이는 MasterCatalog와 convert()가 검사합니다.

**예 1 — 빈 회사 등록 자료: 0개라서 실패**

```http
POST /api/common/admin/companies
Content-Type: application/json
```

```json
{ "values": {} }
```

values는 있어서 @NotNull은 통과하지만 항목 수가 0입니다. 최소 1개 조건을 만족하지 못해 @Size가 거절합니다.

**예 2 — 회사 이름 한 칸만 수정: 1개라서 크기 검사 통과**

```http
PUT /api/common/admin/companies/100
Content-Type: application/json
```

```json
{
  "values": { "COMPANY_NAME": "배움 회사 새 이름" },
  "expectedVersion": 0
}
```

values 안의 짝은 COMPANY_NAME 하나라 크기가 1입니다. 이 @Size 조건을 통과합니다. 바깥의 expectedVersion은 values 안에 없으므로 이 개수에 더하지 않습니다. 실제 수정은 행이 존재하고 현재 버전도 0이어야 성공합니다.

**예 3 — 회사 등록에 코드와 이름을 보냄: 2개라서 크기 검사 통과**

```json
{ "values": { "COMPANY_CODE": "SIZE01", "COMPANY_NAME": "배움 회사" } }
```

코드와 이름 두 짝이므로 크기는 2입니다. 회사 이름이 다섯 글자여도 Map 크기가 5로 바뀌지는 않습니다.

**예 4 — 너무 많은 항목: 41개라서 실패**

아래는 POST companies에서 항목 수 제한을 확인하기 위한 요청 자료입니다. F01~F41은 실제 회사 편집 컬럼이 아닙니다. 권한 검사를 통과한 HTTP 요청에서는 먼저 @Size 단계가 41개라는 이유로 거절하고 Service의 허용 필드 검사까지 가지 않습니다.

```json
{
  "values": {
    "F01": 1, "F02": 2, "F03": 3, "F04": 4, "F05": 5,
    "F06": 6, "F07": 7, "F08": 8, "F09": 9, "F10": 10,
    "F11": 11, "F12": 12, "F13": 13, "F14": 14, "F15": 15,
    "F16": 16, "F17": 17, "F18": 18, "F19": 19, "F20": 20,
    "F21": 21, "F22": 22, "F23": 23, "F24": 24, "F25": 25,
    "F26": 26, "F27": 27, "F28": 28, "F29": 29, "F30": 30,
    "F31": 31, "F32": 32, "F33": 33, "F34": 34, "F35": 35,
    "F36": 36, "F37": 37, "F38": 38, "F39": 39, "F40": 40,
    "F41": 41
  }
}
```

@Size 자체는 null을 실패로 보지 않습니다. 그래서 현재 values에는 @NotNull도 함께 필요합니다. 크기 규칙을 통과했다고 F01 같은 모르는 칸이 허용되는 것은 아니며, 항목 수가 적으면 이후 normalize()가 그 필드 이름을 거절합니다.

### 2.11 @Min — “숫자가 이 값보다 작지 않은가”: 실제 요청 예제

현재 `@Min(0)`은 `MasterRequest.expectedVersion`에 붙어 있습니다. “0도 허용하고 1·2·3도 허용하지만 -1·-2는 허용하지 않는다”는 뜻입니다.

**예 1 — 첫 수정에서 버전 0을 보낸다**

```http
PUT /api/common/admin/companies/100
Content-Type: application/json
```

```json
{ "values": { "COMPANY_NAME": "첫 수정 이름" }, "expectedVersion": 0 }
```

0은 최소값 0보다 작지 않으므로 @Min을 통과합니다. 실제 DB 버전도 0이고 다른 검사도 통과하면 수정됩니다. DB 버전이 1이면 @Min은 여전히 통과하지만 DB 버전 비교에서 ERR_CONFLICT가 납니다.

**예 2 — 음수 -1을 보낸다**

```json
{ "values": { "COMPANY_NAME": "잘못된 수정" }, "expectedVersion": -1 }
```

같은 PUT 주소에서 -1은 0보다 작으므로 @Min이 거절합니다. 자료 버전을 조회하는 Mapper까지 가지 않습니다.

**예 3 — 두 번째 수정에서 버전 1을 보낸다**

```json
{ "values": { "COMPANY_NAME": "두 번째 수정 이름" }, "expectedVersion": 1 }
```

1도 @Min을 통과합니다. @Min(0)은 “반드시 0이어야 한다”가 아니라 “0 이상이어야 한다”입니다. 역시 DB의 현재 버전과 같아야 실제 수정이 됩니다.

**예 4 — null을 보낸다**

```json
{ "values": { "COMPANY_NAME": "버전 없는 수정" }, "expectedVersion": null }
```

@Min은 null을 거절하지 않습니다. 하지만 이 자료를 PUT에 보내면 `MasterServiceImpl.update()`가 expectedVersion을 필수로 요구하여 ERR_INVALID_INPUT을 던집니다. 같은 record를 신규 등록에도 사용하므로 expectedVersion에 공통 @NotNull을 붙이지 않은 것입니다. POST에서는 버전이 없어야 하고 PUT에서는 있어야 한다는 작업별 규칙을 Service가 구분합니다.

### 2.12 네 표시를 한 요청에서 함께 읽기

```json
{
  "values": { "COMPANY_NAME": "배움 회사" },
  "expectedVersion": 0
}
```

이 자료를 PUT companies/100에 보내면 Controller의 @RequestBody가 MasterRequest로 읽고 @Valid가 내부 검사표를 실행합니다. @NotNull은 values가 있으므로 통과, @Size는 짝이 1개이므로 통과, @Min은 0 이상이므로 통과합니다. 그 뒤 Controller 본문이 Service를 호출합니다.

**왜 한 종류로 전부 검사하지 않는지** 생각해 보세요. 값의 존재, 목록의 크기, 숫자의 하한은 서로 다른 질문입니다. 검증 도구가 각 표시의 뜻을 알고 있기 때문에 조건을 작게 나누어 붙일 수 있습니다. 업무상 필수 회사 이름, 허용 필드, 조직 소속, 버전 일치까지는 이어지는 Service와 DB가 검사합니다.

<a id="chapter-3"></a>
## 3. 프로그램이 켜질 때와 요청이 들어올 때

### 3.1 시작 버튼: ErpcomApplication.java의 main()

[ErpcomApplication.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/ErpcomApplication.java)의 `main()`이 `SpringApplication.run()`을 호출합니다. Spring Boot가 설정을 읽고, 웹 서버와 DB 연결을 준비하며, 필요한 클래스들을 찾아 연결합니다.

패키지는 파일들의 주소 체계입니다. 시작 클래스가 `com.erpcom.erpcom`에 있으므로 그 아래 `common.master`, `common.permission` 등의 구성 요소를 찾을 수 있습니다.

Spring이 만들어 함께 사용하는 객체를 **Bean**이라고 부릅니다. 생성자에 `MasterService service`를 적으면 Spring이 준비한 서비스를 건네줍니다. 이를 **의존성 주입**이라고 합니다. 쉬운 말로 하면 “필요한 담당자를 프로그램이 연결해 준다”는 뜻입니다.

예를 들어 `MasterController`의 생성자는 `MasterService`와 `RequestUserService`를 받습니다. `MasterServiceImpl`의 생성자는 `MasterMapper`를 받습니다. 매번 요청이 들어올 때 새 담당자 목록을 직접 만드는 코드는 필요하지 않습니다.

DB 연결이 준비되면 Flyway가 아직 적용하지 않은 migration SQL을 순서대로 적용합니다. 실제로 어떤 DB를 변경할지는 실행 시 설정한 접속 대상에 달려 있습니다. 설명서를 작성하는 작업에서는 실제 Oracle에 연결하거나 migration을 실행하지 않았습니다.

### 3.2 요청 한 건의 큰 그림

아래는 공통 관리자 API로 회사 등록을 요청했을 때의 길입니다. 단계 사이에 문제가 생기면 정상 경로를 멈추고 공통 오류 경로로 이동합니다.

```text
화면에서 POST /api/common/admin/companies 요청
  ↓
Spring MVC가 주소에 맞는 메서드를 찾음
  ↓
CommonWebConfiguration의 preHandle()
  ├─ RequestUserService.require() : 검증된 사용자 번호가 있는가?
  ├─ PermissionServiceImpl.requireButton() : 메뉴와 신규 버튼 권한이 있는가?
  └─ PermissionServiceImpl.requireScope() : 중앙 관리에 필요한 COMPANY 범위인가?
  ↓
MasterController.create()
  ↓
MasterServiceImpl.create()
  ├─ MasterResource.named() : companies는 허용된 관리 항목인가?
  ├─ normalize(), convert() : 필드 이름과 값이 올바른가?
  ├─ MasterMapper.nextId() : 새 번호를 받음
  └─ validate() : 공통 규칙과 연결 관계 검사
  ↓
MasterMapper.insert() → MasterMapper.xml의 insert → TB_COMPANY
  ↓
MasterServiceImpl.find() → MasterMapper.find()
  ↓
등록한 행을 JSON으로 응답, HTTP 201
```

### 3.3 요청은 먼저 사용자 번호를 얻는다

`RequestUserService.java`의 `require()`는 `CurrentUserProvider.userId()`를 호출합니다. 번호가 없거나 0 이하이면 `ApiException("ERR_AUTH_REQUIRED")`를 던집니다.

번호가 있더라도 끝이 아닙니다. `PermissionServiceImpl.user()`를 호출해 DB에서 사용자가 실제로 활성 상태인지 다시 확인합니다. 삭제되거나 정지된 사용자가 예전 토큰에 번호만 가지고 있다고 해서 허용하지 않기 위해서입니다.

`PermissionMapper.user()`는 다음을 확인합니다.

- 사용자 `USE_YN='Y'`, `USER_STATUS='ACTIVE'`.
- 사용자가 속한 회사도 `USE_YN='Y'`.
- 지정된 사업장과 부서가 있으면 그 행도 활성 상태.

현재 SQL은 사용자가 직접 연결한 부서를 확인합니다. 그 부서의 모든 상위 부서를 재귀적으로 확인하는 SQL은 아닙니다.

`X-User-Id: 1`처럼 요청 헤더에 번호를 써도 기본 구현은 그것을 신뢰하지 않습니다. `CurrentUserProvider`에 실제 인증 구현체가 연결되어 있어야 합니다.

### 3.4 직접 Service를 부를 때 주의할 점

`MasterServiceImpl.create(..., actor)`의 `actor`는 변경한 사람의 번호입니다. 서비스 내부에서 actor가 관리자인지 다시 검사하는 구조는 아닙니다. 현재 HTTP 관리 API의 권한 검사는 앞의 interceptor가 맡고 있습니다.

테스트나 향후 배치가 Service를 직접 호출하면 HTTP interceptor를 지나지 않습니다. 그런 호출자는 별도로 적절한 권한과 신뢰할 수 있는 actor를 확보해야 합니다. “Service 메서드를 호출하면 어떤 상황에서도 자동으로 관리자 권한을 검사한다”고 이해하면 안 됩니다.

<a id="chapter-4"></a>
## 4. 21개 테이블은 각각 무엇을 보관하는가

이 장은 현재 원본 SQL에 정의된 21개 테이블과 공통 관리 resource를 설명합니다.
단위·통화·금액 표시의 실제 계산과 basis 파일 연결은 17장에서 더 자세히 설명합니다.

### 4.1 모든 표에 붙어 있는 다섯 칸과 버전 번호

21개 일반 테이블은 공통 관리 칸을 가지고 있습니다. 통합 `V001`이 다섯 칸과 `VERSION_NO`를 처음부터 만듭니다.

| 컬럼 | 쉬운 뜻 | 누가 정하는가 |
|---|---|---|
| `USE_YN` | 지금 사용하는 자료인가? Y=사용, N=비활성 | 관리자 입력 가능, 신규 기본값 Y |
| `CREATED_AT` | 처음 만든 시간 | DB의 `SYSTIMESTAMP` 기본값 |
| `CREATED_BY` | 처음 만든 사람의 사용자 번호 | Controller가 확인한 actor |
| `UPDATED_AT` | 마지막으로 고친 시간 | 수정 SQL의 `SYSTIMESTAMP` |
| `UPDATED_BY` | 마지막으로 고친 사람의 번호 | 수정 시 확인한 actor |
| `VERSION_NO` | 몇 번째 상태인가? | 신규 0, 수정·비활성화 때 1 증가 |

`CREATED_BY`에는 로그인 아이디 문자열이 아니라 바뀌지 않는 `TB_USER.USER_ID`를 저장합니다. 초기 기준 데이터는 시스템 작업을 의미하는 `0`을 사용합니다. `0`은 일반 사용자가 인증된 번호로 쓸 수 있는 값은 아닙니다.

관리 API는 `CREATED_BY`, `UPDATED_BY`, `CREATED_AT`, `UPDATED_AT`, `VERSION_NO`를 임의 입력으로 받지 않습니다. `MasterResource.getProjection()`은 결과에 이 칸들을 포함하지만, `MasterServiceImpl.normalize()`의 입력 허용 목록에는 없습니다.

### 4.2 표와 API 주소의 대응

주소의 `{resource}`는 관리할 자료의 이름입니다. 사용자가 DB 테이블 이름을 직접 보내는 것이 아닙니다. `MasterResource.named()`가 아래 이름만 허용합니다.

| resource | DB 표 | 한 행을 찾는 키 | 쉬운 뜻 |
|---|---|---|---|
| `languages` | `TB_LANGUAGE` | `LANG_CODE` | 지원 언어 목록 |
| `messages` | `TB_MESSAGE` | `MESSAGE_CODE` | 메시지의 공통 정책 |
| `message-translations` | `TB_MESSAGE_LANG` | `MESSAGE_CODE~LANG_CODE` | 메시지별 번역문 |
| `companies` | `TB_COMPANY` | `COMPANY_ID` | 회사 |
| `sites` | `TB_SITE` | `SITE_ID` | 사업장 |
| `departments` | `TB_DEPT` | `DEPT_ID` | 부서 |
| `users` | `TB_USER` | `USER_ID` | 사용자 기본정보 |
| `roles` | `TB_ROLE` | `ROLE_ID` | 역할의 종류 |
| `user-roles` | `TB_USER_ROLE` | `USER_ROLE_ID` | 사용자에게 준 역할 |
| `menus` | `TB_MENU` | `MENU_ID` | 메뉴와 화면 안내 |
| `buttons` | `TB_BUTTON` | `BUTTON_ID` | 기능 종류 |
| `menu-buttons` | `TB_MENU_BUTTON` | `MENU_ID~BUTTON_ID` | 어느 화면에 어느 버튼을 놓는가 |
| `role-menu-permissions` | `TB_ROLE_MENU_AUTH` | `ROLE_MENU_AUTH_ID` | 역할별 메뉴 권한 |
| `user-menu-permissions` | `TB_USER_MENU_AUTH` | `USER_MENU_AUTH_ID` | 사용자별 메뉴 예외 |
| `role-button-permissions` | `TB_ROLE_BUTTON_AUTH` | `ROLE_ID~MENU_ID~BUTTON_ID` | 역할별 화면·버튼 권한 |
| `user-button-permissions` | `TB_USER_BUTTON_AUTH` | `USER_BUTTON_AUTH_ID` | 사용자별 화면·버튼 예외 |
| `code-groups` | `TB_COMMON_CODE_GROUP` | `CODE_GROUP` | 공통코드 분류 |
| `codes` | `TB_COMMON_CODE` | `CODE_GROUP~CODE` | 분류 안의 코드 |
| `units` | `TB_UNIT` | `UNIT_CODE` | 재고·자산 단위 |
| `currencies` | `TB_CURRENCY` | `CURRENCY_CODE` | 통화 기준정보 |
| `menu-amount-formats` | `TB_MENU_AMOUNT_FORMAT` | `MENU_ID~FIELD_CODE` | 메뉴·금액 항목의 표시 방법 |

예를 들어 `/api/common/admin/codes/USER_STATUS~ACTIVE`는 “사용자 상태 분류 안의 ACTIVE 코드”입니다. `/api/common/admin/menu-buttons/1010~3`은 “1010번 화면에 놓인 3번 버튼”입니다.

숫자 한 칸으로 찾는 11개 resource는 서버가 새 번호를 생성합니다. 언어·메시지·코드처럼 의미 있는 문자열이나 복합키를 쓰는 나머지 10개는 그 키를 등록 자료에 넣습니다. `MasterCatalog.java`의 각 `MasterResource` 정의에서 `identity`가 있으면 서버 번호 생성 대상입니다.

### 4.3 회사 → 사업장 → 부서 → 사용자

`TB_COMPANY`는 회사 목록입니다. `COMPANY_CODE`는 사람이 읽는 회사 코드, `COMPANY_NAME`은 회사 이름입니다. `COMPANY_ID`는 서버가 관리하는 고유 번호입니다.

`TB_SITE`는 사업장입니다. 본사나 공장처럼 같은 회사의 장소를 나눕니다. `COMPANY_ID`로 회사를 연결하고 `SITE_CODE`, `SITE_NAME`을 갖습니다.

`TB_DEPT`는 부서입니다. 반드시 회사에 속하고, `SITE_ID`는 선택할 수 있습니다. `PARENT_DEPT_ID`는 상위 부서 번호입니다. `DEPT_LEVEL`은 표시용 단계 값, `SORT_ORDER`는 정렬 순서입니다. 현재 코드는 부모의 단계에 따라 `DEPT_LEVEL`을 자동 계산하지 않습니다. 부모 관계와 숫자 단계는 별개로 입력·관리합니다.

`TB_USER`는 직원 또는 시스템 사용자입니다. `LOGIN_ID`, `USER_NAME`, `EMP_NO`, `EMAIL`, `COMPANY_ID`, 선택적인 `SITE_ID`·`DEPT_ID`, `LANG_CODE`, `USER_STATUS`를 공통 관리 API에서 다룹니다. 회사 연결은 필수이고 사업장·부서는 선택입니다. 회사명·부서명·사람 이름은 현재 원래 이름 값으로 저장하며, 자동 번역하는 기능은 없습니다.

사용자 상태의 초기 예는 `ACTIVE`, `LOCKED`, `DORMANT`, `RETIRED`입니다. 사용자를 저장할 때 `MasterServiceImpl.validate()`가 `USER_STATUS` 공통코드에서 그 상태가 활성 코드인지 확인합니다. 저장할 수 있는 상태가 늘어나더라도 현재 권한 조회에서 활동 가능한 상태는 SQL의 `ACTIVE`입니다. 새 상태를 추가했다고 자동으로 로그인 가능한 상태가 되는 것은 아닙니다.

조직 연결이 맞는지는 `MasterServiceImpl.validateOrganization()`과 DB 외래키가 함께 확인합니다. 예를 들어 A회사 직원에게 B회사 부서를 연결하려고 하면 막습니다.

### 4.4 ROLE과 사용자-ROLE

`TB_ROLE`은 “시스템 관리자”, “사업 관리자”, “조회 사용자”처럼 역할 종류를 저장합니다. `ROLE_CODE`는 프로그램에서 구분하는 코드입니다. 표시 이름은 `ROLE_NAME_MSG_CODE`로 메시지에 연결합니다. `ROLE_TYPE`은 현재 DB에서 `SYSTEM` 또는 `BUSINESS`를 허용합니다.

`TB_USER_ROLE`은 사용자와 역할을 연결합니다. 사용자 하나가 역할 여러 개를 가질 수 있고, 역할 하나도 여러 사용자에게 줄 수 있습니다. 연결 행에는 `VALID_FROM`, `VALID_TO`가 있어 기간을 정할 수 있습니다. 종료가 `null`이면 끝 날짜가 정해지지 않은 것입니다.

“조회 역할을 내일부터 준다”면 오늘은 사용할 수 없습니다. `PermissionMapper.xml`의 `activeRoles`가 현재 DB 시간 `SYSDATE`로 기간을 검사합니다. `USE_YN='N'`이거나 역할 자체가 비활성이어도 제외합니다.

기간 시작과 끝은 포함됩니다. 두 기간의 끝과 시작이 정확히 같은 시각이면 현재 구현에서는 겹치는 것으로 판정합니다. 겹침 저장을 막는 담당자는 `MasterServiceImpl.validatePeriod()`입니다.

### 4.5 메뉴, 버튼, 메뉴-버튼

`TB_MENU`는 화면 안내판입니다. `MENU_CODE`로 메뉴를 구분하고, `MENU_NAME_MSG_CODE`로 번역 이름을 찾습니다. `PARENT_MENU_ID`는 상위 메뉴입니다. `SCREEN_ID`, `FORM_PATH`, `ICON_NAME`은 화면 프로그램이 참고할 정보입니다.

`MENU_TYPE`은 현재 DB에서 `GROUP`·`SCREEN`·`POPUP`을 허용합니다. `GROUP`은 메뉴 묶음, `SCREEN`은 화면, `POPUP`은 팝업 성격의 메뉴입니다. 현재 서비스는 유형에 따라 화면을 실행하지 않습니다. 정보만 제공합니다. `MENU_LEVEL`도 부모에 따라 자동 계산하지 않습니다.

`TB_BUTTON`은 조회·신규·저장·삭제·엑셀 같은 기능 사전입니다. `BUTTON_CODE`와 `BUTTON_NAME_MSG_CODE`, `BUTTON_TYPE`을 갖습니다. 버튼이라고 부르지만 실제 화면의 버튼만 의미하지 않고, 서버에서 실행하는 기능의 식별자로도 사용할 수 있습니다.

`TB_MENU_BUTTON`은 “이 메뉴에서 이 버튼을 사용한다”는 배치표입니다. 저장 버튼은 여러 화면에 놓일 수 있으므로 메뉴와 버튼을 짝으로 저장합니다. `SORT_ORDER`는 그 화면에서 버튼을 어떤 순서로 표시할지 안내합니다.

메뉴나 버튼의 `SENSITIVE_YN='Y'`는 민감 기능이라는 정보입니다. 현재 코드가 이 값만 보고 DENY 우선 계산이나 추가 인증을 자동으로 수행하지는 않습니다. 그런 정책은 후속 구현이 필요합니다.

### 4.6 메뉴 권한 두 표

`TB_ROLE_MENU_AUTH`는 역할에게 주는 기본 메뉴 권한입니다. 예를 들어 “조회 역할은 프로젝트 화면을 READ_ALL로 읽고 범위는 OWN”처럼 저장합니다. `ROLE_ID`, `MENU_ID`, `ACCESS_LEVEL`, `DATA_SCOPE`가 핵심입니다.

`TB_USER_MENU_AUTH`는 사용자 개인의 예외입니다. 같은 역할이어도 특정 직원만 범위를 넓히거나 메뉴를 금지할 수 있습니다. `ACCESS_LEVEL` 또는 `DATA_SCOPE`가 `null`이면 그 항목은 역할에서 물려받습니다. `ACCESS_LEVEL='NONE'`은 메뉴 금지입니다. 개인 예외에도 유효기간이 있습니다.

이 표들을 저장하는 곳은 공통 `MasterServiceImpl`이고, 읽어 최종 결정을 계산하는 곳은 `PermissionServiceImpl.get()`입니다. **자료를 저장하는 코드와 권한을 판정하는 코드가 다릅니다.**

### 4.7 버튼 권한 두 표

`TB_ROLE_BUTTON_AUTH`는 역할 + 메뉴 + 버튼의 세 칸으로 권한을 정합니다. “사업 관리자에게 프로젝트 화면의 저장 버튼을 허용한다”는 식입니다. 역할 + 버튼만으로 정하지 않는 이유는 같은 저장 버튼이라도 화면마다 허용 여부가 달라지기 때문입니다.

`TB_USER_BUTTON_AUTH`는 사용자 개인의 예외입니다. `ACCESS_YN='Y'`면 허용을 추가하고, `N`이면 허용을 제거하며, `null`이면 역할 결과를 유지합니다. 이것도 유효기간을 갖습니다.

버튼만 허용하고 메뉴를 금지하면 실행할 수 없습니다. `PermissionServiceImpl.requireButton()`이 먼저 `requireMenu()`를 호출하기 때문입니다.

### 4.8 언어, 메시지, 번역

`TB_LANGUAGE`는 지원 언어를 저장합니다. `LANG_CODE`는 `ko`, `en`, `ja`, `zh-CN`, `zh-TW` 같은 코드입니다. `LANG_NAME`은 언어 이름, `NATIVE_NAME`은 해당 언어의 이름 표기입니다. `DEFAULT_YN`은 기본 언어인지, `FALLBACK_LANG_CODE`는 번역이 없을 때 어느 언어로 넘어갈지 정합니다.

`TB_MESSAGE`는 메시지의 이름표와 정책입니다. 예를 들어 `ERR_ACCESS_DENIED`라는 코드, `ERROR`라는 종류, HTTP 403 상태, `WARN` 중요도, 들어갈 인자 개수 `ARG_COUNT`를 갖습니다. 한국어 문장 자체는 이 표에 넣지 않습니다.

`TB_MESSAGE_LANG`은 실제 번역문입니다. 같은 `ERR_ACCESS_DENIED`라도 `ko` 행과 `en` 행에 서로 다른 문장을 저장합니다. `TRANSLATED_YN='Y'`이면서 `USE_YN='Y'`인 번역만 메시지 조회 SQL이 사용합니다.

이렇게 하면 `MENU_NAME_KO`, `MENU_NAME_EN`처럼 언어마다 새 컬럼을 만들지 않아도 됩니다. 언어가 늘면 번역 행을 추가합니다. 실제 찾는 과정은 8장에서 `MessageServiceImpl.get()`을 따라가며 설명합니다.

### 4.9 공통코드 분류와 코드

`TB_COMMON_CODE_GROUP`은 코드의 분류입니다. `USER_STATUS`는 “사용자 상태 종류”라는 분류입니다. `DESCRIPTION`은 관리자용 설명입니다.

`TB_COMMON_CODE`는 그 분류 안의 값입니다. `ACTIVE`는 코드, `COD_USER_STATUS_ACTIVE`는 표시 이름을 찾을 메시지 코드입니다. 영어로 보여 줄 때 `Active`, 한국어로 보여 줄 때 `사용`을 찾아 줄 수 있습니다. `EXT_VALUE1`·`2`·`3`은 확장 정보를 담는 선택 칸이고, 현재 코드에 별도 의미 계산은 없습니다.

새 단위·통화 설계에서는 이 확장 문자열 칸에 환산계수나 통화의 계산 규칙을 숨겨 넣지 않습니다.
단위 종류처럼 단순한 분류는 공통코드를 재사용하고, 실제 단위·통화 기준값은 전용 테이블에서 관리합니다.
왜 나누는지는 17.2와 17.5의 실제 예를 보세요.

운영 중 늘어나는 상태 값을 Java enum에만 고정하지 않고 DB에 저장한 이유입니다. 반면 권한 계산의 `AccessLevel`과 `DataScope`는 현재 Java의 검사 알고리즘과 직접 연결된 고정 규칙이므로 enum입니다.

### 4.10 각 자료를 새로 만들 때 어느 칸을 보내는가

아래 표는 `MasterCatalog`의 현재 정의를 사람이 읽을 수 있게 정리한 것입니다. **필수 입력**은 기본값이 없으므로 새로 만들 때 반드시 보낼 칸입니다. **자동 기본값**은 생략하면 채워 주는 칸이며, 필요한 경우 허용 범위에서 다른 값을 지정할 수 있습니다. **선택 입력**은 생략하면 null인 칸입니다.

모든 자료에는 별도로 `USE_YN=Y` 기본값이 붙습니다. 감사 칸·VERSION_NO는 보내지 않고, 숫자 기본키도 서버가 발급합니다. 문자열·복합 기본키는 필수 입력에 포함합니다.

| resource | 필수 입력 | 자동 기본값 | 선택 입력 |
|---|---|---|---|
| languages | LANG_CODE, LANG_NAME | DEFAULT_YN=N, SORT_ORDER=0 | NATIVE_NAME, FALLBACK_LANG_CODE |
| messages | MESSAGE_CODE, MESSAGE_TYPE | ARG_COUNT=0 | HTTP_STATUS, SEVERITY |
| message-translations | MESSAGE_CODE, LANG_CODE, MESSAGE_TEXT | TRANSLATED_YN=Y | 없음 |
| companies | COMPANY_CODE, COMPANY_NAME | 없음 | 없음 |
| sites | COMPANY_ID, SITE_CODE, SITE_NAME | 없음 | 없음 |
| departments | COMPANY_ID, DEPT_CODE, DEPT_NAME | DEPT_LEVEL=1, SORT_ORDER=0 | SITE_ID, PARENT_DEPT_ID |
| users | LOGIN_ID, USER_NAME, COMPANY_ID | USER_STATUS=ACTIVE | EMP_NO, SITE_ID, DEPT_ID, EMAIL, LANG_CODE |
| roles | ROLE_CODE, ROLE_NAME_MSG_CODE | ROLE_TYPE=BUSINESS | DESCRIPTION |
| user-roles | USER_ID, ROLE_ID | VALID_FROM=DB 현재 시각 | VALID_TO, REASON |
| menus | MENU_CODE, MENU_NAME_MSG_CODE | MENU_LEVEL=1, MENU_TYPE=SCREEN, SORT_ORDER=0, SENSITIVE_YN=N | PARENT_MENU_ID, SCREEN_ID, FORM_PATH, ICON_NAME |
| buttons | BUTTON_CODE, BUTTON_NAME_MSG_CODE | SENSITIVE_YN=N | BUTTON_TYPE |
| menu-buttons | MENU_ID, BUTTON_ID | SORT_ORDER=0 | 없음 |
| role-menu-permissions | ROLE_ID, MENU_ID | ACCESS_LEVEL=NONE, DATA_SCOPE=OWN | 없음 |
| user-menu-permissions | USER_ID, MENU_ID | VALID_FROM=DB 현재 시각 | ACCESS_LEVEL, DATA_SCOPE, VALID_TO, REASON |
| role-button-permissions | ROLE_ID, MENU_ID, BUTTON_ID | ACCESS_YN=Y | 없음 |
| user-button-permissions | USER_ID, MENU_ID, BUTTON_ID | VALID_FROM=DB 현재 시각 | ACCESS_YN, VALID_TO, REASON |
| code-groups | CODE_GROUP | 없음 | DESCRIPTION |
| codes | CODE_GROUP, CODE, CODE_NAME_MSG_CODE | SORT_ORDER=0 | EXT_VALUE1, EXT_VALUE2, EXT_VALUE3 |
| units | UNIT_CODE, UNIT_NAME_MSG_CODE, UNIT_TYPE_CODE | QUANTITY_SCALE=0, SORT_ORDER=0 | BASE_UNIT_CODE, TO_BASE_FACTOR |
| currencies | CURRENCY_CODE, CURRENCY_NAME_MSG_CODE, MINOR_UNIT_DIGITS | SORT_ORDER=0 | NUMERIC_CODE |
| menu-amount-formats | MENU_ID, FIELD_CODE, UNIT_LABEL_MSG_CODE | DISPLAY_DIVISOR=1, DECIMAL_PLACES=0, ROUNDING_MODE=HALF_UP | CURRENCY_CODE |

표의 “DB 현재 시각”은 `MasterMapper.now()`의 SYSDATE입니다. 나중에 기간을 지정하려면 `2030-01-02T09:00:00`처럼 보냅니다. 종료 null은 끝이 정해지지 않은 기간입니다.

role-menu-permissions는 기본 ACCESS_LEVEL이 NONE입니다. 역할과 메뉴 번호만 등록했다고 허용이 생기지 않습니다. 실제로 허용하려면 필요한 수준을 명시해야 합니다. user-menu-permissions의 기본 수준은 null이고, user-button-permissions의 기본 ACCESS_YN도 null이므로 개인 예외를 새로 등록하면서 아무 허용도 명시하지 않으면 기존 역할 값을 유지합니다.

이 표는 신규 입력 표입니다. 수정에서 필수 칸을 전부 다시 보낼 필요는 없습니다. 보낸 칸은 검사하고, 안 보낸 칸은 기존 값을 유지한 뒤 전체 자료의 정합성을 검사합니다.

### 4.11 이름표의 번호와 사람이 보는 코드는 다르다

`COMPANY_ID=100`은 연결을 위한 고유 번호이고 `COMPANY_CODE=C100`은 사람이 정한 식별 코드입니다. `ROLE_ID=10`과 `ROLE_CODE=SYSTEM_ADMIN`, `MENU_ID=9000`과 `MENU_CODE=COMMON_ADMIN`도 같은 관계입니다.

API 주소의 한 행 조회는 위 표의 PK를 쓰고, 권한 검사 annotation은 메뉴·버튼 코드를 씁니다. `PermissionMapper.menuId()`와 `buttonId()`가 코드에서 번호를 찾기 때문에 두 종류가 연결됩니다. 메뉴 표시 이름은 또 다른 `MENU_NAME_MSG_CODE`를 통해 번역합니다.

따라서 한 메뉴에는 “연결 번호”, “기능 식별 코드”, “번역 메시지 코드”, “번역된 표시 이름”이 있을 수 있습니다. 비슷해 보여도 역할이 다릅니다. 숫자를 이름처럼 화면에 표시하거나 번역 문장으로 PK를 찾으면 안 됩니다.

<a id="chapter-5"></a>
## 5. 회사 등록·조회·수정·비활성화를 끝까지 따라가기

### 5.1 왜 CompanyController.java가 없는가

회사·부서·언어 등 21개 기준정보는 기본적인 등록·조회·수정·비활성화 과정이 비슷합니다. 그래서 각 표마다 비슷한 Controller와 Service를 복사하지 않고 다음 파일들이 함께 처리합니다.

- `MasterCatalog.java`: 관리 가능한 자료와 필드의 고정 목록.
- `MasterResource.java`: 특정 자료의 테이블·키·입력 필드 정보를 담는 상자.
- `MasterController.java`: 공통 관리 API 창구.
- `MasterService.java`: 관리 작업의 약속.
- `MasterServiceImpl.java`: 검사와 처리의 실제 구현.
- `MasterMapper.java` + `MasterMapper.xml`: 실제 DB 작업.

회사 관리 설명에서 `CompanyService`를 찾을 필요는 없습니다. 그런 별도 파일은 현재 없습니다. `companies`라는 resource 값을 따라가면 됩니다.

### 5.2 관리 API에 들어가는 조건

`MasterController`의 클래스 위에는 `@RequiresPermission(scopes = DataScope.COMPANY)`가 있습니다. 별도 값을 쓰지 않은 항목은 `RequiresPermission.java`의 기본값을 사용합니다.

| 작업 | Controller 메서드 | 메뉴 | 버튼 | 권한 수준 | 범위 |
|---|---|---|---|---|---|
| 관리 대상 목록 | `resources()` | COMMON_ADMIN | BTN_SEARCH | READ_ALL | COMPANY |
| 목록 조회 | `list()` | COMMON_ADMIN | BTN_SEARCH | READ_ALL | COMPANY |
| 한 행 조회 | `get()` | COMMON_ADMIN | BTN_SEARCH | READ_ALL | COMPANY |
| 새 행 등록 | `create()` | COMMON_ADMIN | BTN_NEW | WRITE_ALL | COMPANY |
| 기존 행 수정 | `update()` | COMMON_ADMIN | BTN_SAVE | WRITE_ALL | COMPANY |
| 비활성화 | `deactivate()` | COMMON_ADMIN | BTN_DELETE | WRITE_ALL | COMPANY |

여기의 COMPANY는 중앙 공통 관리 창구에 들어가는 조건입니다. 현재 관리 API는 회사별 자동 필터가 없는 중앙 관리 API이므로 여러 회사의 기준정보를 관리할 수 있습니다. 일반 업무 자료의 COMPANY 범위를 검사하는 `requireData()`는 사용자와 같은 회사만 허용합니다. 둘의 용도를 섞지 마세요.

### 5.3 회사 등록 자료의 모양

권한이 있는 인증 사용자가 다음처럼 요청한다고 가정합니다.

```http
POST /api/common/admin/companies
Content-Type: application/json
```

```json
{
  "values": {
    "COMPANY_CODE": "EDU01",
    "COMPANY_NAME": "배움 회사"
  }
}
```

`values`는 저장할 칸과 값입니다. `COMPANY_ID`, 감사 컬럼, `VERSION_NO`는 보내지 않습니다. 생성할 때 `expectedVersion`은 없어야 하거나 `null`이어야 합니다. 기존 버전과 비교하는 것은 수정·비활성화 때 필요합니다.

<a id="request-binding-examples"></a>
### 5.4 POST companies의 JSON이 왜 MasterRequest로 들어가는가

먼저 표현을 정확히 고칩니다. **MasterRequest는 이 관리 API의 JSON 본문을 담는 입력 객체입니다. 전체 프로그램이나 HTTP 처리의 맨 첫 단계라는 뜻은 아닙니다.** 요청을 받고 주소를 찾고 preHandle()에서 권한을 확인한 뒤, Controller 메서드의 인자를 준비하는 단계에서 만들어집니다.

또한 `companies`라는 주소를 보았기 때문에 Spring이 스스로 MasterRequest라는 이름을 골랐다는 뜻도 아닙니다. 개발자가 **Controller 메서드의 매개변수 타입을 MasterRequest로 선언했기 때문에** 그 타입으로 읽습니다.

#### 5.4.1 실제 create() 선언을 먼저 보기

현재 MasterController의 관련 부분은 다음과 같습니다.

```java
@RequestMapping("/api/common/admin")
public class MasterController {
    // 다른 코드 생략: 주소와 입력 선언을 설명하는 발췌

    @PostMapping("/{resource}")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(button = "BTN_NEW", level = AccessLevel.WRITE_ALL)
    public Map<String, Object> create(
            @PathVariable String resource,
            @Valid @RequestBody MasterRequest body,
            HttpServletRequest request) {
        return service.create(resource, body, users.require(request).userId());
    }
}
```

위는 해당 부분을 읽기 쉽게 발췌한 것으로 클래스 권한 표시와 생성자 등은 생략했습니다. 각 조각은 다음 뜻입니다.

| 선언 | 실제 요청에서 들어오는 값 | 쉬운 뜻 |
|---|---|---|
| 클래스의 `/api/common/admin` | URL의 앞부분 | 공통 관리자 창구의 주소 |
| 메서드의 `/{resource}` | URL의 끝부분 companies | 관리할 자료 이름을 주소에서 받음 |
| `@PostMapping` | HTTP POST | 신규 등록 요청이면 이 메서드를 고름 |
| `@PathVariable String resource` | `"companies"` | 주소의 자리 이름을 문자열 변수로 받음 |
| `@RequestBody MasterRequest body` | JSON 본문 전체 | 지정한 MasterRequest 타입으로 본문을 읽음 |
| `@Valid` | body의 검사표 | 읽은 객체의 조건을 검사 |
| `HttpServletRequest request` | 서버가 가진 요청 객체 | 헤더·경로·인증 연결에 필요한 현재 요청 정보 |
| `Map<String,Object>` 반환 타입 | 저장된 행 | 입력 타입이 아니라 응답 자료의 타입 |

입력 타입 MasterRequest와 반환 타입 Map은 용도가 다릅니다. 반환 타입을 보고 입력도 그냥 Map이라고 생각하면 안 됩니다.

#### 5.4.2 화면에서 보낸 글자와 Java 객체는 다르다

화면은 HTTP 본문에 아래 JSON 글자를 보냅니다.

```http
POST /api/common/admin/companies
Content-Type: application/json
```

```json
{
  "values": {
    "COMPANY_CODE": "REQ01",
    "COMPANY_NAME": "배움 회사"
  }
}
```

서버가 받은 순간에는 회사 row 객체나 MasterResource가 이미 완성되어 있는 것이 아닙니다. JSON 본문은 글자를 담은 요청 데이터입니다. Controller가 Java 객체로 작업하려면 JSON을 읽어 타입과 칸에 맞게 옮겨야 합니다. 이를 **역직렬화**라고 합니다. 쉬운 말로 “전송한 글자를 Java의 자료 상자로 바꾸는 작업”입니다.

Spring MVC의 요청 처리 담당인 DispatcherServlet이 맞는 메서드를 찾습니다. preHandle()이 통과하면 메서드를 실행할 도구인 HandlerAdapter가 매개변수들을 준비합니다. @RequestBody를 처리하는 Spring의 인자 해석기가 Content-Type과 목표 타입을 보고 JSON 변환 도구를 사용합니다. 현재 프로젝트의 JSON 변환은 Jackson을 사용합니다.

각 도구 이름을 외울 필요는 없습니다. “주소 담당이 메서드를 찾는다 → 사전 권한 검사 → 매개변수 담당이 JSON을 선언된 타입으로 읽는다 → 입력 검증 → 메서드 본문” 순서를 기억하면 됩니다.

#### 5.4.3 JSON 이름과 record의 항목이 이렇게 맞는다

```java
public record MasterRequest(
        @NotNull @Size(min = 1, max = 40) Map<String, Object> values,
        @Min(0) Long expectedVersion) {}
```

JSON 바깥에는 values와 선택적인 expectedVersion이 있고, record에도 같은 이름의 두 항목이 있습니다.

```text
JSON 바깥 values
  → MasterRequest의 values Map
      COMPANY_CODE → "REQ01"
      COMPANY_NAME → "배움 회사"

JSON 바깥 expectedVersion이 생략됨
  → MasterRequest의 expectedVersion = null
```

Jackson이 만드는 결과를 Java로 표현하면 아래와 같은 모양입니다. **설명용으로 결과를 표현한 코드이지, 실제 Controller에 개발자가 이 생성 코드를 넣었다는 뜻은 아닙니다.**

```java
MasterRequest body = new MasterRequest(
        Map.of("COMPANY_CODE", "REQ01", "COMPANY_NAME", "배움 회사"),
        null);
```

이제 `body.values().get("COMPANY_NAME")`을 읽으면 배움 회사이고, `body.expectedVersion()`을 읽으면 null입니다. record의 생성자와 값 꺼내기 메서드는 Java가 제공합니다.

수정 JSON에 `"expectedVersion": 3`이 있으면 Long 항목에 3L이 들어갑니다. @Min은 그 값이 음수인지 검사합니다. 입력 값이 목표 타입으로 읽을 수 없는 모양이면 JSON 변환 과정에서 실패할 수도 있습니다. 그 경우에는 내부 조건 검사를 하기 전에 오류 처리로 이동합니다.

#### 5.4.4 왜 테이블별 CompanyRequest를 쓰지 않았는가

현재 21개 resource가 같은 create()/update() 창구를 공유합니다. 회사는 COMPANY_CODE·COMPANY_NAME, 사업장은 COMPANY_ID·SITE_CODE·SITE_NAME, 사용자-ROLE은 USER_ID·ROLE_ID를 보냅니다. 안쪽 필드가 서로 다릅니다.

바깥 입력은 공통 규격으로 두고, 안쪽 값을 Map으로 받은 이유입니다.

```text
같은 MasterRequest 바깥 상자
  ├─ values          : 자료 종류마다 다른 편집 값
  └─ expectedVersion : 수정 충돌 확인 번호
```

`resource`와 body를 함께 `MasterServiceImpl.create()`에 전달하면, Service가 `MasterResource.named(resource)`로 자료 종류에 맞는 허용 목록을 찾습니다. **MasterRequest는 값을 담고, MasterCatalog는 그 값의 검사 규칙을 담습니다.** 둘은 같은 종류의 상자가 아닙니다.

이 공통 Map 방식은 모든 항목을 위한 별도 요청 record를 만들지 않아도 되지만, values 안의 필드 규칙을 직접 검사해야 하는 비용도 있습니다. 그래서 normalize()와 convert()가 필요한 것입니다.

#### 5.4.5 MasterCatalog가 입력 본문을 받는 것이 아닌 이유

회사 JSON은 “실제 저장할 값”이고 MasterCatalog의 회사 정의는 “어떤 값과 칸을 허용할지 적은 규칙”입니다.

| 대상 | 현재 자료 | 목적 |
|---|---|---|
| body.values | COMPANY_CODE=REQ01, COMPANY_NAME=배움 회사 | 이번 요청의 저장 값 |
| Catalog의 companies | 코드 TEXT 20 필수, 이름 TEXT 100 필수 | 모든 회사 등록·수정에 사용하는 규칙 |
| DB의 TB_COMPANY 행 | 생성된 COMPANY_ID와 저장 값·감사 정보 | 오래 보관한 결과 |

Catalog에는 “배움 회사”라는 이번 회사 이름이 저장되지 않습니다. 회사 이름이 달라져도 Catalog의 허용 목록 자체를 바꿀 필요는 없습니다.

#### 5.4.6 실제 실행 순서를 번호로 다시 보기

1. POST `/api/common/admin/companies`와 JSON 본문이 서버에 도착합니다.
2. Spring이 POST와 URL에 맞는 `MasterController.create()`를 찾습니다. 아직 본문은 실행하지 않았습니다.
3. `CommonWebConfiguration` 안의 `preHandle()`이 해당 메서드의 권한 표시를 읽고 사용자·메뉴·버튼·COMPANY 범위를 확인합니다.
4. 통과하면 Spring이 create()의 매개변수를 준비합니다. resource는 companies, body의 목표 타입은 MasterRequest, request는 현재 요청 객체입니다.
5. @RequestBody에 따라 JSON을 MasterRequest로 읽고, @Valid에 따라 그 객체의 @NotNull·@Size·@Min을 검사합니다.
6. 입력 변환·검사가 성공해야 `MasterController.create()`의 중괄호 안 본문을 실행합니다.
7. Controller가 `MasterServiceImpl.create("companies", body, actor)`를 호출합니다.
8. Service가 이제 `MasterResource.named("companies")`로 Catalog 규칙을 찾고 normalize()/convert()/validate()를 수행합니다.
9. DB 저장과 결과 조회를 거쳐 응답합니다.

그러므로 “회사 주소가 들어오면 첫 Java 처리가 무조건 MasterRequest다”라는 설명은 틀립니다. 정확한 의미는 **권한 검사를 통과한 이 Controller에서, JSON 본문을 담아 받도록 선언한 첫 입력 DTO가 MasterRequest다**입니다.

#### 5.4.7 모양이 달라지면 어디서 실패하는가

**예 1 — values 바깥 상자를 빼고 칸만 보냈다**

```json
{ "COMPANY_CODE": "REQ02", "COMPANY_NAME": "상자 없는 회사" }
```

이것은 이 API의 MasterRequest 규격이 아닙니다. `values` 항목으로 들어갈 내용이 없습니다. JSON 변환 설정에 따라 알 수 없는 항목을 읽는 단계에서 거절되거나 values가 null이 된 뒤 @NotNull에서 거절됩니다. 어느 경우든 정상 회사 저장으로 이어지는 모양이 아닙니다.

**예 2 — 바깥 모양은 맞지만 회사 이름이 빠졌다**

```json
{ "values": { "COMPANY_CODE": "REQ03" } }
```

MasterRequest로 읽을 수 있고 values가 있으며 항목 수는 1이고 버전은 null이라 기본 어노테이션 검사를 통과합니다. 하지만 `MasterServiceImpl.normalize()`가 회사 필수 필드를 채우려 할 때 COMPANY_NAME의 기본값은 null이고 required=true입니다. `convert()`가 ERR_INVALID_INPUT을 던집니다.

**예 3 — JSON 문법 자체가 깨졌다**

본문이 `{ "values":`에서 끝났다면 JSON을 완성할 수 없으므로 MasterRequest 변환 단계가 실패합니다. @Valid를 실행할 완성된 body도 만들지 못했습니다. ApiExceptionHandler.validation()이 입력 오류로 처리합니다.

이 세 경우는 “입력이 나쁘다”는 결과가 비슷하지만 실패한 위치가 다릅니다. 주소 선택, 인증·권한, JSON 읽기, 객체 기본 검사, 업무 필드 검사, DB 저장을 나누어 이해해야 원인을 찾을 수 있습니다.

### 5.5 MasterController.create()는 전달 역할

`MasterController.create()`는 `users.require(request).userId()`로 변경한 사람을 얻고, `service.create(resource, body, actor)`를 부릅니다. Controller에서 SQL이나 `MasterMapper`를 직접 부르지 않습니다.

권한 검사는 앞의 interceptor가 했고, Controller는 입력과 현재 사용자 정보를 묶어 서비스에 전달합니다. 정상 결과의 HTTP 상태는 `@ResponseStatus(HttpStatus.CREATED)` 때문에 201입니다.

### 5.6 MasterServiceImpl.create()의 순서

`create()` 본문은 다음 순서입니다.

1. `MasterResource.named("companies")`로 허용된 정의를 찾습니다. 이름이 없으면 `ERR_NOT_FOUND`입니다.
2. 생성 요청에 `expectedVersion`이 들어 있으면 `invalid()`로 거절합니다.
3. `normalize(definition, values, true)`로 허용 필드만 받고 타입을 맞춥니다. 새 자료에서는 누락한 필드의 기본값도 채웁니다.
4. 숫자 키를 쓰는 회사이므로 `MasterMapper.nextId()`로 번호를 받아 `COMPANY_ID`에 넣습니다.
5. `validate()`로 저장 규칙을 확인합니다.
6. `MasterMapper.insert()`로 DB에 넣습니다.
7. 저장된 키를 모아 `find()`로 다시 읽고 결과를 돌려줍니다.

`normalize()`가 `USE_YN='Y'`를 채워 줍니다. 생성 시간과 버전 번호는 DB 기본값이 채웁니다. `CREATED_BY`는 `MasterMapper.insert()`에 전달한 actor입니다.

`nextId()`는 `SQ_COMMON_ID.NEXTVAL`을 읽습니다. migration에서 시작 번호는 1,000,000이지만, 현재 DB에서 다음에 받을 번호는 사용 상태에 따라 달라집니다. 번호가 연속이라고 가정하면 안 됩니다. 실패한 등록이나 DB의 sequence 캐시 때문에 중간 번호가 비어 있을 수 있습니다.

### 5.7 MasterMapper.java와 XML이 연결되는 방법

`MasterMapper.java`는 `insert()`라는 메서드의 모양만 선언합니다. `MasterMapper.xml`의 namespace는 그 인터페이스의 전체 이름입니다. XML의 `<insert id="insert">`가 Java의 `insert()`와 짝이 됩니다.

MyBatis가 그 연결을 만들어 주기 때문에 `MasterMapperImpl.java`를 직접 만들지 않습니다. 이것은 Service 인터페이스에 직접 만든 `MasterServiceImpl.java`가 있는 것과 다릅니다.

SQL은 개념적으로 다음 일을 합니다.

```sql
INSERT INTO TB_COMPANY
  (COMPANY_CODE, COMPANY_NAME, USE_YN, COMPANY_ID, CREATED_BY)
VALUES
  (요청 코드, 요청 이름, 'Y', 서버가 받은 번호, 확인한 사용자 번호);
```

위 SQL은 이해를 위한 표현입니다. 실제 XML은 고정 허용 목록에서 컬럼을 조합하고 값은 바인딩합니다. DB에 저장된 행을 다시 읽기 때문에 응답에는 생성 시간·버전 등 DB가 채운 결과도 포함됩니다.

### 5.8 회사 목록 조회

```http
GET /api/common/admin/companies?offset=0&limit=50&includeInactive=false
```

`MasterController.list()` → `MasterServiceImpl.list()` → `MasterMapper.list()`와 `count()` 순서입니다.

`offset`은 앞에서 건너뛸 행 수, `limit`은 이번에 가져올 최대 행 수입니다. 서비스는 offset이 0 이상이고 limit이 1~200인지 확인합니다. 기본값은 0과 50입니다.

`includeInactive=false`이면 `USE_YN='Y'`인 행만 나옵니다. `true`면 비활성도 포함합니다. 현재 공통 목록 API에는 이름 검색이나 임의 정렬 조건이 없습니다. 기본키 순서로 정렬합니다.

`MasterPage.java`는 결과를 네 칸으로 묶습니다.

```json
{
  "items": [],
  "total": 0,
  "offset": 0,
  "limit": 50
}
```

빈 표를 예로 든 모양입니다. 실제로 행이 있으면 `items` 안에 각 행의 JSON이 들어갑니다. `total`은 현재 활성/전체 조건을 만족하는 전체 행 수입니다. 이번 페이지의 길이와 같지 않을 수 있습니다.

### 5.9 회사 한 행 조회

```http
GET /api/common/admin/companies/100
```

`MasterController.get()` → `MasterServiceImpl.get()` → `keys()` → `find()` → `MasterMapper.find()` 순서입니다. `keys()`는 URL 문자열 `100`을 회사 번호로 바꿉니다. `find()`는 없는 행이면 `ERR_NOT_FOUND`를 던집니다.

한 행 조회의 SQL에는 `USE_YN='Y'` 조건이 없습니다. 비활성 행도 관리자가 조회할 수 있습니다. 목록 조회의 기본 조건과 구분해야 합니다.

### 5.10 수정: 읽었던 버전을 함께 보낸다

행을 조회했을 때 `VERSION_NO=0`이었다면 다음처럼 보냅니다. 주소의 `1000000`은 예시 번호입니다. 실제 생성 응답에서 받은 번호를 사용해야 합니다.

```http
PUT /api/common/admin/companies/1000000
Content-Type: application/json
```

```json
{
  "values": {
    "COMPANY_NAME": "새 이름의 배움 회사"
  },
  "expectedVersion": 0
}
```

`MasterServiceImpl.update()`는 먼저 `expectedVersion`이 있는지 확인합니다. `normalize(..., false)`는 이번에 보낸 항목만 처리합니다. 회사 코드를 안 보냈다고 지우지 않습니다.

서비스는 기존 행을 읽고 새 값을 합칩니다. 이것이 `merged`입니다. 예를 들어 기존 부서의 회사 번호는 그대로 두고 사업장만 바꾸면, **합친 전체 자료**로 조직 관계를 다시 검사해야 하기 때문입니다.

기본키를 `values`에 넣어 바꾸려 하면 거절합니다. 수정 SQL은 기본키와 `VERSION_NO=expectedVersion`이 모두 맞는 행만 고치고 버전을 1 증가시킵니다.

### 5.11 버전 번호가 두 사람의 수정을 지켜준다

민수와 지수가 같은 행의 버전 0을 읽었다고 합시다.

| 순서 | 행동 | DB의 상태 |
|---|---|---|
| 1 | 민수가 버전 0으로 이름 수정 | 성공, 버전 1 |
| 2 | 지수가 옛 화면의 버전 0으로 코드 수정 | 버전 불일치로 실패 |
| 3 | 지수가 다시 조회 | 민수의 수정 내용과 버전 1을 받음 |
| 4 | 지수가 버전 1로 다시 수정 | 성공, 버전 2 |

실패는 `ERR_CONFLICT`입니다. `MasterMapper.update()`가 변경한 행 수를 돌려주고, `MasterServiceImpl.update()`는 그 수가 1인지 확인합니다. 이런 방식을 **낙관적 잠금**이라고 합니다. 화면을 보는 동안 DB 전체를 잠그지 않고, 저장할 때 버전을 비교합니다.

### 5.12 비활성화와 다시 사용하기

```http
DELETE /api/common/admin/companies/1000000?expectedVersion=1
```

`MasterController.deactivate()` → `MasterServiceImpl.deactivate()` → `MasterMapper.deactivate()`입니다. SQL은 `USE_YN='N'`, 수정 시간·수정자, 버전 증가를 기록합니다. 정상 응답은 본문 없는 HTTP 204입니다.

다시 사용하려면 현재 버전을 조회하고 PUT에서 `USE_YN='Y'`를 보냅니다. 재활성화도 일반 수정과 같은 전체 자료 검사를 받습니다. 비활성화 전에 있던 연결이 잘못되었으면 그냥 다시 켜지지 않습니다.

현재 기본 언어인 행은 `deactivate()`가 비활성화를 막습니다. 회사를 비활성화한다고 관련 사업장·부서·사용자의 `USE_YN`을 모두 자동 변경하지는 않습니다. 다만 `PermissionMapper.user()`는 회사가 비활성이면 사용자를 유효 사용자로 읽지 못하므로 권한 사용이 막힙니다.

### 5.13 복합키를 읽는 작은 연습

```http
GET /api/common/admin/message-translations/BTN_SAVE~en
GET /api/common/admin/role-button-permissions/20~1010~3
```

`MasterServiceImpl.keys()`는 `~`로 키를 나누고, `MasterResource.keys()`에 적힌 순서와 개수를 맞춥니다. 첫 예는 메시지 코드와 언어 코드, 둘째 예는 역할·메뉴·버튼 번호입니다.

개수가 다르거나 빈 부분이 있으면 `ERR_INVALID_INPUT`입니다. 키로 사용할 문자열에 `~`나 `/`를 저장하는 것도 `validate()`가 막습니다. 주소 해석과 실제 키가 혼동되는 일을 피하기 위해서입니다.

<a id="chapter-6"></a>
## 6. 잘못된 입력을 막는 메서드들

### 6.1 허용 목록은 어떤 역할을 하는가

`MasterCatalog.java`는 “현재 관리할 수 있는 자료와 칸의 목록”입니다. 예를 들어 회사의 편집 칸은 `COMPANY_CODE`, `COMPANY_NAME`입니다. `MasterResource`의 생성자가 공통 편집 칸인 `USE_YN`을 덧붙입니다.

`MasterCatalog.Column`에는 `name`, `kind`, `length`, `required`, `defaultValue`, `scale`이 들어 있습니다.
필드 이름·자료 종류·길이/정밀도·필수 여부·기본값·소수 자릿수라는 뜻입니다.
`Kind`는 TEXT(글자), NUMBER(long 범위의 정수), DECIMAL(BigDecimal 십진수), TIME(시간)입니다.
5개 인자의 기존 생성자도 남아 있고 scale의 기본값은 0입니다.
예를 들어 TO_BASE_FACTOR는 DECIMAL, 전체 정밀도 38, 소수 자릿수 18이고 DISPLAY_DIVISOR는 DECIMAL(38,0)입니다.

예를 들어 `COMPANY_CODE` 정의는 “문자열이고 최대 20바이트이며 반드시 필요하고 별도 기본값은 없다”는 의미입니다. 이 정의를 `MasterServiceImpl.normalize()`와 `convert()`가 읽습니다.

Java 목록에 없는 resource나 필드 이름은 사용할 수 없습니다. 요청에서 `users WHERE 1=1` 같은 이름을 보내도 SQL로 실행하지 않습니다. `MasterResource.named()`에서 실패합니다.


<a id="catalog-line-guide"></a>
### 6.1.A 이전 MasterCatalog.java 124줄을 학습용으로 읽기

아래 번호와 코드는 **2026-10-07의 이전 124줄 버전**을 설명하는 학습 자료입니다.
현재 [MasterCatalog.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/master/MasterCatalog.java)는
21개 resource와 DECIMAL·scale을 포함하므로 아래 번호가 현재 파일의 줄 번호라는 뜻은 아닙니다.
당시의 18개 정의 설명은 유지하고, 현재 변경점은 6.1과 17.10~17.12에서 확인하세요.

먼저 이 파일을 **신청서 작성 규칙집**으로 생각하세요. 회사 등록의 실제 값은 MasterRequest.values에 있고, 이 파일에는 “회사 코드라는 칸은 문자열 20바이트까지, 회사 이름은 필수” 같은 규칙이 있습니다. 여기의 new Column은 DB에 컬럼을 생성하거나 회사 행을 INSERT하는 명령이 아닙니다. Java 안에 규칙 상자를 만드는 표현입니다.

각 줄의 입력값 예는 그 칸의 뜻을 보여 주는 예입니다. 한 칸만 적힌 예를 완성된 등록 요청으로 생각하지 마세요. 전체 필수 칸은 4.10의 표와 14장의 등록 예제를 함께 봅니다. 연결 번호·메시지 코드는 실제 DB에 있어야 하고, 기존 코드와 중복되면 안 됩니다.

**공통 읽기 규칙**

- `new MasterResource(name, table, keys, identity, columns)`: API 이름, 실제 표, 기본키 목록, 서버가 만들 숫자 ID 칸 또는 null, 편집할 칸의 규칙 목록입니다.
- `new Column(name, kind, length, required, defaultValue)`: 이름, 종류, 길이/정밀도, 필수 여부, 생략 시 기본값입니다. true/false는 현재 값을 Y/N으로 저장한다는 뜻이 아니라 **그 칸이 필수인가**입니다.
- TEXT의 길이는 현재 convert()에서 UTF-8 바이트 수로 검사합니다. NUMBER의 양수 길이는 정수의 정밀도 제한이고, 0은 그 추가 제한을 두지 않는다는 뜻입니다. long 범위와 ID의 양수 검사는 그대로 있습니다. TIME의 0은 글자 길이를 검사하지 않는다는 뜻입니다.
- null 기본값과 `"0"`·`"N"` 기본값은 다릅니다. `required=true, defaultValue=null`이면 생략할 때 필수값 오류입니다. `required=false, defaultValue=null`이면 생략할 수 있습니다. `required=true, defaultValue="0"`이면 생략 시 기본값을 채울 수 있습니다.
- 모든 MasterResource 생성자는 여기에 적히지 않은 공통 편집 칸 `USE_YN`을 Y 기본값으로 추가합니다. CREATED_BY·VERSION_NO 같은 서버 관리 칸은 편집 목록에 추가하지 않습니다.

#### 6.1.A.1 클래스와 공통 규칙: 1~11줄

**1줄**

```java
package com.erpcom.erpcom.common.master;
```

`package`는 이 클래스의 Java 주소를 선언합니다. 예를 들어 MasterResource.java가 같은 패키지에 있으므로 이 파일에서 별도 import 없이 이름을 사용할 수 있습니다. URL 주소나 DB 테이블 이름을 선언하는 줄은 아닙니다.

**2줄 — 빈 줄**

코드가 없는 줄입니다. 앞뒤 선언을 사람이 구분하기 쉽게 띄웠으며 저장 값이나 검사 결과에 영향을 주지 않습니다. 예를 들어 이 빈 줄을 없애도 회사 코드의 최대 길이 20은 바뀌지 않습니다.

**3줄**

```java
import java.util.List;
```

`import`는 Java의 List 도구를 짧은 이름으로 쓰게 합니다. 아래 List.of("COMPANY_ID")는 회사 기본키 목록 하나를 만들 때 이 도구를 사용합니다. 실제 DB 회사 목록을 조회하는 명령은 아닙니다.

**4줄 — 빈 줄**

코드가 없는 줄입니다. 앞뒤 선언을 사람이 구분하기 쉽게 띄웠으며 저장 값이나 검사 결과에 영향을 주지 않습니다. 예를 들어 이 빈 줄을 없애도 회사 코드의 최대 길이 20은 바뀌지 않습니다.

**5줄**

```java
// Generated by scripts/generate_master_catalog.py. Identifiers never come from request data.
```

`//`로 시작하므로 실행하지 않는 개발자용 주석입니다. 생성 도구의 결과이며 테이블·컬럼 식별 이름은 요청값에서 직접 받지 않는다는 의도를 적었습니다. 예를 들어 회사 이름 입력은 받아도 SQL의 테이블 이름 TB_COMPANY는 이 파일의 고정 정의에서 받습니다.

**6줄**

```java
public final class MasterCatalog {
```

public은 다른 클래스에서도 이 타입을 사용할 수 있다는 뜻이고, final은 이 클래스를 상속해 다른 종류로 늘리지 못한다는 뜻입니다. class MasterCatalog는 관리 규칙 목록을 담는 클래스 이름입니다. 뒤의 {는 클래스 내용의 시작입니다. 예를 들어 MasterController.resources()가 MasterCatalog.RESOURCES를 읽습니다.

**7줄**

```java
    private MasterCatalog() {}
```

클래스와 같은 이름이므로 생성자입니다. private이어서 다른 클래스가 new MasterCatalog()로 객체를 만들 수 없습니다. {}는 생성자가 별도 작업 없이 비어 있다는 뜻입니다. 필요한 것은 개별 Catalog 객체가 아니라 아래 static RESOURCES 목록입니다.

**8줄**

```java
    public enum Kind { TEXT, NUMBER, TIME }
```

enum은 정해진 종류만 쓸 수 있는 이름 목록입니다. TEXT는 회사 이름 같은 글자, NUMBER는 COMPANY_ID=100 같은 정수, TIME은 VALID_FROM=2030-01-02T09:00:00 같은 시간입니다. 이 이름들이 MasterServiceImpl.convert()의 switch 분기와 연결됩니다.

**9줄**

```java
    public record Column(String name, Kind kind, int length, boolean required, String defaultValue) {}
```

record Column은 한 편집 칸의 규칙을 담는 상자를 정의합니다. 순서는 이름(String), 종류(Kind), 길이·정밀도(int), 필수 여부(boolean), 기본값(String)입니다. 예를 들어 new Column("COMPANY_CODE", Kind.TEXT, 20, true, null)은 회사 코드 문자열 최대 20바이트, 필수, 생략용 기본값 없음입니다. defaultValue가 String이므로 숫자 기본값도 "0"처럼 적고 convert()가 숫자로 바꿉니다. {}는 직접 작성한 추가 메서드가 없다는 뜻이며 name(), kind(), length(), required(), defaultValue()는 Java가 제공합니다.

**10줄 — 빈 줄**

코드가 없는 줄입니다. 앞뒤 선언을 사람이 구분하기 쉽게 띄웠으며 저장 값이나 검사 결과에 영향을 주지 않습니다. 예를 들어 이 빈 줄을 없애도 회사 코드의 최대 길이 20은 바뀌지 않습니다.

**11줄**

```java
    public static final List<MasterResource> RESOURCES = List.of(
```

public은 다른 코드가 읽을 수 있게 하고, static은 MasterCatalog 객체를 만들지 않아도 클래스 이름으로 사용할 수 있게 합니다. final은 RESOURCES 변수에 다른 목록을 다시 대입하지 못하게 합니다. List<MasterResource>는 자료별 규칙 상자들을 모은 목록이라는 뜻입니다. List.of( 뒤에는 18개 규칙이 이어지며 이 목록은 항목 추가·삭제가 불가능한 목록입니다. 처음 이 클래스가 초기화될 때 구성되고 POST마다 18개 DB 표를 조회해 만드는 목록은 아닙니다.

#### 6.1.A.2 languages 규칙

**12줄**

```java
        new MasterResource("languages", "TB_LANGUAGE", List.of("LANG_CODE"), null, List.of(
```

지원 언어 한 종류의 관리 규칙입니다. 예를 들어 프랑스어를 추가한다면 LANG_CODE=fr, LANG_NAME=French 같은 값이 이 규칙으로 검사됩니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"languages"`: /api/common/admin/languages의 자료 이름입니다. MasterResource.named("languages")이 찾는 값입니다.
2. `"TB_LANGUAGE"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("LANG_CODE")`: 한 행을 찾는 기본키 1개입니다. 한 칸의 값으로 URL 키를 만듭니다.
4. `null`: 서버가 별도의 숫자 기본키를 만들지 않습니다. 아래 입력 칸에 적힌 문자열 또는 복합키를 요청에서 제공합니다. null은 기본키가 없다는 뜻이 아닙니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**13줄**

```java
            new Column("LANG_CODE", Kind.TEXT, 10, true, null),
```

`LANG_CODE` 필드는 번역을 구분하는 언어 이름표입니다. 실제 값 예는 `LANG_CODE="ko"`입니다.

다섯 인자 중 첫 번째는 `"LANG_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `10`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 10바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 LANG_CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

조회의 language()는 대소문자를 유연하게 맞추지만 관리 저장의 validateLanguage()는 ko·en·zh-CN 같은 정규 언어 표기도 확인합니다.

**14줄**

```java
            new Column("LANG_NAME", Kind.TEXT, 100, true, null),
```

`LANG_NAME` 필드는 관리자가 읽을 언어 이름입니다. 실제 값 예는 `LANG_NAME="Korean"`입니다.

다섯 인자 중 첫 번째는 `"LANG_NAME"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `100`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 100바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 LANG_NAME 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**15줄**

```java
            new Column("NATIVE_NAME", Kind.TEXT, 100, false, null),
```

`NATIVE_NAME` 필드는 그 언어로 적은 언어 이름입니다. 실제 값 예는 `NATIVE_NAME="한국어"`입니다.

다섯 인자 중 첫 번째는 `"NATIVE_NAME"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `100`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 100바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 NATIVE_NAME 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

**16줄**

```java
            new Column("FALLBACK_LANG_CODE", Kind.TEXT, 10, false, null),
```

`FALLBACK_LANG_CODE` 필드는 원하는 번역이 없을 때 다음으로 찾아볼 언어입니다. 실제 값 예는 `FALLBACK_LANG_CODE="en"`입니다.

다섯 인자 중 첫 번째는 `"FALLBACK_LANG_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `10`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 10바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 FALLBACK_LANG_CODE 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

예를 들어 ja의 이 값이 en이면 일본어 번역이 없을 때 영어를 찾습니다. 부모 언어의 존재와 대체 경로 순환은 별도 검사합니다.

**17줄**

```java
            new Column("DEFAULT_YN", Kind.TEXT, 1, true, "N"),
```

`DEFAULT_YN` 필드는 이 언어가 기본 언어인지 표시입니다. 실제 값 예는 `DEFAULT_YN="N"`입니다.

다섯 인자 중 첫 번째는 `"DEFAULT_YN"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `1`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 1바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"N"`는 신규 등록에서 생략하면 DEFAULT_YN="N"으로 채운다는 뜻입니다. 위 예의 값 또는 허용되는 다른 문자열을 직접 보낼 수도 있습니다.

뒤의 _YN 때문에 Y/N만 허용합니다. 기본 언어를 Y로 바꾸면 활성 기본 언어가 두 개가 아닌지 DB unique index도 확인합니다.

**18줄**

```java
            new Column("SORT_ORDER", Kind.NUMBER, 5, true, "0"))),
```

`SORT_ORDER` 필드는 목록을 보여 줄 정렬 순서 숫자입니다. 실제 값 예는 `SORT_ORDER=10`입니다.

다섯 인자 중 첫 번째는 `"SORT_ORDER"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `5`입니다. 이 숫자는 현재 convert()에서 최대 5자리 정밀도를 검사한다는 뜻입니다.  네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"0"`는 신규 등록에서 생략하면 SORT_ORDER=0으로 채운다는 뜻입니다. 정의에는 "0"라는 문자열로 적었지만 convert()가 숫자 0로 바꿉니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.3 messages 규칙

**19줄**

```java
        new MasterResource("messages", "TB_MESSAGE", List.of("MESSAGE_CODE"), null, List.of(
```

번역 문장 자체가 아닌 메시지 이름표와 정책의 규칙입니다. 예를 들어 새 레이블 LBL_LEARNING과 MESSAGE_TYPE=LABEL을 등록할 수 있습니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"messages"`: /api/common/admin/messages의 자료 이름입니다. MasterResource.named("messages")이 찾는 값입니다.
2. `"TB_MESSAGE"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("MESSAGE_CODE")`: 한 행을 찾는 기본키 1개입니다. 한 칸의 값으로 URL 키를 만듭니다.
4. `null`: 서버가 별도의 숫자 기본키를 만들지 않습니다. 아래 입력 칸에 적힌 문자열 또는 복합키를 요청에서 제공합니다. null은 기본키가 없다는 뜻이 아닙니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**20줄**

```java
            new Column("MESSAGE_CODE", Kind.TEXT, 100, true, null),
```

`MESSAGE_CODE` 필드는 메시지를 찾는 고유한 이름표입니다. 실제 값 예는 `MESSAGE_CODE="LBL_LEARNING"`입니다.

다섯 인자 중 첫 번째는 `"MESSAGE_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `100`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 100바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 MESSAGE_CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**21줄**

```java
            new Column("MESSAGE_TYPE", Kind.TEXT, 30, true, null),
```

`MESSAGE_TYPE` 필드는 오류·메뉴·레이블 등의 메시지 분류입니다. 실제 값 예는 `MESSAGE_TYPE="LABEL"`입니다.

다섯 인자 중 첫 번째는 `"MESSAGE_TYPE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `30`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 30바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 MESSAGE_TYPE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**22줄**

```java
            new Column("HTTP_STATUS", Kind.NUMBER, 3, false, null),
```

`HTTP_STATUS` 필드는 메시지가 오류로 사용될 때의 HTTP 상태 정책입니다. 실제 값 예는 `HTTP_STATUS=403`입니다.

다섯 인자 중 첫 번째는 `"HTTP_STATUS"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `3`입니다. 이 숫자는 현재 convert()에서 최대 3자리 정밀도를 검사한다는 뜻입니다.  네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 HTTP_STATUS 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

403은 권한 부족 오류 정책의 예입니다. convert()의 숫자 검사뿐 아니라 validate()가 100~599인지 확인하고 오류 writer는 오류 응답에서 400~599만 사용합니다.

**23줄**

```java
            new Column("SEVERITY", Kind.TEXT, 20, false, null),
```

`SEVERITY` 필드는 메시지의 중요도 표시입니다. 실제 값 예는 `SEVERITY="WARN"`입니다.

다섯 인자 중 첫 번째는 `"SEVERITY"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `20`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 20바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 SEVERITY 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

**24줄**

```java
            new Column("ARG_COUNT", Kind.NUMBER, 2, true, "0"))),
```

`ARG_COUNT` 필드는 문장의 {0}, {1} 자리에 넣을 인자 개수입니다. 실제 값 예는 `ARG_COUNT=1`입니다.

다섯 인자 중 첫 번째는 `"ARG_COUNT"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `2`입니다. 이 숫자는 현재 convert()에서 최대 2자리 정밀도를 검사한다는 뜻입니다.  네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"0"`는 신규 등록에서 생략하면 ARG_COUNT=0으로 채운다는 뜻입니다. 정의에는 "0"라는 문자열로 적었지만 convert()가 숫자 0로 바꿉니다.

1이면 {0}까지 사용할 수 있습니다. 2라면 {0}과 {1}입니다. validate()가 0~10 범위인지도 확인합니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.4 message-translations 규칙

**25줄**

```java
        new MasterResource("message-translations", "TB_MESSAGE_LANG", List.of("MESSAGE_CODE", "LANG_CODE"), null, List.of(
```

메시지 하나의 언어별 번역 규칙입니다. LBL_LEARNING~ko와 LBL_LEARNING~en은 같은 메시지의 서로 다른 행입니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"message-translations"`: /api/common/admin/message-translations의 자료 이름입니다. MasterResource.named("message-translations")이 찾는 값입니다.
2. `"TB_MESSAGE_LANG"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("MESSAGE_CODE", "LANG_CODE")`: 한 행을 찾는 기본키 2개입니다. 목록에 적힌 순서대로 ~로 이어 붙여 URL 키를 만듭니다.
4. `null`: 서버가 별도의 숫자 기본키를 만들지 않습니다. 아래 입력 칸에 적힌 문자열 또는 복합키를 요청에서 제공합니다. null은 기본키가 없다는 뜻이 아닙니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**26줄**

```java
            new Column("MESSAGE_CODE", Kind.TEXT, 100, true, null),
```

`MESSAGE_CODE` 필드는 메시지를 찾는 고유한 이름표입니다. 실제 값 예는 `MESSAGE_CODE="LBL_LEARNING"`입니다.

다섯 인자 중 첫 번째는 `"MESSAGE_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `100`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 100바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 MESSAGE_CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**27줄**

```java
            new Column("LANG_CODE", Kind.TEXT, 10, true, null),
```

`LANG_CODE` 필드는 번역을 구분하는 언어 이름표입니다. 실제 값 예는 `LANG_CODE="ko"`입니다.

다섯 인자 중 첫 번째는 `"LANG_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `10`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 10바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 LANG_CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

조회의 language()는 대소문자를 유연하게 맞추지만 관리 저장의 validateLanguage()는 ko·en·zh-CN 같은 정규 언어 표기도 확인합니다.

**28줄**

```java
            new Column("MESSAGE_TEXT", Kind.TEXT, 2000, true, null),
```

`MESSAGE_TEXT` 필드는 해당 언어로 실제 보여 줄 문장입니다. 실제 값 예는 `MESSAGE_TEXT="안녕하세요, {0}님."`입니다.

다섯 인자 중 첫 번째는 `"MESSAGE_TEXT"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `2000`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 2000바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 MESSAGE_TEXT 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

문장의 {0}을 쓰려면 해당 master의 ARG_COUNT가 최소 1이어야 합니다. validateTranslation()이 포맷과 인자 번호를 검사합니다.

**29줄**

```java
            new Column("TRANSLATED_YN", Kind.TEXT, 1, true, "Y"))),
```

`TRANSLATED_YN` 필드는 이 번역을 완성된 번역으로 사용할지 표시입니다. 실제 값 예는 `TRANSLATED_YN="Y"`입니다.

다섯 인자 중 첫 번째는 `"TRANSLATED_YN"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `1`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 1바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"Y"`는 신규 등록에서 생략하면 TRANSLATED_YN="Y"으로 채운다는 뜻입니다. 위 예의 값 또는 허용되는 다른 문자열을 직접 보낼 수도 있습니다.

Y인 활성 번역만 MessageMapper.find()가 사용합니다. N으로 저장한 번역은 읽을 때 대체 언어를 찾는 대상이 됩니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.5 companies 규칙

**30줄**

```java
        new MasterResource("companies", "TB_COMPANY", List.of("COMPANY_ID"), "COMPANY_ID", List.of(
```

회사 관리 규칙입니다. POST companies로 온 EDU01·배움 회사 값을 검사할 때 바로 이 정의를 찾습니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"companies"`: /api/common/admin/companies의 자료 이름입니다. MasterResource.named("companies")이 찾는 값입니다.
2. `"TB_COMPANY"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("COMPANY_ID")`: 한 행을 찾는 기본키 1개입니다. 한 칸의 값으로 URL 키를 만듭니다.
4. `"COMPANY_ID"`: 신규 등록 때 서버가 COMPANY_ID 번호를 발급합니다. 이 칸을 클라이언트 편집 필드로 아래에 적지 않습니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**31줄**

```java
            new Column("COMPANY_CODE", Kind.TEXT, 20, true, null),
```

`COMPANY_CODE` 필드는 회사를 사람이 구분할 식별 코드입니다. 실제 값 예는 `COMPANY_CODE="EDU01"`입니다.

다섯 인자 중 첫 번째는 `"COMPANY_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `20`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 20바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 COMPANY_CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**32줄**

```java
            new Column("COMPANY_NAME", Kind.TEXT, 100, true, null))),
```

`COMPANY_NAME` 필드는 실제 회사 이름입니다. 실제 값 예는 `COMPANY_NAME="배움 회사"`입니다.

다섯 인자 중 첫 번째는 `"COMPANY_NAME"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `100`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 100바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 COMPANY_NAME 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.6 sites 규칙

**33줄**

```java
        new MasterResource("sites", "TB_SITE", List.of("SITE_ID"), "SITE_ID", List.of(
```

사업장 규칙입니다. 회사 번호 100에 S200이라는 새 사업장을 연결할 때 사용합니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"sites"`: /api/common/admin/sites의 자료 이름입니다. MasterResource.named("sites")이 찾는 값입니다.
2. `"TB_SITE"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("SITE_ID")`: 한 행을 찾는 기본키 1개입니다. 한 칸의 값으로 URL 키를 만듭니다.
4. `"SITE_ID"`: 신규 등록 때 서버가 SITE_ID 번호를 발급합니다. 이 칸을 클라이언트 편집 필드로 아래에 적지 않습니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**34줄**

```java
            new Column("COMPANY_ID", Kind.NUMBER, 0, true, null),
```

`COMPANY_ID` 필드는 이 자료가 속하는 회사의 번호입니다. 실제 값 예는 `COMPANY_ID=100`입니다.

다섯 인자 중 첫 번째는 `"COMPANY_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 COMPANY_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**35줄**

```java
            new Column("SITE_CODE", Kind.TEXT, 20, true, null),
```

`SITE_CODE` 필드는 회사의 사업장을 구분할 코드입니다. 실제 값 예는 `SITE_CODE="S200"`입니다.

다섯 인자 중 첫 번째는 `"SITE_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `20`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 20바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 SITE_CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**36줄**

```java
            new Column("SITE_NAME", Kind.TEXT, 100, true, null))),
```

`SITE_NAME` 필드는 사업장의 이름입니다. 실제 값 예는 `SITE_NAME="연구 사업장"`입니다.

다섯 인자 중 첫 번째는 `"SITE_NAME"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `100`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 100바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 SITE_NAME 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.7 departments 규칙

**37줄**

```java
        new MasterResource("departments", "TB_DEPT", List.of("DEPT_ID"), "DEPT_ID", List.of(
```

부서 규칙입니다. 회사 100·사업장 1000에 새 부서를 만드는 값의 기본 형식을 정합니다. 조직 정합성과 부모 순환은 Service의 추가 검사입니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"departments"`: /api/common/admin/departments의 자료 이름입니다. MasterResource.named("departments")이 찾는 값입니다.
2. `"TB_DEPT"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("DEPT_ID")`: 한 행을 찾는 기본키 1개입니다. 한 칸의 값으로 URL 키를 만듭니다.
4. `"DEPT_ID"`: 신규 등록 때 서버가 DEPT_ID 번호를 발급합니다. 이 칸을 클라이언트 편집 필드로 아래에 적지 않습니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**38줄**

```java
            new Column("COMPANY_ID", Kind.NUMBER, 0, true, null),
```

`COMPANY_ID` 필드는 이 자료가 속하는 회사의 번호입니다. 실제 값 예는 `COMPANY_ID=100`입니다.

다섯 인자 중 첫 번째는 `"COMPANY_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 COMPANY_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**39줄**

```java
            new Column("SITE_ID", Kind.NUMBER, 0, false, null),
```

`SITE_ID` 필드는 소속 사업장을 가리키는 번호입니다. 실제 값 예는 `SITE_ID=1000`입니다.

다섯 인자 중 첫 번째는 `"SITE_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 SITE_ID 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

**40줄**

```java
            new Column("PARENT_DEPT_ID", Kind.NUMBER, 0, false, null),
```

`PARENT_DEPT_ID` 필드는 상위 부서를 가리키는 번호입니다. 실제 값 예는 `PARENT_DEPT_ID=10000`입니다.

다섯 인자 중 첫 번째는 `"PARENT_DEPT_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 PARENT_DEPT_ID 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

**41줄**

```java
            new Column("DEPT_CODE", Kind.TEXT, 30, true, null),
```

`DEPT_CODE` 필드는 부서를 구분하는 코드입니다. 실제 값 예는 `DEPT_CODE="D200"`입니다.

다섯 인자 중 첫 번째는 `"DEPT_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `30`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 30바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 DEPT_CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**42줄**

```java
            new Column("DEPT_NAME", Kind.TEXT, 100, true, null),
```

`DEPT_NAME` 필드는 부서의 이름입니다. 실제 값 예는 `DEPT_NAME="개발부"`입니다.

다섯 인자 중 첫 번째는 `"DEPT_NAME"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `100`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 100바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 DEPT_NAME 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**43줄**

```java
            new Column("DEPT_LEVEL", Kind.NUMBER, 3, true, "1"),
```

`DEPT_LEVEL` 필드는 부서 표시의 단계 숫자입니다. 실제 값 예는 `DEPT_LEVEL=2`입니다.

다섯 인자 중 첫 번째는 `"DEPT_LEVEL"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `3`입니다. 이 숫자는 현재 convert()에서 최대 3자리 정밀도를 검사한다는 뜻입니다.  네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"1"`는 신규 등록에서 생략하면 DEPT_LEVEL=1으로 채운다는 뜻입니다. 정의에는 "1"라는 문자열로 적었지만 convert()가 숫자 1로 바꿉니다.

숫자를 2로 적어도 부모 부서를 자동 선택하지 않습니다. 부모는 PARENT_DEPT_ID로 별도 지정하고 계층 단계는 자동 계산하지 않습니다.

**44줄**

```java
            new Column("SORT_ORDER", Kind.NUMBER, 5, true, "0"))),
```

`SORT_ORDER` 필드는 목록을 보여 줄 정렬 순서 숫자입니다. 실제 값 예는 `SORT_ORDER=10`입니다.

다섯 인자 중 첫 번째는 `"SORT_ORDER"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `5`입니다. 이 숫자는 현재 convert()에서 최대 5자리 정밀도를 검사한다는 뜻입니다.  네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"0"`는 신규 등록에서 생략하면 SORT_ORDER=0으로 채운다는 뜻입니다. 정의에는 "0"라는 문자열로 적었지만 convert()가 숫자 0로 바꿉니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.8 users 규칙

**45줄**

```java
        new MasterResource("users", "TB_USER", List.of("USER_ID"), "USER_ID", List.of(
```

사용자 기본정보 규칙입니다. learning.user를 회사 100에 등록하는 예를 생각하면 됩니다. 암호·실패 횟수·AUTH_TYPE은 이 편집 목록에 없습니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"users"`: /api/common/admin/users의 자료 이름입니다. MasterResource.named("users")이 찾는 값입니다.
2. `"TB_USER"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("USER_ID")`: 한 행을 찾는 기본키 1개입니다. 한 칸의 값으로 URL 키를 만듭니다.
4. `"USER_ID"`: 신규 등록 때 서버가 USER_ID 번호를 발급합니다. 이 칸을 클라이언트 편집 필드로 아래에 적지 않습니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**46줄**

```java
            new Column("LOGIN_ID", Kind.TEXT, 100, true, null),
```

`LOGIN_ID` 필드는 사용자에게 부여한 로그인 식별값입니다. 실제 값 예는 `LOGIN_ID="learning.user"`입니다.

다섯 인자 중 첫 번째는 `"LOGIN_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `100`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 100바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 LOGIN_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**47줄**

```java
            new Column("USER_NAME", Kind.TEXT, 200, true, null),
```

`USER_NAME` 필드는 사용자의 이름입니다. 실제 값 예는 `USER_NAME="배움 사용자"`입니다.

다섯 인자 중 첫 번째는 `"USER_NAME"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `200`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 200바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 USER_NAME 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**48줄**

```java
            new Column("EMP_NO", Kind.TEXT, 30, false, null),
```

`EMP_NO` 필드는 직원을 구분할 사번입니다. 실제 값 예는 `EMP_NO="EMP001"`입니다.

다섯 인자 중 첫 번째는 `"EMP_NO"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `30`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 30바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 EMP_NO 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

**49줄**

```java
            new Column("COMPANY_ID", Kind.NUMBER, 0, true, null),
```

`COMPANY_ID` 필드는 이 자료가 속하는 회사의 번호입니다. 실제 값 예는 `COMPANY_ID=100`입니다.

다섯 인자 중 첫 번째는 `"COMPANY_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 COMPANY_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**50줄**

```java
            new Column("SITE_ID", Kind.NUMBER, 0, false, null),
```

`SITE_ID` 필드는 소속 사업장을 가리키는 번호입니다. 실제 값 예는 `SITE_ID=1000`입니다.

다섯 인자 중 첫 번째는 `"SITE_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 SITE_ID 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

**51줄**

```java
            new Column("DEPT_ID", Kind.NUMBER, 0, false, null),
```

`DEPT_ID` 필드는 사용자의 소속 부서 번호입니다. 실제 값 예는 `DEPT_ID=10000`입니다.

다섯 인자 중 첫 번째는 `"DEPT_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 DEPT_ID 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

**52줄**

```java
            new Column("EMAIL", Kind.TEXT, 200, false, null),
```

`EMAIL` 필드는 사용자의 이메일 주소입니다. 실제 값 예는 `EMAIL="learner@example.com"`입니다.

다섯 인자 중 첫 번째는 `"EMAIL"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `200`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 200바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 EMAIL 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

**53줄**

```java
            new Column("LANG_CODE", Kind.TEXT, 10, false, null),
```

`LANG_CODE` 필드는 번역을 구분하는 언어 이름표입니다. 실제 값 예는 `LANG_CODE="ko"`입니다.

다섯 인자 중 첫 번째는 `"LANG_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `10`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 10바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 LANG_CODE 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

조회의 language()는 대소문자를 유연하게 맞추지만 관리 저장의 validateLanguage()는 ko·en·zh-CN 같은 정규 언어 표기도 확인합니다.

**54줄**

```java
            new Column("USER_STATUS", Kind.TEXT, 20, true, "ACTIVE"))),
```

`USER_STATUS` 필드는 USER_STATUS 공통코드에 연결되는 사용자 상태입니다. 실제 값 예는 `USER_STATUS="ACTIVE"`입니다.

다섯 인자 중 첫 번째는 `"USER_STATUS"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `20`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 20바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"ACTIVE"`는 신규 등록에서 생략하면 USER_STATUS="ACTIVE"으로 채운다는 뜻입니다. 위 예의 값 또는 허용되는 다른 문자열을 직접 보낼 수도 있습니다.

이 문자열이 활성 USER_STATUS 코드인지 validate()가 확인합니다. PermissionMapper.user()는 현재 ACTIVE만 유효 사용자로 읽습니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.9 roles 규칙

**55줄**

```java
        new MasterResource("roles", "TB_ROLE", List.of("ROLE_ID"), "ROLE_ID", List.of(
```

역할 master 규칙입니다. 새 역할 코드는 LEARNING_VIEWER여도 표시 이름은 이미 있는 ROLE_VIEWER 메시지를 참조할 수 있습니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"roles"`: /api/common/admin/roles의 자료 이름입니다. MasterResource.named("roles")이 찾는 값입니다.
2. `"TB_ROLE"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("ROLE_ID")`: 한 행을 찾는 기본키 1개입니다. 한 칸의 값으로 URL 키를 만듭니다.
4. `"ROLE_ID"`: 신규 등록 때 서버가 ROLE_ID 번호를 발급합니다. 이 칸을 클라이언트 편집 필드로 아래에 적지 않습니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**56줄**

```java
            new Column("ROLE_CODE", Kind.TEXT, 50, true, null),
```

`ROLE_CODE` 필드는 역할의 식별 코드입니다. 실제 값 예는 `ROLE_CODE="LEARNING_VIEWER"`입니다.

다섯 인자 중 첫 번째는 `"ROLE_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `50`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 50바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 ROLE_CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**57줄**

```java
            new Column("ROLE_NAME_MSG_CODE", Kind.TEXT, 100, true, null),
```

`ROLE_NAME_MSG_CODE` 필드는 역할 표시 이름을 번역할 메시지 코드입니다. 실제 값 예는 `ROLE_NAME_MSG_CODE="ROLE_VIEWER"`입니다.

다섯 인자 중 첫 번째는 `"ROLE_NAME_MSG_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `100`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 100바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 ROLE_NAME_MSG_CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**58줄**

```java
            new Column("ROLE_TYPE", Kind.TEXT, 20, true, "BUSINESS"),
```

`ROLE_TYPE` 필드는 시스템용인지 업무용인지 역할 분류입니다. 실제 값 예는 `ROLE_TYPE="BUSINESS"`입니다.

다섯 인자 중 첫 번째는 `"ROLE_TYPE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `20`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 20바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"BUSINESS"`는 신규 등록에서 생략하면 ROLE_TYPE="BUSINESS"으로 채운다는 뜻입니다. 위 예의 값 또는 허용되는 다른 문자열을 직접 보낼 수도 있습니다.

현재 DB CHECK는 SYSTEM 또는 BUSINESS를 허용합니다. 문자열 길이만 맞는 임의 값이 최종 저장까지 허용된다는 뜻은 아닙니다.

**59줄**

```java
            new Column("DESCRIPTION", Kind.TEXT, 500, false, null))),
```

`DESCRIPTION` 필드는 관리자가 참고할 설명입니다. 실제 값 예는 `DESCRIPTION="설명용 기준자료"`입니다.

다섯 인자 중 첫 번째는 `"DESCRIPTION"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `500`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 500바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 DESCRIPTION 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.10 user-roles 규칙

**60줄**

```java
        new MasterResource("user-roles", "TB_USER_ROLE", List.of("USER_ROLE_ID"), "USER_ROLE_ID", List.of(
```

사용자에게 역할을 주는 연결 규칙입니다. 사용자 2에게 역할 50을 언제부터 언제까지 주는지 값을 검사합니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"user-roles"`: /api/common/admin/user-roles의 자료 이름입니다. MasterResource.named("user-roles")이 찾는 값입니다.
2. `"TB_USER_ROLE"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("USER_ROLE_ID")`: 한 행을 찾는 기본키 1개입니다. 한 칸의 값으로 URL 키를 만듭니다.
4. `"USER_ROLE_ID"`: 신규 등록 때 서버가 USER_ROLE_ID 번호를 발급합니다. 이 칸을 클라이언트 편집 필드로 아래에 적지 않습니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**61줄**

```java
            new Column("USER_ID", Kind.NUMBER, 0, true, null),
```

`USER_ID` 필드는 역할 또는 개인 예외를 적용할 사용자 번호입니다. 실제 값 예는 `USER_ID=2`입니다.

다섯 인자 중 첫 번째는 `"USER_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 USER_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**62줄**

```java
            new Column("ROLE_ID", Kind.NUMBER, 0, true, null),
```

`ROLE_ID` 필드는 연결하거나 권한을 부여할 역할 번호입니다. 실제 값 예는 `ROLE_ID=50`입니다.

다섯 인자 중 첫 번째는 `"ROLE_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 ROLE_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**63줄**

```java
            new Column("VALID_FROM", Kind.TIME, 0, true, "@now"),
```

`VALID_FROM` 필드는 이 연결이나 예외가 시작되는 시각입니다. 실제 값 예는 `VALID_FROM="2030-01-02T09:00:00"`입니다.

다섯 인자 중 첫 번째는 `"VALID_FROM"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TIME`는 시간으로 읽는 칸입니다. 세 번째 `0`은 TEXT 같은 바이트 길이를 검사하지 않는다는 뜻입니다. 위 ISO 지역 날짜·시간 예를 Timestamp로 변환합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"@now"`는 신규 등록 시 생략하면 mapper.now()에서 DB 현재 시각을 읽어 넣으라는 특수 기본값입니다. 시작 시간을 직접 위 예처럼 보내면 그 값을 사용합니다. 따옴표 안의 @now는 문자열이며 @Valid 같은 Java 어노테이션이 아닙니다.

현재 SQL 권한 조회는 시작 시각을 포함하며 미래의 시작이면 아직 활성 권한에 넣지 않습니다.

**64줄**

```java
            new Column("VALID_TO", Kind.TIME, 0, false, null),
```

`VALID_TO` 필드는 이 연결이나 예외가 끝나는 시각입니다. 실제 값 예는 `VALID_TO="2030-12-31T18:00:00"`입니다.

다섯 인자 중 첫 번째는 `"VALID_TO"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TIME`는 시간으로 읽는 칸입니다. 세 번째 `0`은 TEXT 같은 바이트 길이를 검사하지 않는다는 뜻입니다. 위 ISO 지역 날짜·시간 예를 Timestamp로 변환합니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 VALID_TO 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

종료를 보내면 시작보다 빠르지 않아야 합니다. null이면 종료 시각이 정해지지 않은 기간이고 현재 구현은 종료 시각도 포함합니다.

**65줄**

```java
            new Column("REASON", Kind.TEXT, 500, false, null))),
```

`REASON` 필드는 역할 부여나 개인 예외의 이유입니다. 실제 값 예는 `REASON="한시적 업무 지원"`입니다.

다섯 인자 중 첫 번째는 `"REASON"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `500`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 500바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 REASON 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.11 menus 규칙

**66줄**

```java
        new MasterResource("menus", "TB_MENU", List.of("MENU_ID"), "MENU_ID", List.of(
```

메뉴 기준정보 규칙입니다. 새 메뉴의 식별 코드와 표시 이름용 메시지 코드, 화면 경로를 관리합니다. 이 정의가 화면 파일을 생성하지는 않습니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"menus"`: /api/common/admin/menus의 자료 이름입니다. MasterResource.named("menus")이 찾는 값입니다.
2. `"TB_MENU"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("MENU_ID")`: 한 행을 찾는 기본키 1개입니다. 한 칸의 값으로 URL 키를 만듭니다.
4. `"MENU_ID"`: 신규 등록 때 서버가 MENU_ID 번호를 발급합니다. 이 칸을 클라이언트 편집 필드로 아래에 적지 않습니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**67줄**

```java
            new Column("PARENT_MENU_ID", Kind.NUMBER, 0, false, null),
```

`PARENT_MENU_ID` 필드는 상위 메뉴 번호입니다. 실제 값 예는 `PARENT_MENU_ID=1000`입니다.

다섯 인자 중 첫 번째는 `"PARENT_MENU_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 PARENT_MENU_ID 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

**68줄**

```java
            new Column("MENU_CODE", Kind.TEXT, 50, true, null),
```

`MENU_CODE` 필드는 메뉴를 구분하고 권한 검사에서 사용하는 코드입니다. 실제 값 예는 `MENU_CODE="LEARNING_MENU"`입니다.

다섯 인자 중 첫 번째는 `"MENU_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `50`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 50바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 MENU_CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**69줄**

```java
            new Column("MENU_NAME_MSG_CODE", Kind.TEXT, 100, true, null),
```

`MENU_NAME_MSG_CODE` 필드는 메뉴 표시 이름을 번역할 메시지 코드입니다. 실제 값 예는 `MENU_NAME_MSG_CODE="MNU_PROJECT_LIST"`입니다.

다섯 인자 중 첫 번째는 `"MENU_NAME_MSG_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `100`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 100바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 MENU_NAME_MSG_CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**70줄**

```java
            new Column("MENU_LEVEL", Kind.NUMBER, 3, true, "1"),
```

`MENU_LEVEL` 필드는 메뉴 표시의 단계 숫자입니다. 실제 값 예는 `MENU_LEVEL=2`입니다.

다섯 인자 중 첫 번째는 `"MENU_LEVEL"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `3`입니다. 이 숫자는 현재 convert()에서 최대 3자리 정밀도를 검사한다는 뜻입니다.  네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"1"`는 신규 등록에서 생략하면 MENU_LEVEL=1으로 채운다는 뜻입니다. 정의에는 "1"라는 문자열로 적었지만 convert()가 숫자 1로 바꿉니다.

예의 2는 표시 단계 값입니다. 부모 메뉴 번호와 일치하도록 자동 재계산하는 기능은 현재 없습니다.

**71줄**

```java
            new Column("MENU_TYPE", Kind.TEXT, 20, true, "SCREEN"),
```

`MENU_TYPE` 필드는 메뉴가 묶음·화면·팝업 중 무엇인지 분류입니다. 실제 값 예는 `MENU_TYPE="SCREEN"`입니다.

다섯 인자 중 첫 번째는 `"MENU_TYPE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `20`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 20바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"SCREEN"`는 신규 등록에서 생략하면 MENU_TYPE="SCREEN"으로 채운다는 뜻입니다. 위 예의 값 또는 허용되는 다른 문자열을 직접 보낼 수도 있습니다.

현재 DB CHECK는 GROUP·SCREEN·POPUP입니다. 값이 SCREEN이라고 실제 화면 코드를 자동 생성하지 않습니다.

**72줄**

```java
            new Column("SCREEN_ID", Kind.TEXT, 100, false, null),
```

`SCREEN_ID` 필드는 화면 프로그램에서 찾을 화면 식별값입니다. 실제 값 예는 `SCREEN_ID="PM_PJ010"`입니다.

다섯 인자 중 첫 번째는 `"SCREEN_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `100`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 100바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 SCREEN_ID 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

**73줄**

```java
            new Column("FORM_PATH", Kind.TEXT, 300, false, null),
```

`FORM_PATH` 필드는 화면 프로그램이 참고할 화면 파일 경로입니다. 실제 값 예는 `FORM_PATH="pm/PM_PJ010.xfdl"`입니다.

다섯 인자 중 첫 번째는 `"FORM_PATH"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `300`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 300바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 FORM_PATH 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

이 경로는 기준정보 값입니다. 실제 .xfdl 화면 파일을 이 프로젝트가 함께 생성하거나 실행하는 명령이 아닙니다.

**74줄**

```java
            new Column("ICON_NAME", Kind.TEXT, 100, false, null),
```

`ICON_NAME` 필드는 화면이 사용할 아이콘의 이름입니다. 실제 값 예는 `ICON_NAME="folder"`입니다.

다섯 인자 중 첫 번째는 `"ICON_NAME"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `100`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 100바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 ICON_NAME 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

**75줄**

```java
            new Column("SORT_ORDER", Kind.NUMBER, 5, true, "0"),
```

`SORT_ORDER` 필드는 목록을 보여 줄 정렬 순서 숫자입니다. 실제 값 예는 `SORT_ORDER=10`입니다.

다섯 인자 중 첫 번째는 `"SORT_ORDER"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `5`입니다. 이 숫자는 현재 convert()에서 최대 5자리 정밀도를 검사한다는 뜻입니다.  네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"0"`는 신규 등록에서 생략하면 SORT_ORDER=0으로 채운다는 뜻입니다. 정의에는 "0"라는 문자열로 적었지만 convert()가 숫자 0로 바꿉니다.

**76줄**

```java
            new Column("SENSITIVE_YN", Kind.TEXT, 1, true, "N"))),
```

`SENSITIVE_YN` 필드는 민감한 기능인지 나타내는 기준정보입니다. 실제 값 예는 `SENSITIVE_YN="N"`입니다.

다섯 인자 중 첫 번째는 `"SENSITIVE_YN"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `1`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 1바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"N"`는 신규 등록에서 생략하면 SENSITIVE_YN="N"으로 채운다는 뜻입니다. 위 예의 값 또는 허용되는 다른 문자열을 직접 보낼 수도 있습니다.

Y/N만 허용합니다. Y라고 현재 권한 계산이 자동으로 추가 인증이나 DENY 우선으로 바뀌지 않습니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.12 buttons 규칙

**77줄**

```java
        new MasterResource("buttons", "TB_BUTTON", List.of("BUTTON_ID"), "BUTTON_ID", List.of(
```

여러 메뉴에서 함께 사용할 기능 master 규칙입니다. 조회 기능을 새 코드로 추가하더라도 실제 화면의 클릭 동작까지 만드는 것은 아닙니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"buttons"`: /api/common/admin/buttons의 자료 이름입니다. MasterResource.named("buttons")이 찾는 값입니다.
2. `"TB_BUTTON"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("BUTTON_ID")`: 한 행을 찾는 기본키 1개입니다. 한 칸의 값으로 URL 키를 만듭니다.
4. `"BUTTON_ID"`: 신규 등록 때 서버가 BUTTON_ID 번호를 발급합니다. 이 칸을 클라이언트 편집 필드로 아래에 적지 않습니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**78줄**

```java
            new Column("BUTTON_CODE", Kind.TEXT, 50, true, null),
```

`BUTTON_CODE` 필드는 버튼 또는 기능을 구분하는 코드입니다. 실제 값 예는 `BUTTON_CODE="LEARNING_SEARCH"`입니다.

다섯 인자 중 첫 번째는 `"BUTTON_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `50`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 50바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 BUTTON_CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**79줄**

```java
            new Column("BUTTON_NAME_MSG_CODE", Kind.TEXT, 100, true, null),
```

`BUTTON_NAME_MSG_CODE` 필드는 버튼 표시 이름을 번역할 메시지 코드입니다. 실제 값 예는 `BUTTON_NAME_MSG_CODE="BTN_SEARCH"`입니다.

다섯 인자 중 첫 번째는 `"BUTTON_NAME_MSG_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `100`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 100바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 BUTTON_NAME_MSG_CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**80줄**

```java
            new Column("BUTTON_TYPE", Kind.TEXT, 30, false, null),
```

`BUTTON_TYPE` 필드는 조회·저장 등 버튼 기능의 분류입니다. 실제 값 예는 `BUTTON_TYPE="QUERY"`입니다.

다섯 인자 중 첫 번째는 `"BUTTON_TYPE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `30`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 30바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 BUTTON_TYPE 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

**81줄**

```java
            new Column("SENSITIVE_YN", Kind.TEXT, 1, true, "N"))),
```

`SENSITIVE_YN` 필드는 민감한 기능인지 나타내는 기준정보입니다. 실제 값 예는 `SENSITIVE_YN="N"`입니다.

다섯 인자 중 첫 번째는 `"SENSITIVE_YN"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `1`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 1바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"N"`는 신규 등록에서 생략하면 SENSITIVE_YN="N"으로 채운다는 뜻입니다. 위 예의 값 또는 허용되는 다른 문자열을 직접 보낼 수도 있습니다.

Y/N만 허용합니다. Y라고 현재 권한 계산이 자동으로 추가 인증이나 DENY 우선으로 바뀌지 않습니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.13 menu-buttons 규칙

**82줄**

```java
        new MasterResource("menu-buttons", "TB_MENU_BUTTON", List.of("MENU_ID", "BUTTON_ID"), null, List.of(
```

특정 메뉴에 버튼을 배치하는 규칙입니다. 메뉴 1010과 버튼 1의 짝은 1010~1로 찾습니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"menu-buttons"`: /api/common/admin/menu-buttons의 자료 이름입니다. MasterResource.named("menu-buttons")이 찾는 값입니다.
2. `"TB_MENU_BUTTON"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("MENU_ID", "BUTTON_ID")`: 한 행을 찾는 기본키 2개입니다. 목록에 적힌 순서대로 ~로 이어 붙여 URL 키를 만듭니다.
4. `null`: 서버가 별도의 숫자 기본키를 만들지 않습니다. 아래 입력 칸에 적힌 문자열 또는 복합키를 요청에서 제공합니다. null은 기본키가 없다는 뜻이 아닙니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**83줄**

```java
            new Column("MENU_ID", Kind.NUMBER, 0, true, null),
```

`MENU_ID` 필드는 권한이나 버튼을 연결할 메뉴 번호입니다. 실제 값 예는 `MENU_ID=1010`입니다.

다섯 인자 중 첫 번째는 `"MENU_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 MENU_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**84줄**

```java
            new Column("BUTTON_ID", Kind.NUMBER, 0, true, null),
```

`BUTTON_ID` 필드는 해당 화면에 연결할 버튼 번호입니다. 실제 값 예는 `BUTTON_ID=1`입니다.

다섯 인자 중 첫 번째는 `"BUTTON_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 BUTTON_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**85줄**

```java
            new Column("SORT_ORDER", Kind.NUMBER, 5, true, "0"))),
```

`SORT_ORDER` 필드는 목록을 보여 줄 정렬 순서 숫자입니다. 실제 값 예는 `SORT_ORDER=10`입니다.

다섯 인자 중 첫 번째는 `"SORT_ORDER"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `5`입니다. 이 숫자는 현재 convert()에서 최대 5자리 정밀도를 검사한다는 뜻입니다.  네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"0"`는 신규 등록에서 생략하면 SORT_ORDER=0으로 채운다는 뜻입니다. 정의에는 "0"라는 문자열로 적었지만 convert()가 숫자 0로 바꿉니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.14 role-menu-permissions 규칙

**86줄**

```java
        new MasterResource("role-menu-permissions", "TB_ROLE_MENU_AUTH", List.of("ROLE_MENU_AUTH_ID"), "ROLE_MENU_AUTH_ID", List.of(
```

역할별 메뉴 허용의 규칙입니다. 역할 50·메뉴 1010에 READ_ALL+OWN 같은 짝을 저장합니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"role-menu-permissions"`: /api/common/admin/role-menu-permissions의 자료 이름입니다. MasterResource.named("role-menu-permissions")이 찾는 값입니다.
2. `"TB_ROLE_MENU_AUTH"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("ROLE_MENU_AUTH_ID")`: 한 행을 찾는 기본키 1개입니다. 한 칸의 값으로 URL 키를 만듭니다.
4. `"ROLE_MENU_AUTH_ID"`: 신규 등록 때 서버가 ROLE_MENU_AUTH_ID 번호를 발급합니다. 이 칸을 클라이언트 편집 필드로 아래에 적지 않습니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**87줄**

```java
            new Column("ROLE_ID", Kind.NUMBER, 0, true, null),
```

`ROLE_ID` 필드는 연결하거나 권한을 부여할 역할 번호입니다. 실제 값 예는 `ROLE_ID=50`입니다.

다섯 인자 중 첫 번째는 `"ROLE_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 ROLE_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**88줄**

```java
            new Column("MENU_ID", Kind.NUMBER, 0, true, null),
```

`MENU_ID` 필드는 권한이나 버튼을 연결할 메뉴 번호입니다. 실제 값 예는 `MENU_ID=1010`입니다.

다섯 인자 중 첫 번째는 `"MENU_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 MENU_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**89줄**

```java
            new Column("ACCESS_LEVEL", Kind.TEXT, 20, true, "NONE"),
```

`ACCESS_LEVEL` 필드는 메뉴에서 어떤 동작 수준을 허용할지 표시입니다. 실제 값 예는 `ACCESS_LEVEL="READ_ALL"`입니다.

다섯 인자 중 첫 번째는 `"ACCESS_LEVEL"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `20`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 20바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"NONE"`는 신규 등록에서 생략하면 ACCESS_LEVEL="NONE"으로 채운다는 뜻입니다. 위 예의 값 또는 허용되는 다른 문자열을 직접 보낼 수도 있습니다.

기본 NONE은 허용을 추가하지 않는다는 뜻입니다. 역할+메뉴 번호만 보내면 이 기본값 때문에 새 메뉴 허용이 생기지 않습니다. READ_ALL 등 실제 허용 수준을 명시해야 합니다.

**90줄**

```java
            new Column("DATA_SCOPE", Kind.TEXT, 20, true, "OWN"))),
```

`DATA_SCOPE` 필드는 사용자·부서·사업장·회사 중 허용 자료 범위입니다. 실제 값 예는 `DATA_SCOPE="DEPT"`입니다.

다섯 인자 중 첫 번째는 `"DATA_SCOPE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `20`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 20바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"OWN"`는 신규 등록에서 생략하면 DATA_SCOPE="OWN"으로 채운다는 뜻입니다. 위 예의 값 또는 허용되는 다른 문자열을 직접 보낼 수도 있습니다.

OWN 기본값은 자료 소유자가 본인인 범위입니다. 수준과 이 범위는 Grant 한 짝으로 보존됩니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.15 user-menu-permissions 규칙

**91줄**

```java
        new MasterResource("user-menu-permissions", "TB_USER_MENU_AUTH", List.of("USER_MENU_AUTH_ID"), "USER_MENU_AUTH_ID", List.of(
```

개인 메뉴 예외 규칙입니다. 사용자 2·메뉴 1010에 수준은 null로 상속하고 범위만 DEPT로 바꾸는 예를 생각하면 됩니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"user-menu-permissions"`: /api/common/admin/user-menu-permissions의 자료 이름입니다. MasterResource.named("user-menu-permissions")이 찾는 값입니다.
2. `"TB_USER_MENU_AUTH"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("USER_MENU_AUTH_ID")`: 한 행을 찾는 기본키 1개입니다. 한 칸의 값으로 URL 키를 만듭니다.
4. `"USER_MENU_AUTH_ID"`: 신규 등록 때 서버가 USER_MENU_AUTH_ID 번호를 발급합니다. 이 칸을 클라이언트 편집 필드로 아래에 적지 않습니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**92줄**

```java
            new Column("USER_ID", Kind.NUMBER, 0, true, null),
```

`USER_ID` 필드는 역할 또는 개인 예외를 적용할 사용자 번호입니다. 실제 값 예는 `USER_ID=2`입니다.

다섯 인자 중 첫 번째는 `"USER_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 USER_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**93줄**

```java
            new Column("MENU_ID", Kind.NUMBER, 0, true, null),
```

`MENU_ID` 필드는 권한이나 버튼을 연결할 메뉴 번호입니다. 실제 값 예는 `MENU_ID=1010`입니다.

다섯 인자 중 첫 번째는 `"MENU_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 MENU_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**94줄**

```java
            new Column("ACCESS_LEVEL", Kind.TEXT, 20, false, null),
```

`ACCESS_LEVEL` 필드는 메뉴에서 어떤 동작 수준을 허용할지 표시입니다. 실제 값 예는 `ACCESS_LEVEL="READ_ALL"`입니다.

다섯 인자 중 첫 번째는 `"ACCESS_LEVEL"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `20`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 20바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 ACCESS_LEVEL 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

이 개인 예외에서 null은 ROLE 수준 상속, NONE은 명시적 메뉴 금지입니다. READ_ALL을 적으면 그 수준으로 덮습니다.

**95줄**

```java
            new Column("DATA_SCOPE", Kind.TEXT, 20, false, null),
```

`DATA_SCOPE` 필드는 사용자·부서·사업장·회사 중 허용 자료 범위입니다. 실제 값 예는 `DATA_SCOPE="DEPT"`입니다.

다섯 인자 중 첫 번째는 `"DATA_SCOPE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `20`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 20바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 DATA_SCOPE 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

이 개인 예외에서 null이면 ROLE 범위를 유지합니다. DEPT를 적으면 수준은 유지하면서 범위만 같은 부서로 바꿀 수 있습니다.

**96줄**

```java
            new Column("VALID_FROM", Kind.TIME, 0, true, "@now"),
```

`VALID_FROM` 필드는 이 연결이나 예외가 시작되는 시각입니다. 실제 값 예는 `VALID_FROM="2030-01-02T09:00:00"`입니다.

다섯 인자 중 첫 번째는 `"VALID_FROM"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TIME`는 시간으로 읽는 칸입니다. 세 번째 `0`은 TEXT 같은 바이트 길이를 검사하지 않는다는 뜻입니다. 위 ISO 지역 날짜·시간 예를 Timestamp로 변환합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"@now"`는 신규 등록 시 생략하면 mapper.now()에서 DB 현재 시각을 읽어 넣으라는 특수 기본값입니다. 시작 시간을 직접 위 예처럼 보내면 그 값을 사용합니다. 따옴표 안의 @now는 문자열이며 @Valid 같은 Java 어노테이션이 아닙니다.

현재 SQL 권한 조회는 시작 시각을 포함하며 미래의 시작이면 아직 활성 권한에 넣지 않습니다.

**97줄**

```java
            new Column("VALID_TO", Kind.TIME, 0, false, null),
```

`VALID_TO` 필드는 이 연결이나 예외가 끝나는 시각입니다. 실제 값 예는 `VALID_TO="2030-12-31T18:00:00"`입니다.

다섯 인자 중 첫 번째는 `"VALID_TO"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TIME`는 시간으로 읽는 칸입니다. 세 번째 `0`은 TEXT 같은 바이트 길이를 검사하지 않는다는 뜻입니다. 위 ISO 지역 날짜·시간 예를 Timestamp로 변환합니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 VALID_TO 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

종료를 보내면 시작보다 빠르지 않아야 합니다. null이면 종료 시각이 정해지지 않은 기간이고 현재 구현은 종료 시각도 포함합니다.

**98줄**

```java
            new Column("REASON", Kind.TEXT, 500, false, null))),
```

`REASON` 필드는 역할 부여나 개인 예외의 이유입니다. 실제 값 예는 `REASON="한시적 업무 지원"`입니다.

다섯 인자 중 첫 번째는 `"REASON"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `500`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 500바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 REASON 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.16 role-button-permissions 규칙

**99줄**

```java
        new MasterResource("role-button-permissions", "TB_ROLE_BUTTON_AUTH", List.of("ROLE_ID", "MENU_ID", "BUTTON_ID"), null, List.of(
```

역할·메뉴·버튼 세 값으로 기능 허용을 정하는 규칙입니다. 역할 50이 메뉴 1010에서 버튼 1을 실행하는 허용은 50~1010~1로 찾습니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"role-button-permissions"`: /api/common/admin/role-button-permissions의 자료 이름입니다. MasterResource.named("role-button-permissions")이 찾는 값입니다.
2. `"TB_ROLE_BUTTON_AUTH"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("ROLE_ID", "MENU_ID", "BUTTON_ID")`: 한 행을 찾는 기본키 3개입니다. 목록에 적힌 순서대로 ~로 이어 붙여 URL 키를 만듭니다.
4. `null`: 서버가 별도의 숫자 기본키를 만들지 않습니다. 아래 입력 칸에 적힌 문자열 또는 복합키를 요청에서 제공합니다. null은 기본키가 없다는 뜻이 아닙니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**100줄**

```java
            new Column("ROLE_ID", Kind.NUMBER, 0, true, null),
```

`ROLE_ID` 필드는 연결하거나 권한을 부여할 역할 번호입니다. 실제 값 예는 `ROLE_ID=50`입니다.

다섯 인자 중 첫 번째는 `"ROLE_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 ROLE_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**101줄**

```java
            new Column("MENU_ID", Kind.NUMBER, 0, true, null),
```

`MENU_ID` 필드는 권한이나 버튼을 연결할 메뉴 번호입니다. 실제 값 예는 `MENU_ID=1010`입니다.

다섯 인자 중 첫 번째는 `"MENU_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 MENU_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**102줄**

```java
            new Column("BUTTON_ID", Kind.NUMBER, 0, true, null),
```

`BUTTON_ID` 필드는 해당 화면에 연결할 버튼 번호입니다. 실제 값 예는 `BUTTON_ID=1`입니다.

다섯 인자 중 첫 번째는 `"BUTTON_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 BUTTON_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**103줄**

```java
            new Column("ACCESS_YN", Kind.TEXT, 1, true, "Y"))),
```

`ACCESS_YN` 필드는 메뉴별 버튼 허용 여부 표시입니다. 실제 값 예는 `ACCESS_YN="Y"`입니다.

다섯 인자 중 첫 번째는 `"ACCESS_YN"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `1`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 1바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"Y"`는 신규 등록에서 생략하면 ACCESS_YN="Y"으로 채운다는 뜻입니다. 위 예의 값 또는 허용되는 다른 문자열을 직접 보낼 수도 있습니다.

역할 허용에서 Y는 허용 합집합에 추가합니다. 한 ROLE의 N이 다른 ROLE의 Y를 제거하는 정책은 아닙니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.17 user-button-permissions 규칙

**104줄**

```java
        new MasterResource("user-button-permissions", "TB_USER_BUTTON_AUTH", List.of("USER_BUTTON_AUTH_ID"), "USER_BUTTON_AUTH_ID", List.of(
```

개인 버튼 예외 규칙입니다. 사용자 2에게 메뉴 1010의 버튼 1만 N으로 막는 경우입니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"user-button-permissions"`: /api/common/admin/user-button-permissions의 자료 이름입니다. MasterResource.named("user-button-permissions")이 찾는 값입니다.
2. `"TB_USER_BUTTON_AUTH"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("USER_BUTTON_AUTH_ID")`: 한 행을 찾는 기본키 1개입니다. 한 칸의 값으로 URL 키를 만듭니다.
4. `"USER_BUTTON_AUTH_ID"`: 신규 등록 때 서버가 USER_BUTTON_AUTH_ID 번호를 발급합니다. 이 칸을 클라이언트 편집 필드로 아래에 적지 않습니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**105줄**

```java
            new Column("USER_ID", Kind.NUMBER, 0, true, null),
```

`USER_ID` 필드는 역할 또는 개인 예외를 적용할 사용자 번호입니다. 실제 값 예는 `USER_ID=2`입니다.

다섯 인자 중 첫 번째는 `"USER_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 USER_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**106줄**

```java
            new Column("MENU_ID", Kind.NUMBER, 0, true, null),
```

`MENU_ID` 필드는 권한이나 버튼을 연결할 메뉴 번호입니다. 실제 값 예는 `MENU_ID=1010`입니다.

다섯 인자 중 첫 번째는 `"MENU_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 MENU_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**107줄**

```java
            new Column("BUTTON_ID", Kind.NUMBER, 0, true, null),
```

`BUTTON_ID` 필드는 해당 화면에 연결할 버튼 번호입니다. 실제 값 예는 `BUTTON_ID=1`입니다.

다섯 인자 중 첫 번째는 `"BUTTON_ID"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `0`입니다. 이 숫자는 별도의 정밀도 제한을 추가하지 않는다는 뜻이며, long 범위 검사는 적용됩니다. 이름이 _ID로 끝나므로 값을 보내면 양수여야 합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 BUTTON_ID 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**108줄**

```java
            new Column("ACCESS_YN", Kind.TEXT, 1, false, null),
```

`ACCESS_YN` 필드는 메뉴별 버튼 허용 여부 표시입니다. 실제 값 예는 `ACCESS_YN="Y"`입니다.

다섯 인자 중 첫 번째는 `"ACCESS_YN"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `1`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 1바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 ACCESS_YN 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

개인 예외에서 Y는 허용 추가, N은 제거, null은 역할 결과 유지입니다.

**109줄**

```java
            new Column("VALID_FROM", Kind.TIME, 0, true, "@now"),
```

`VALID_FROM` 필드는 이 연결이나 예외가 시작되는 시각입니다. 실제 값 예는 `VALID_FROM="2030-01-02T09:00:00"`입니다.

다섯 인자 중 첫 번째는 `"VALID_FROM"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TIME`는 시간으로 읽는 칸입니다. 세 번째 `0`은 TEXT 같은 바이트 길이를 검사하지 않는다는 뜻입니다. 위 ISO 지역 날짜·시간 예를 Timestamp로 변환합니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"@now"`는 신규 등록 시 생략하면 mapper.now()에서 DB 현재 시각을 읽어 넣으라는 특수 기본값입니다. 시작 시간을 직접 위 예처럼 보내면 그 값을 사용합니다. 따옴표 안의 @now는 문자열이며 @Valid 같은 Java 어노테이션이 아닙니다.

현재 SQL 권한 조회는 시작 시각을 포함하며 미래의 시작이면 아직 활성 권한에 넣지 않습니다.

**110줄**

```java
            new Column("VALID_TO", Kind.TIME, 0, false, null),
```

`VALID_TO` 필드는 이 연결이나 예외가 끝나는 시각입니다. 실제 값 예는 `VALID_TO="2030-12-31T18:00:00"`입니다.

다섯 인자 중 첫 번째는 `"VALID_TO"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TIME`는 시간으로 읽는 칸입니다. 세 번째 `0`은 TEXT 같은 바이트 길이를 검사하지 않는다는 뜻입니다. 위 ISO 지역 날짜·시간 예를 Timestamp로 변환합니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 VALID_TO 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

종료를 보내면 시작보다 빠르지 않아야 합니다. null이면 종료 시각이 정해지지 않은 기간이고 현재 구현은 종료 시각도 포함합니다.

**111줄**

```java
            new Column("REASON", Kind.TEXT, 500, false, null))),
```

`REASON` 필드는 역할 부여나 개인 예외의 이유입니다. 실제 값 예는 `REASON="한시적 업무 지원"`입니다.

다섯 인자 중 첫 번째는 `"REASON"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `500`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 500바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 REASON 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.18 code-groups 규칙

**112줄**

```java
        new MasterResource("code-groups", "TB_COMMON_CODE_GROUP", List.of("CODE_GROUP"), null, List.of(
```

공통코드 분류의 규칙입니다. 새 분류 EDU_STAGE를 만들면 해당 분류 이름이 그대로 기본키가 됩니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"code-groups"`: /api/common/admin/code-groups의 자료 이름입니다. MasterResource.named("code-groups")이 찾는 값입니다.
2. `"TB_COMMON_CODE_GROUP"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("CODE_GROUP")`: 한 행을 찾는 기본키 1개입니다. 한 칸의 값으로 URL 키를 만듭니다.
4. `null`: 서버가 별도의 숫자 기본키를 만들지 않습니다. 아래 입력 칸에 적힌 문자열 또는 복합키를 요청에서 제공합니다. null은 기본키가 없다는 뜻이 아닙니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**113줄**

```java
            new Column("CODE_GROUP", Kind.TEXT, 50, true, null),
```

`CODE_GROUP` 필드는 공통코드를 묶는 분류 이름표입니다. 실제 값 예는 `CODE_GROUP="USER_STATUS"`입니다.

다섯 인자 중 첫 번째는 `"CODE_GROUP"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `50`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 50바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 CODE_GROUP 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**114줄**

```java
            new Column("DESCRIPTION", Kind.TEXT, 500, false, null))),
```

`DESCRIPTION` 필드는 관리자가 참고할 설명입니다. 실제 값 예는 `DESCRIPTION="설명용 기준자료"`입니다.

다섯 인자 중 첫 번째는 `"DESCRIPTION"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `500`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 500바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 DESCRIPTION 필드를 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 마지막 쉼표는 바깥 RESOURCES 목록에 다음 resource가 이어진다는 뜻입니다.

#### 6.1.A.19 codes 규칙

**115줄**

```java
        new MasterResource("codes", "TB_COMMON_CODE", List.of("CODE_GROUP", "CODE"), null, List.of(
```

공통코드 값의 규칙입니다. USER_STATUS~ACTIVE처럼 분류와 코드를 함께 써야 행 하나를 찾을 수 있습니다.

다섯 인자를 순서대로 읽으면 다음과 같습니다.

1. `"codes"`: /api/common/admin/codes의 자료 이름입니다. MasterResource.named("codes")이 찾는 값입니다.
2. `"TB_COMMON_CODE"`: 이 자료를 저장하고 읽을 실제 DB 표입니다. 클라이언트가 정하는 표 이름이 아닙니다.
3. `List.of("CODE_GROUP", "CODE")`: 한 행을 찾는 기본키 2개입니다. 목록에 적힌 순서대로 ~로 이어 붙여 URL 키를 만듭니다.
4. `null`: 서버가 별도의 숫자 기본키를 만들지 않습니다. 아래 입력 칸에 적힌 문자열 또는 복합키를 요청에서 제공합니다. null은 기본키가 없다는 뜻이 아닙니다.
5. 마지막 `List.of(`: 다음 줄들에 이 자료의 편집 칸 규칙이 이어집니다. 첫 번째 List.of는 키 목록이고 이 두 번째 List.of는 Column 목록이므로 서로 다른 목록입니다.

**116줄**

```java
            new Column("CODE_GROUP", Kind.TEXT, 50, true, null),
```

`CODE_GROUP` 필드는 공통코드를 묶는 분류 이름표입니다. 실제 값 예는 `CODE_GROUP="USER_STATUS"`입니다.

다섯 인자 중 첫 번째는 `"CODE_GROUP"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `50`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 50바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 CODE_GROUP 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**117줄**

```java
            new Column("CODE", Kind.TEXT, 50, true, null),
```

`CODE` 필드는 공통코드 분류 안에서 구분하는 값입니다. 실제 값 예는 `CODE="ACTIVE"`입니다.

다섯 인자 중 첫 번째는 `"CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `50`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 50바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

**118줄**

```java
            new Column("CODE_NAME_MSG_CODE", Kind.TEXT, 100, true, null),
```

`CODE_NAME_MSG_CODE` 필드는 공통코드의 표시 이름을 번역할 메시지 코드입니다. 실제 값 예는 `CODE_NAME_MSG_CODE="COD_USER_STATUS_ACTIVE"`입니다.

다섯 인자 중 첫 번째는 `"CODE_NAME_MSG_CODE"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `100`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 100바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 CODE_NAME_MSG_CODE 필드를 빼면 normalize()가 null을 검사하다가 필수값 오류로 거절합니다.

화면용 codes() 조회는 이 메시지 코드를 MessageService.text()에 보내 한국어·영어 이름을 얻습니다. 코드 ACTIVE 자체를 번역문으로 바꾸는 것은 아닙니다.

**119줄**

```java
            new Column("SORT_ORDER", Kind.NUMBER, 5, true, "0"),
```

`SORT_ORDER` 필드는 목록을 보여 줄 정렬 순서 숫자입니다. 실제 값 예는 `SORT_ORDER=10`입니다.

다섯 인자 중 첫 번째는 `"SORT_ORDER"`입니다. 이 값은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.NUMBER`는 소수점 없는 정수로 읽는 칸입니다. 세 번째 인자는 `5`입니다. 이 숫자는 현재 convert()에서 최대 5자리 정밀도를 검사한다는 뜻입니다.  네 번째 `true`는 필수 칸이라는 뜻입니다.

다섯 번째 `"0"`는 신규 등록에서 생략하면 SORT_ORDER=0으로 채운다는 뜻입니다. 정의에는 "0"라는 문자열로 적었지만 convert()가 숫자 0로 바꿉니다.

**120줄**

```java
            new Column("EXT_VALUE1", Kind.TEXT, 500, false, null),
```

`EXT_VALUE1`은 공통코드의 첫 번째 선택 확장값입니다. 실제 값 예는 `EXT_VALUE1="추가 설명 1"`입니다.

다섯 인자 중 첫 값 `"EXT_VALUE1"`은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `500`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 500바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 EXT_VALUE1을 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

**121줄**

```java
            new Column("EXT_VALUE2", Kind.TEXT, 500, false, null),
```

`EXT_VALUE2`은 공통코드의 두 번째 선택 확장값입니다. 실제 값 예는 `EXT_VALUE2="추가 설명 2"`입니다.

다섯 인자 중 첫 값 `"EXT_VALUE2"`은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `500`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 500바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 EXT_VALUE2을 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

**122줄**

```java
            new Column("EXT_VALUE3", Kind.TEXT, 500, false, null)))
```

`EXT_VALUE3`은 공통코드의 세 번째 선택 확장값입니다. 실제 값 예는 `EXT_VALUE3="추가 설명 3"`입니다.

다섯 인자 중 첫 값 `"EXT_VALUE3"`은 요청 values에서 사용할 정확한 대문자 필드 이름입니다. 두 번째 `Kind.TEXT`는 문자열이어야 한다는 뜻입니다. 세 번째 인자는 `500`입니다. 이 숫자는 UTF-8로 바꾼 길이의 최대 500바이트입니다. ASCII 영문·숫자는 한 글자가 1바이트이고, 예를 들어 한글 `가`는 UTF-8에서 3바이트입니다. 네 번째 `false`는 값을 비울 수 있는 선택 칸이라는 뜻입니다.

다섯 번째 `null`은 생략 시 대신 채울 기본값이 없다는 뜻입니다. 신규 등록에서 EXT_VALUE3을 빼면 null로 두고 이 칸의 기본 형식 검사는 통과합니다. 단, 다른 연결 관계 검사에 따라 추가 조건이 있을 수 있습니다.

줄 끝의 `)))`는 현재 Column 생성, 그 자료의 Column 목록 List.of, 그 자료의 MasterResource 생성을 차례로 닫습니다. 여기가 마지막 resource이므로 다음 resource를 구분하는 쉼표는 없습니다.

#### 6.1.A.20 목록과 클래스 닫기: 123~124줄

**123줄**

```java
    );
```

이 `);`는 11줄에서 시작한 가장 바깥 List.of(...) 호출을 닫고 RESOURCES 선언 문장을 끝냅니다. 이제 languages부터 codes까지 18개 정의가 한 목록에 들어 있습니다. 여기서 세미콜론은 SQL 종료 표시가 아니라 Java 문장 종료 표시입니다.

**124줄**

```java
}
```

이 `}`는 6줄에서 시작한 MasterCatalog 클래스의 내용을 닫습니다. 등록 메서드나 SQL 실행 코드가 없는 이유는 이 파일이 실제 처리를 하는 Service가 아니라 허용 규칙을 담는 파일이기 때문입니다.

#### 6.1.B 회사 규칙 30~32줄이 실제 POST 값에 적용되는 순간

이제 전체 줄 설명을 회사 요청 하나에 연결합니다.

```json
{
  "values": {
    "COMPANY_CODE": "CAT01",
    "COMPANY_NAME": "배움 회사"
  }
}
```

1. JSON 변환과 @Valid 검사가 끝나 create()가 호출됩니다. body.values에는 CAT01과 배움 회사가 있습니다.
2. MasterServiceImpl.create()가 MasterResource.named("companies")를 호출합니다. named()가 RESOURCES에서 name이 companies인 **30줄 정의**를 찾습니다.
3. normalize()가 그 정의의 편집 columns를 읽습니다. **31줄**의 COMPANY_CODE 규칙은 입력 CAT01을 TEXT로 받고 UTF-8 20바이트 이내인지 확인합니다.
4. **32줄**의 COMPANY_NAME 규칙은 배움 회사가 문자열이며 UTF-8 100바이트 이내이고 null이나 공백뿐이 아닌지 확인합니다.
5. 생성자가 추가한 USE_YN 규칙의 기본값 Y도 채웁니다.
6. **30줄의 네 번째 인자**가 COMPANY_ID이므로 mapper.nextId()로 새 번호를 받아 값을 추가합니다.
7. validate()를 거쳐 mapper.insert()에 넘깁니다. **30줄의 두 번째 인자** TB_COMPANY가 실제 SQL 표 이름이고, 입력값은 parameter로 전달됩니다.
8. 저장한 행을 다시 읽을 때 **30줄의 세 번째 인자** COMPANY_ID가 검색 키가 됩니다.

한 줄의 정의가 저장 값을 직접 갖고 있지 않아도, 입력 검사·번호 생성·SQL 표 선택·행 검색에 각각 사용되는 이유입니다.

**다른 두 입력을 비교해 봅시다.**

```json
{ "values": { "COMPANY_CODE": "CAT02" } }
```

Map 크기 1이므로 @NotNull·@Size 기본 검사는 통과합니다. 하지만 32줄 COMPANY_NAME은 required=true이고 defaultValue=null입니다. normalize()가 생략된 이름을 null로 검사할 때 convert()에서 실패합니다.

```json
{
  "values": {
    "COMPANY_CODE": "ABCDEFGHIJKLMNOPQRSTU",
    "COMPANY_NAME": "길이 검사 회사"
  }
}
```

COMPANY_CODE의 영어 글자는 21개, UTF-8 길이도 21바이트입니다. 31줄의 허용 길이 20보다 크므로 convert()에서 실패합니다. @Size는 이 문자열의 21글자를 검사한 것이 아니라 values의 항목 2개를 검사하여 이미 통과했습니다.

반대로 같은 정상 값으로 사이트를 등록하려고 resource를 sites로 바꾸면 회사 규칙을 재사용하는 것이 아니라 33줄의 sites 정의를 찾습니다. 사이트는 COMPANY_ID·SITE_CODE·SITE_NAME이 필요하므로 회사용 JSON을 그대로 보내서는 안 됩니다.

### 6.2 normalize(): 들어온 자료를 정돈한다

`normalize(resource, input, create)`는 다음 일을 합니다.

1. 입력 Map이 있고 1~40개의 항목인지 확인합니다. HTTP를 통하지 않는 직접 호출도 검사합니다.
2. resource의 편집 허용 필드를 이름별로 모읍니다.
3. 사용자가 보낸 각 필드가 허용 목록에 있는지 확인합니다.
4. 각 값을 `convert()`에 보내 형식을 맞춥니다.
5. 신규 등록이면 빠진 항목에 기본값을 채웁니다.

필드 이름은 현재 정확한 대문자 컬럼 이름을 사용합니다. `COMPANY_NAME` 대신 `companyName`을 보내면 현재 관리 API는 자동 변환하지 않고 거절합니다. record 결과에서 `userId`처럼 보이는 이름과 이 Map 입력 규칙을 구분하세요.

등록 기본값의 예는 `USE_YN=Y`, `SORT_ORDER=0`, `USER_STATUS=ACTIVE`, `VALID_FROM=@now`입니다. `@now`는 실제 글자 그대로 저장하지 않고 `MasterMapper.now()`로 DB 시간을 받아 채웁니다.

수정에서는 빠진 필드를 기본값으로 채우지 않습니다. 보낸 값만 바꿉니다. 선택 항목에 명시적으로 `null`을 보내면 그 값을 비울 수 있고, 필수 항목은 비울 수 없습니다.

### 6.3 convert(): 값의 종류를 확인한다

`convert(Column column, Object raw)`는 한 칸씩 검사합니다.

| 종류 | 처리 | 예 |
|---|---|---|
| TEXT | 실제 문자열인지, UTF-8 바이트 길이가 허용 길이 이내인지 | 이름에 숫자 객체를 보내면 거절 |
| NUMBER | 정수로 정확하게 표현되는지, long 범위인지, 지정된 자릿수 이내인지 | 3.5를 정수 칸에 보내면 거절 |
| DECIMAL | BigDecimal로 읽고 scale·precision 범위인지, 자동 반올림 없이 저장할 수 있는지 | 계수 0.001은 허용하고 소수 18자리를 넘는 계수는 거절 |
| TIME | Timestamp 또는 ISO 형식의 지역 날짜·시간으로 바꿀 수 있는지 | `2030-01-02T09:00:00` |

값이 `null`이거나 공백뿐인 문자열이면 선택 칸은 `null`, 필수 칸은 오류입니다. 일반 문자열을 자동으로 앞뒤 공백 제거해서 저장하는 코드는 아닙니다. 공백뿐인지 확인하는 것과 문자열 전체를 trim하는 것은 다릅니다.

이름이 `_YN`으로 끝나는 칸은 `Y`와 `N`만 허용합니다. `true`, `yes`, 소문자 `y`로 보내지 않습니다. `_ID`로 끝나는 숫자 칸은 양수여야 합니다.

길이는 현재 Oracle DDL의 VARCHAR2 바이트 길이를 기준으로 검사합니다. 한글 한 글자와 영어 한 글자가 같은 바이트 수를 쓰지 않으므로, “20글자 허용”과 “20바이트 허용”은 다릅니다.

시간 예의 `2030-01-02T09:00:00`에는 시간대 정보가 없습니다. 현재 구현은 `LocalDateTime`을 파싱하므로 `Z`나 `+09:00`을 붙인 다른 형식이 자동으로 받아들여진다고 가정하지 마세요.

### 6.4 validate(): 전체 자료의 규칙을 확인한다

`validate()`는 값 하나뿐 아니라 연결 관계도 확인합니다. 수정에서는 기존 값과 새 값을 합친 자료를 받습니다.

공통적으로 필드 값을 다시 검사하고, 키 문자열의 `/`·`~`를 막습니다. `ACCESS_LEVEL`과 `DATA_SCOPE`가 있으면 해당 enum의 정확한 이름인지 확인합니다. `HTTP_STATUS`는 100~599, `ARG_COUNT`는 0~10 범위여야 합니다.

이후 resource에 따라 아래 메서드를 호출합니다.

| 자료 종류 | 추가 검사 메서드 |
|---|---|
| 사용자-ROLE, 사용자 메뉴·버튼 예외 | `validatePeriod()` |
| 부서, 메뉴 | `lockHierarchy()` 후 `validateHierarchy()` |
| 사업장, 부서, 사용자 | `validateOrganization()` |
| 사용자 | 상태 공통코드와 이메일 검사 |
| 언어 | `lockHierarchy()` 후 `validateLanguage()` |
| 메시지 번역 | `validateTranslation()` |

모든 DB CHECK나 UNIQUE 조건을 Java가 동일하게 복제한 것은 아닙니다. 예를 들어 ROLE_TYPE, MENU_TYPE, 일부 단계 조건은 DB의 제약도 적용됩니다. DB 제약 위반은 공통 `ERR_CONFLICT`로 응답할 수 있습니다.

### 6.5 validateOrganization(): 회사와 소속이 맞는가

A회사, A본사, A개발부를 연결한다고 합시다. 서비스는 회사가 존재하고 활성인지 확인합니다. 지정한 사업장과 부서도 존재하고 활성인지 확인하며 그 회사 번호가 같은지 비교합니다.

사용자의 부서에 사업장이 정해져 있으면 사용자에게 지정한 사업장도 그 부서와 같아야 합니다. 부서 번호만 맞고 사업장 번호가 엉뚱하면 거절합니다.

사업장이나 부서 자체를 신규 생성할 때는 그 행의 새 기본키를 기존 참조 행인 것처럼 찾으면 안 됩니다. 그래서 자신의 identity 필드는 참조 검사에서 건너뜁니다.

`same(left, right)`는 이 비교를 돕습니다. DB에서 받은 `100`이 어떤 숫자 객체이고 입력의 `100`이 다른 숫자 객체여도 같은 숫자로 비교합니다. null도 양쪽이 모두 없을 때만 같다고 처리합니다.

### 6.6 validateHierarchy(): 부모가 자기 자식이 되지 않게 한다

부서 A의 부모를 B로 정했는데 B의 부모가 A라면 A → B → A로 끝없이 돌아갑니다. 메뉴도 같은 문제가 생길 수 있습니다.

`validateHierarchy()`는 자신부터 방문한 번호를 `seen`에 기록하고 부모를 계속 따라갑니다. 이미 방문한 번호를 다시 만나면 오류입니다. 부모는 활성이어야 하고, 부서 부모는 회사와 사업장도 같아야 합니다. `idToken()`은 숫자 번호를 일정한 문자열 형태로 만들어 중복을 비교합니다.

동시에 두 사람이 A의 부모를 B, B의 부모를 A로 바꾸면 각각의 검사 순간에는 순환이 안 보일 수 있습니다. 그래서 먼저 `MasterMapper.lockHierarchy()`로 표의 변경을 잠시 묶습니다. Oracle의 SQL은 `LOCK TABLE ... IN SHARE ROW EXCLUSIVE MODE`입니다. 단순히 화면을 보는 모든 시간을 잠그는 것이 아니라 해당 저장 transaction 동안 검사와 변경을 보호합니다.

현재 코드는 자식 행을 자동 비활성화하거나 부모 단계에 맞춰 LEVEL 값을 자동 조정하지 않습니다. 순환과 연결 정합성을 확인하는 기능입니다.

### 6.7 validatePeriod(): 같은 예외가 같은 시간에 두 개 생기지 않게 한다

사용자 2에게 프로젝트 화면을 금지하는 예외와 허용하는 예외가 같은 시간에 동시에 있으면 어떤 행을 믿어야 할지 애매합니다. 행이 읽힌 순서에 따라 답이 달라져서는 안 됩니다.

`validatePeriod()`는 다음을 확인합니다.

1. 시작 시간을 필수로 받고 종료 시간이 시작보다 빠르지 않은지 검사합니다.
2. 비활성 행이라면 활성 기간 겹침 검사를 건너뜁니다.
3. `MasterMapper.lockUser()`로 해당 사용자 행을 잠급니다.
4. 같은 사용자+역할, 같은 사용자+메뉴, 또는 같은 사용자+메뉴+버튼을 기준으로 모읍니다.
5. `MasterMapper.overlaps()`로 활성 기간이 겹치는 행이 있는지 확인합니다.
6. 겹치면 `ERR_CONFLICT`입니다. 수정 중인 자기 행은 비교에서 제외합니다.

사용자 행을 잠그는 이유는 같은 사용자의 권한을 두 요청이 동시에 저장해 서로를 못 보고 통과하는 일을 줄이기 위해서입니다. 운영 데이터를 SQL로 직접 넣으면 이 Java 검사를 지나지 않습니다. DB가 모든 가능한 기간 겹침을 대신 검사한다고 가정하지 마세요.

직접 입력 등으로 이미 중복된 활성 예외가 있으면 `PermissionServiceImpl.get()`도 이를 감지하고 `ERR_CONFLICT`로 멈춥니다. 첫 번째 행을 임의로 고르는 방식이 아닙니다.

### 6.8 validateLanguage(): 언어 코드와 대체 경로를 확인한다

`validateLanguage()`는 BCP-47 계열의 표준 언어 표기를 Java의 `Locale.Builder`로 확인합니다. 관리 입력은 `zh-CN`처럼 정규 표기를 사용해야 합니다. 조회 시의 대소문자 유연 처리와 관리 등록의 표준 표기 검사는 다릅니다.

기본 언어이면서 비활성인 상태는 막습니다. fallback 언어를 계속 따라가며 같은 언어를 다시 만나면 순환으로 거절합니다. 없는 fallback은 `find()`와 DB FK가 막습니다.

현재 관리 검사 자체가 fallback 행에 반드시 `USE_YN='Y'`를 요구하는 것은 아닙니다. 읽을 때 `MessageServiceImpl.get()`은 지원하는 활성 언어 목록을 기준으로 대체 경로를 선택하고, 사용할 수 없으면 활성 기본 언어를 찾습니다.

DB의 unique index는 “활성 기본 언어가 두 개”인 상태를 막습니다. “언제나 정확히 하나가 있어야 한다”를 보장하는 것은 아닙니다. 기본값 표시가 전부 없어지면 메시지 서비스의 안전 기본값 `ko`가 사용될 수 있습니다.

### 6.9 validateTranslation(): 문장 속 빈칸을 확인한다

`MESSAGE_TEXT`에 `안녕하세요, {0}님.`이라고 적으면 `{0}`은 나중에 넣을 첫 번째 인자 자리입니다. 인자는 0부터 번호를 셉니다. 인자 하나를 허용하려면 `TB_MESSAGE.ARG_COUNT=1`입니다.

`validateTranslation()`은 메시지 master를 찾고, `MessageFormat`이 문장 형식을 읽을 수 있는지 확인합니다. 문장의 `{숫자}` 인덱스가 `ARG_COUNT` 범위를 넘는지도 검사합니다. ARG_COUNT가 1인데 `{1}`을 쓰면 두 번째 인자를 요구하므로 거절합니다.

이 검사는 번역문을 저장하는 시점의 검사입니다. Message master의 ARG_COUNT를 바꿀 때 기존 모든 번역문을 자동으로 다시 쓰거나 재검증하는 루프는 현재 없습니다.

### 6.10 사용자 인증 관련 칸은 일반 관리에서 제외한다

`TB_USER`에는 Level 2가 사용할 암호 관련 칸도 원래 DDL에 있습니다. 현재 일반 관리 입력과 조회는 다음 칸을 제외합니다.

```text
PASSWORD_HASH, FAILED_LOGIN_COUNT, LOCKED_UNTIL,
PASSWORD_CHANGED_AT, MUST_CHANGE_PASSWORD_YN, LAST_LOGIN_AT, AUTH_TYPE
```

`MasterCatalog`가 편집 목록에 넣지 않고, `MasterResource.getProjection()`도 결과 목록에 넣지 않습니다. 그래서 사용자 기본정보를 관리하면서 비밀번호 해시가 같이 노출되지 않습니다. `AUTH_TYPE` 등 실제 인증 데이터의 관리는 향후 Level 2 담당입니다.

### 6.11 SQL의 ${}와 #{}를 구분하기

`MasterMapper.xml`에는 `${resource.table}` 같은 부분이 있습니다. 이것은 테이블·컬럼 이름을 SQL 문장에 넣는 자리입니다. 이름은 `MasterCatalog`의 고정 목록과 서비스가 검증한 필드 목록에서만 가져옵니다.

`#{value}`는 실제 저장 값입니다. 이름에 따옴표가 들어가도 SQL 명령 일부로 해석되지 않게 DB parameter로 전달합니다. 이를 바인딩이라고 합니다.

따라서 이 공통 Mapper가 안전하게 동작하려면 테이블·컬럼 이름을 계속 내부 허용 목록에서만 공급해야 합니다. 향후 요청의 `table` 문자열을 그대로 `${}`에 연결하는 식으로 확장하면 현재의 보호가 깨집니다.

### 6.12 transaction은 중간 실패를 되돌린다

`MasterServiceImpl`은 기본적으로 `@Transactional(readOnly=true)`이고, `create()`, `update()`, `deactivate()`에는 쓰기용 `@Transactional`이 따로 있습니다.

예를 들어 저장 후 다시 조회하는 과정에서 예외가 나면 해당 DB 변경 묶음을 되돌리는 것이 transaction의 역할입니다. 잠금도 이 묶음이 끝날 때 풀립니다. 내부 private 검사 메서드마다 별도 transaction을 새로 만들지 않고 바깥 public 저장 메서드의 묶음 안에서 작동합니다.

읽기 서비스의 readOnly는 “이 작업은 조회 중심”이라는 안내입니다. 이것을 사용자 권한 검사로 이해하면 안 됩니다. 읽기 담당에게 쓰기 권한을 막는 보안 정책과 transaction 설정은 다른 일입니다.

<a id="chapter-7"></a>
## 7. 누가 어떤 기능을 사용할 수 있는가

### 7.1 권한에는 세 가지 질문이 있다

“프로젝트 관리 화면을 읽을 수 있는가”, “그 화면에서 삭제를 실행할 수 있는가”, “어느 회사·부서의 자료까지 다룰 수 있는가”는 서로 다른 질문입니다.

현재 코드의 연결은 다음과 같습니다.

```text
메뉴 권한 수준 → AccessLevel.java
자료의 범위   → DataScope.java
화면의 버튼   → TB_MENU_BUTTON + 버튼 권한 표
실제 계산     → PermissionServiceImpl.java
DB 조회       → PermissionMapper.java + PermissionMapper.xml
API 앞 검사   → CommonWebConfiguration.java의 preHandle()
```

### 7.2 AccessLevel.allows(): 권한 수준을 검사한다

`AccessLevel.java`에는 `NONE`, `READ_PARTIAL`, `READ_ALL`, `WRITE_PARTIAL`, `WRITE_ALL`이 있습니다. `allows(required)`는 가진 수준으로 요청한 수준을 수행할 수 있는지 계산합니다.

단순히 enum의 순서가 높다고 모두 허용하지 않습니다. 내부의 숫자 mask로 포함된 능력을 확인합니다.

| 가진 수준 | READ_PARTIAL 요청 | READ_ALL 요청 | WRITE_PARTIAL 요청 | WRITE_ALL 요청 |
|---|---|---|---|---|
| NONE | 불가 | 불가 | 불가 | 불가 |
| READ_PARTIAL | 가능 | 불가 | 불가 | 불가 |
| READ_ALL | 가능 | 가능 | 불가 | 불가 |
| WRITE_PARTIAL | 가능 | 불가 | 가능 | 불가 |
| WRITE_ALL | 가능 | 가능 | 가능 | 가능 |

`WRITE_PARTIAL`은 `READ_ALL`을 자동 포함하지 않는다는 점을 보세요. 내부 mask 값은 각각 0, 1, 3, 5, 15입니다. `allows()`는 required가 NONE이면 허용 요청으로 인정하지 않습니다.

PARTIAL과 ALL이 업무에서 정확히 어느 기능 차이를 뜻할지는 각 업무 API가 정해야 합니다. 현재 enum은 능력 포함 관계를 계산하지만 특정 화면의 필드나 행을 직접 가리지는 않습니다.

### 7.3 DataScope: 어느 자료까지인가

`DataScope.java`는 고정 범위 이름 네 개를 선언합니다.

| 값 | `requireData()`에서의 의미 |
|---|---|
| OWN | 대상 자료의 소유 사용자 번호가 나와 같아야 한다 |
| DEPT | 나에게 부서가 지정되어 있고 대상 부서가 그 부서와 같아야 한다 |
| SITE | 나에게 사업장이 지정되어 있고 대상 사업장이 그 사업장과 같아야 한다 |
| COMPANY | 대상 회사가 나의 회사와 같아야 한다 |

모든 범위에 앞서 같은 회사인지 검사합니다. OWN이라도 다른 회사 자료는 허용하지 않습니다. DEPT는 현재 정확히 같은 부서 번호를 뜻하며 하위 부서 전체를 자동 포함하는 코드는 없습니다. SITE는 정확히 같은 사업장 번호입니다.

### 7.4 권한과 범위를 꼭 짝으로 보관하는 이유

사용자에게 두 역할이 있다고 가정합니다.

- A역할: 회사 자료를 전체 조회할 수 있다 — `READ_ALL + COMPANY`.
- B역할: 자기 자료 일부를 수정할 수 있다 — `WRITE_PARTIAL + OWN`.

이 둘을 잘못 합쳐 `WRITE_PARTIAL + COMPANY`로 만들면 회사 전체를 고칠 수 있는 권한이 생겨 버립니다. 원래 어떤 역할도 그런 허용을 주지 않았습니다.

그래서 `PermissionModels.Grant(accessLevel, dataScope)`는 수준과 범위를 한 상자로 묶습니다. `PermissionServiceImpl.get()`은 한 메뉴에 여러 Grant를 유지합니다. `requireData()`와 `requireScope()`는 **같은 Grant 하나가 필요한 수준과 범위를 함께 만족하는지** 확인합니다.

두 역할에서 반쪽씩 떼어 새로운 더 큰 허용을 만드는 계산은 하지 않습니다. `CommonCoreIntegrationTests.roleUnionDoesNotWidenWriteScope()`가 이 부분을 확인합니다.

### 7.5 PermissionModels.java의 상자들

이 파일은 권한 계산에 쓰는 여러 record를 한 곳에 모았습니다.

| record | 무엇을 담는가 | 어디서 사용하는가 |
|---|---|---|
| `UserContext` | 사용자·회사·사업장·부서·언어 | `user()`, `RequestUserService.require()` |
| `Grant` | 권한 수준 + 자료 범위 | 최종 메뉴 권한 |
| `MenuGrant` | 메뉴 번호 + ROLE의 수준·범위 | `PermissionMapper.roleMenus()` 결과 |
| `MenuOverride` | 메뉴 번호 + 개인 예외, null 가능 | `PermissionMapper.userMenus()` 결과 |
| `ButtonGrant` | 메뉴 번호 + 버튼 번호 + 허용 Y/N/null | 버튼 합산과 개인 예외 |
| `MenuPermission` | 메뉴 번호 + 최종 Grant 목록 | `get()`, `requireMenu()` |
| `DataTarget` | 대상 자료의 회사·사업장·부서·소유자 | `requireData()` 입력 |
| `PermissionSnapshot` | 최종 메뉴 목록 + 버튼 목록 | 권한 조회 응답 |

`MenuPermission` 생성자는 Grant 목록을 복사합니다. 바깥에서 목록을 바꾸면서 계산 결과가 뒤늦게 변하는 일을 줄입니다. `MenuPermission.allows(level)`은 Grant 중 하나라도 그 수준을 허용하는지 확인합니다.

`PermissionSnapshot`이라는 이름은 “이번에 계산한 결과 묶음”이라는 뜻입니다. 현재 로그인 세션이나 Redis에 저장한 권한 캐시를 뜻하지 않습니다.

### 7.6 PermissionServiceImpl.get(): 최종 권한을 만드는 순서

`get(userId)`는 먼저 `user(userId)`로 현재 사용 가능한 사용자인지 확인합니다. 그다음 메뉴와 버튼 권한을 합칩니다.

**첫째, ROLE 메뉴 허용을 모읍니다.** `PermissionMapper.roleMenus()`가 활성 ROLE과 유효기간을 만족하는 연결을 읽습니다. NONE은 허용을 추가하지 않고, 나머지는 메뉴별 Grant 목록에 넣습니다.

**둘째, 사용자 메뉴 예외를 적용합니다.** `PermissionMapper.userMenus()`가 현재 유효한 예외를 읽습니다. 같은 메뉴에 활성 예외가 두 개면 ERR_CONFLICT입니다. 기존 Grant마다 예외의 null 아닌 값만 덮습니다.

예를 들어 역할은 `READ_ALL+OWN`, 개인 예외는 `ACCESS_LEVEL=null, DATA_SCOPE=DEPT`이면 `READ_ALL+DEPT`가 됩니다. 개인 예외 수준이 NONE이면 해당 메뉴의 허용이 제거됩니다.

ROLE 허용이 없는 메뉴라도 개인 예외에 명시적인 허용 수준이 있으면 부여할 수 있습니다. 바탕은 `NONE+OWN`이고, 지정한 값으로 바꿉니다. 둘 다 null이면 새로운 허용은 생기지 않습니다.

**셋째, ROLE 버튼 허용을 모읍니다.** `PermissionMapper.roleButtons()` 결과에서 Y만 추가합니다. 역할 하나의 N은 다른 역할의 Y를 지우지 않습니다. 기본 ROLE 정책은 허용의 합집합입니다.

**넷째, 사용자 버튼 예외를 적용합니다.** Y는 추가하고 N은 제거하고 null은 그대로 둡니다. 동일 메뉴·버튼에 활성 개인 예외가 둘이면 충돌입니다. private `key()`는 내부적으로 `메뉴번호:버튼번호` 문자열을 만들어 같은 짝을 구분합니다. URL의 복합키 `~`와는 다른 내부 표현입니다.

**다섯째, 최종 결과를 정리합니다.** 같은 메뉴의 중복 Grant를 없애고 빈 허용 메뉴를 제외합니다. 허용 메뉴 목록에 없는 메뉴의 버튼도 제외합니다. `PermissionSnapshot`을 돌려줍니다.

### 7.7 조회 SQL은 어떤 비활성 정보를 제외하는가

`PermissionMapper.xml`의 `roleMenus`, `userMenus`, `roleButtons`, `userButtons`는 단순히 권한 표만 읽지 않습니다.

ROLE 쪽은 역할 자체, 사용자-역할 연결, 연결 기간, 권한 행, 메뉴의 활성 여부를 확인합니다. 버튼 쪽은 버튼 master와 메뉴-버튼 배치가 활성인지도 확인합니다. 개인 예외는 자신의 활성 여부와 유효기간을 확인합니다.

따라서 메뉴-버튼 배치를 비활성화하면 예전 역할 버튼 허용이 남아 있어도 사용할 수 없습니다. 사용자나 회사가 비활성이면 시작 사용자 확인에서 막힙니다.

현재 권한 결과를 애플리케이션 캐시에 오래 저장하는 코드는 없습니다. 다시 조회할 때 DB의 상태를 반영합니다. 다만 동시에 진행 중인 transaction의 가시성까지 “모든 순간 절대 즉시”라고 단정하는 것은 아닙니다.

### 7.8 requireMenu(), requireButton(), requireScope(), requireData()

`require`로 시작하는 메서드는 “조건을 만족해야 계속 진행한다”는 뜻입니다. 결과 true/false를 주는 대신 실패하면 `ApiException`을 던집니다.

| 메서드 | 확인하는 질문 |
|---|---|
| `requireMenu(userId, menuCode, required)` | 이 메뉴가 활성이고 필요한 수준의 메뉴 권한이 있는가? |
| `requireButton(userId, menuCode, buttonCode, required)` | 메뉴 권한이 있고 해당 메뉴에 배치된 해당 버튼도 허용되었는가? |
| `requireScope(userId, menuCode, required, scopes...)` | 필요한 수준과 허용 범위를 같은 Grant가 만족하는가? |
| `requireData(userId, menuId, required, target)` | 실제 대상 자료의 회사·부서·사업장·소유자가 Grant 범위 안에 있는가? |

`menuId(menuCode)`와 `buttonId(menuId, buttonCode)`는 이름 코드에서 DB 번호를 찾는 Mapper 메서드입니다. 코드가 없거나 비활성이면 권한 검사에서 403으로 거절합니다.

`requireButton()`은 `requireMenu()`를 먼저 부르고 다시 버튼 결과를 확인합니다. 현재 여러 검사에서 `get()`을 다시 호출하므로 한 HTTP 요청에 DB 조회가 여러 번 일어날 수 있습니다. 요청 단위로 한 번 계산해 공유하는 최적화는 아직 구현하지 않았습니다.

### 7.9 DataTarget은 서버가 확인한 실제 자료여야 한다

업무 Service가 어떤 자료를 수정하려 한다고 합시다. 먼저 DB에서 그 자료의 회사·부서·소유자를 읽고, 그 값으로 `DataTarget`을 만들어 `requireData()`에 전달해야 합니다.

화면이 “이 자료는 제 소유입니다”라고 보낸 번호를 그대로 믿으면 안 됩니다. 그러면 다른 사람 자료에 자신의 번호를 적어 권한을 통과할 수 있습니다. `DataTarget`은 신뢰할 수 있는 서버 조회 결과에서 만들어야 합니다.

아래는 **향후 업무에서 사용할 호출 방법의 예시**입니다. 현재 프로젝트에 주문 Service나 이 자료 조회 코드는 없습니다.

```java
// target은 서버가 DB에서 확인한 실제 자료의 소속과 소유자 정보여야 한다.
permissionService.requireData(userId, menuId, AccessLevel.WRITE_PARTIAL, target);
// 통과한 뒤 해당 업무의 수정 작업을 수행한다.
```

현재 공통 관리 API가 각 행에 `requireData()`를 자동 적용하는 구조는 아닙니다. 목록 자료의 범위 제한도 업무 SQL에 직접 반영해야 합니다. 이 메서드를 제공한 것과 모든 미래 업무 API가 이미 보호된 것은 다릅니다.

<a id="permission-annotation-examples"></a>
### 7.10 RequiresPermission과 preHandle()의 연결

#### 7.10.1 어노테이션은 “어떤 규칙을 적용할지 적은 표지”다

수영장 입구에 “이 방은 수영장 입장권 필요”, 도서관 자료실에 “이 방은 자료 열람증 필요”라는 표지가 있다고 생각해 봅시다. 표지는 필요한 조건을 적고, 담당 직원이 표지를 읽어 검사합니다.

우리 코드에서는 @RequiresPermission이 표지이고 preHandle()이 표지를 읽는 담당입니다. `PermissionServiceImpl`은 실제로 가지고 있는 허용을 계산하는 담당입니다.

```text
@RequiresPermission          → 이 API가 요구하는 조건을 적음
preHandle()                  → 선택된 API의 조건을 읽음
RequestUserService.require() → 누구인지 확인함
PermissionServiceImpl        → 그 사람에게 필요한 허용이 있는지 검사함
```

Spring의 @PostMapping은 “어떤 요청을 이 메서드에 보낼까”를 정합니다. 우리가 만든 @RequiresPermission은 “그 요청을 실행하려면 무엇을 허용받아야 하나”를 정합니다. 두 표시는 질문 자체가 다릅니다.

#### 7.10.2 왜 권한 어노테이션을 직접 만들었는가

현재 회사·부서·사용자·단위·통화 등 21개 기준정보의 공통 관리에는 조회·신규·저장·삭제 권한이 필요합니다. 각 Controller 메서드 본문에 사용자 확인, 메뉴 확인, 버튼 확인, 범위 확인 코드를 반복하면 실수로 한 곳을 빼먹을 수 있습니다.

그래서 메서드 옆에는 “필요한 조건”을 적고, 실제 확인 순서는 공통 preHandle()에서 수행하게 했습니다. 개발자는 create() 선언을 보면서 BTN_NEW와 WRITE_ALL이 필요하다는 것을 확인할 수 있습니다.

이 방법을 사용했다고 실제 검사가 사라지는 것은 아닙니다. 검사 코드는 preHandle()과 PermissionServiceImpl에 있습니다. 어노테이션은 **규칙 전달**, 공통 검사기는 **규칙 실행**을 맡습니다.

어노테이션을 붙이지 않고 반복 호출하는 방법도 가능합니다. 현재 구현은 공통 관리자 Controller의 진입 조건을 한 곳에서 처리하려고 이 방식을 선택한 것입니다. 표지 자체가 Spring에 내장된 로그인 기능이거나 DB 권한 행을 생성하는 명령은 아닙니다.

#### 7.10.3 RequiresPermission.java를 한 줄씩 읽기

핵심 선언은 다음과 같습니다.

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    String menu() default "COMMON_ADMIN";
    String button() default "BTN_SEARCH";
    AccessLevel level() default AccessLevel.READ_ALL;
    DataScope[] scopes() default {};
}
```

| 줄 | 쉬운 설명과 실제 사용 |
|---|---|
| `@Target({METHOD, TYPE})` | 이 표시를 메서드나 클래스에 붙일 수 있다는 뜻. 실제로 create()와 MasterController 클래스에 붙어 있다 |
| `@Retention(RUNTIME)` | 실행할 때도 이 표시의 값을 읽을 수 있도록 남긴다는 뜻. preHandle()이 실행 중에 표시를 찾으므로 필요하다 |
| `public @interface RequiresPermission` | 새 어노테이션의 이름을 만든다. 일반 Service 인터페이스 선언과 달리 @interface이다 |
| `String menu() default "COMMON_ADMIN";` | menu 값을 생략하면 공통 관리 메뉴로 검사한다. create()에서 menu를 생략했으므로 COMMON_ADMIN이다 |
| `String button() default "BTN_SEARCH";` | button 값을 생략하면 조회 버튼이다. list()는 별도 메서드 표시가 없어서 클래스 기본 BTN_SEARCH로 검사한다 |
| `AccessLevel level() default READ_ALL;` | level을 생략하면 전체 조회 수준이다. create()는 WRITE_ALL을 명시해 바꾼다 |
| `DataScope[] scopes() default {};` | 허용할 범위 목록. 기본은 빈 목록이며, MasterController는 COMPANY를 지정한다 |
| `}` | 이 어노테이션 정의를 닫는다 |

`menu()` 같은 선언은 “코드를 실행해 메뉴를 찾는 메서드 본문”이 아닙니다. 어노테이션에서 설정할 항목 이름입니다. preHandle()이 표시를 읽어 얻은 rule에 `rule.menu()`를 호출하면 그 설정값을 꺼낼 수 있습니다.

#### 7.10.4 preHandle()은 누가 언제 호출하는가

**pre**는 “미리”, **handle**은 “요청을 처리한다”는 뜻입니다. preHandle()은 Spring MVC의 `HandlerInterceptor`가 제공하는 약속된 메서드입니다. 이 이름을 임의로 지은 일반 회사 처리 메서드가 아닙니다.

`CommonWebConfiguration.commonPermissionConfigurer()`가 MVC 구성 객체를 반환하고, 그 안의 `addInterceptors()`가 우리가 작성한 HandlerInterceptor를 Spring에 등록합니다. 적용 주소는 `/api/common/**`입니다.

실제 요청이 오면 Spring이 먼저 어떤 Controller 메서드가 담당할지 찾습니다. 그 다음 그 Controller 본문을 호출하기 전에 등록된 interceptor의 preHandle()을 호출합니다. **MasterController.create()가 직접 preHandle()을 호출하는 코드는 없습니다.** Spring이 등록된 약속에 따라 호출합니다.

```text
POST companies 수신
  → Spring이 MasterController.create()를 찾음
  → Spring이 interceptor.preHandle()을 호출함
  → 권한이 있으면 JSON 인자 준비와 검증
  → MasterController.create() 본문 실행
```

이 프로젝트의 preHandle()은 CommonWebConfiguration 안에서 `new HandlerInterceptor() { ... }`라는 익명 객체의 메서드로 작성되어 있습니다. 그래서 `CommonWebConfiguration.preHandle()`이라는 직접 public 메서드를 찾으면 없습니다. 정확한 위치는 **commonPermissionConfigurer() → addInterceptors() → 익명 HandlerInterceptor의 preHandle()**입니다.

#### 7.10.5 preHandle()의 세 가지 입력과 반환값

```java
public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler)
```

| 항목 | 뜻 | 회사 등록 요청의 예 |
|---|---|---|
| request | 현재 받은 요청 정보 | POST 방식, companies 경로, 헤더 등의 정보 |
| response | 요청자에게 돌려줄 응답 통로 | 검사 실패 시 오류가 전달될 응답. 현재 검사기는 직접 JSON을 쓰지 않는다 |
| handler | Spring이 선택한 처리 담당 | MasterController의 create()를 나타내는 HandlerMethod |
| boolean 반환값 | 정상 흐름을 이어갈 것인가 | true이면 다음 단계, false이면 interceptor가 정상 진행을 멈춤 |

현재 구현은 **성공하면 true, 실패하면 ApiException을 던지는 방식**입니다. 실패 경로에서 직접 return false를 쓰고 있는 것은 아닙니다. 예외가 전달되어 ApiExceptionHandler와 ApiErrorWriter가 공통 오류 응답을 만듭니다.

#### 7.10.6 preHandle()의 실제 코드를 따라 읽기

아래 코드는 현재 파일의 검사 부분입니다. 읽기 쉽게 들여쓰기를 정돈하고, `com.erpcom.erpcom.common.permission.DataScope`라는 전체 타입 이름을 `DataScope`로 짧게 표시했습니다. 메서드 전체를 독립 실행하는 예제는 아닙니다.

```java
if (handler instanceof HandlerMethod method) {
    var rule = AnnotatedElementUtils.findMergedAnnotation(
            method.getMethod(), RequiresPermission.class);
    var classRule = AnnotatedElementUtils.findMergedAnnotation(
            method.getBeanType(), RequiresPermission.class);
    if (rule == null) rule = classRule;
    if (rule != null) {
        long userId = users.require(request).userId();
        permissions.requireButton(userId, rule.menu(), rule.button(), rule.level());
        var scopes = rule.scopes().length > 0 ? rule.scopes()
                : classRule == null ? new DataScope[0] : classRule.scopes();
        if (scopes.length > 0)
            permissions.requireScope(userId, rule.menu(), rule.level(), scopes);
    }
}
return true;
```

1. `handler instanceof HandlerMethod method`: 선택된 담당이 Controller 메서드인지 확인하고 method라는 이름으로 읽습니다. 여기의 method는 HTTP POST 문자열이 아니라 **Java 메서드 정보 상자**입니다.
2. `method.getMethod()`: 실제 create() 같은 Java 메서드 정보를 꺼냅니다. `findMergedAnnotation()`이 그 메서드의 @RequiresPermission을 찾습니다.
3. `method.getBeanType()`: 그 메서드를 가진 클래스 정보를 꺼냅니다. MasterController 클래스 위의 표시를 찾습니다.
4. `if (rule == null) rule = classRule`: 메서드 표시가 없으면 클래스 표시를 기본 규칙으로 사용합니다.
5. `if (rule != null)`: 검사 규칙이 있을 때만 이 공통 권한 검사를 수행합니다.
6. `users.require(request).userId()`: Level 2가 검증해 연결한 사용자 번호와 활성 사용자 정보를 확인합니다. 실패하면 401 경로입니다.
7. `requireButton(...)`: rule에 적힌 메뉴·버튼·수준으로 검사합니다. 이 메서드가 메뉴 권한도 먼저 확인합니다. 권한 부족이면 403 경로입니다.
8. `rule.scopes().length > 0 ? ...`: 메서드 scopes가 있으면 그것을 쓰고, 비어 있으면 클래스 scopes를 사용합니다. 삼항 연산자는 “조건이 맞으면 앞의 값, 아니면 뒤의 값”을 고르는 표현입니다.
9. `requireScope(...)`: 적용할 범위가 있으면 같은 Grant가 필요한 수준과 범위를 만족하는지 확인합니다.
10. `return true`: 이 검사에서 예외 없이 끝났으므로 다음 처리 단계로 넘어가게 합니다.

메서드의 표시와 클래스의 모든 항목을 무조건 섞는 것은 아닙니다. 메서드 표시가 있으면 menu·button·level은 그 표시의 **명시값 또는 자체 default**를 사용합니다. scopes만 현재 코드가 별도로 클래스 값을 이어 받습니다.

#### 7.10.7 실제 예 1 — 회사 신규 등록의 BTN_NEW

실제 create()의 표시는 다음과 같습니다.

```java
@RequiresPermission(button = "BTN_NEW", level = AccessLevel.WRITE_ALL)
```

클래스에는 `@RequiresPermission(scopes=DataScope.COMPANY)`가 있습니다. POST `/api/common/admin/companies`를 처리할 때 최종 요구값은 다음과 같습니다.

```text
menu   = COMMON_ADMIN   ← 메서드 표시에서 생략했으므로 그 표시의 default
button = BTN_NEW        ← 메서드에서 명시
level  = WRITE_ALL      ← 메서드에서 명시
scopes = [COMPANY]      ← 메서드 목록이 비어서 클래스 값 적용
```

검증된 사용자 1이 COMMON_ADMIN의 WRITE_ALL+COMPANY Grant와 BTN_NEW 허용을 가지고 있다고 가정하면 preHandle()이 true로 끝나고 create() 본문을 실행할 수 있습니다. 여기서 사용자 1은 설명·테스트상의 번호이며 실제 기본 계정이 있다는 뜻은 아닙니다.

반대로 같은 사람이 메뉴 수준은 WRITE_ALL인데 BTN_NEW의 개인 예외가 N이면 requireButton()에서 거절합니다. 이름이 올바른 회사 JSON이라도 저장하지 못합니다. 메뉴·버튼은 있지만 범위가 OWN이면 requireScope()에서 거절합니다.

#### 7.10.8 실제 예 2 — 회사 이름 수정의 BTN_SAVE

실제 update()의 표시는 다음과 같습니다.

```java
@RequiresPermission(button = "BTN_SAVE", level = AccessLevel.WRITE_ALL)
```

PUT `/api/common/admin/companies/100`에 아래 값을 보내는 경우입니다.

```json
{ "values": { "COMPANY_NAME": "고친 회사 이름" }, "expectedVersion": 0 }
```

이번에 요구하는 버튼은 BTN_NEW가 아니라 BTN_SAVE입니다. 메뉴는 COMMON_ADMIN, 수준은 WRITE_ALL, 범위는 COMPANY입니다. 신규 버튼만 허용하고 저장 버튼은 허용하지 않은 사용자라면 POST는 가능해도 이 PUT은 거절될 수 있습니다.

또한 READ_ALL+COMPANY와 BTN_SAVE 허용만 가진 사용자는 WRITE_ALL 메뉴 수준을 충족하지 못해 거절됩니다. 버튼 Y 하나만으로 수정권한이 생기지 않습니다. 성공적으로 preHandle()을 통과한 뒤에야 JSON과 expectedVersion 검사를 수행하고, DB 버전이 다르면 후속 단계에서 409가 날 수 있습니다.

#### 7.10.9 실제 예 3 — 회사 목록 조회는 클래스 표시를 사용한다

list()에는 별도의 @RequiresPermission이 없습니다. 하지만 MasterController 클래스에 표시가 있으므로 GET `/api/common/admin/companies`도 규칙을 갖습니다.

```text
menu   = COMMON_ADMIN
button = BTN_SEARCH
level  = READ_ALL
scopes = [COMPANY]
```

COMMON_ADMIN의 READ_ALL+COMPANY Grant와 BTN_SEARCH만 가진 운영자는 목록 조회는 가능할 수 있지만 앞의 POST·PUT은 WRITE_ALL 수준과 해당 버튼이 필요하므로 자동 허용되지 않습니다.

이렇게 같은 Controller의 작업마다 표지를 달리 읽는 것이 직접 어노테이션을 만든 실제 사용 이유입니다.

#### 7.10.10 표시를 붙이지 않은 곳과 적용 범위

표시가 없는 메서드를 이 interceptor가 무조건 로그인 검사하지는 않습니다. `ReferenceDataController`는 자신이 직접 users.require()를 호출합니다. 공개 언어·메시지 API는 원래 공개입니다.

현재 등록 범위는 `/api/common/**`입니다. 그 밖의 향후 API에 @RequiresPermission만 붙이고 이 검사기 등록을 확장하지 않으면, 현재 검사기로 자동 보호되지 않습니다. Java 배치에서 Service를 직접 호출할 때도 MVC preHandle()은 실행되지 않습니다.

**어노테이션이 있는가뿐 아니라 누가 그 표시를 읽고 언제 검사하는가까지 함께 봐야 합니다.**

### 7.11 SYSTEM_ADMIN은 마법의 이름이 아니다

`ROLE_CODE='SYSTEM_ADMIN'` 문자열만 보고 모든 API를 통과시키는 코드는 없습니다. 현재 통합 V002가 ROLE 10에 COMMON_ADMIN의 WRITE_ALL+COMPANY 및 버튼 권한을 명시적으로 넣었기 때문에 사용할 수 있는 것입니다.

Seed에 사용자나 그 사용자의 ROLE 10 연결을 넣지는 않습니다. 실제 관리자 신분 확인과 최초 사용자 연결은 Level 2 또는 고객별 초기 migration에서 마련해야 합니다.

화면에서 버튼을 숨기는 것만으로도 권한 검사가 끝나지 않습니다. 누군가 직접 API를 호출할 수 있으므로 서버의 `preHandle()`과 PermissionService 검사가 필요합니다.

<a id="chapter-8"></a>
## 8. 한국어·영어 메시지를 고르는 과정

### 8.1 문장 대신 메시지 코드를 전달한다

권한 부족을 알릴 때 Java 코드에는 다음처럼 씁니다.

```java
throw new ApiException("ERR_ACCESS_DENIED");
```

Java가 직접 한국어 문장과 HTTP 상태를 함께 적지 않습니다. 평상시 문장과 HTTP 상태는 DB의 `TB_MESSAGE`, `TB_MESSAGE_LANG`이 정합니다. 메시지 정책을 고쳐도 Java에 같은 정책을 중복해서 수정하는 일을 줄입니다.

메뉴·버튼·공통코드 표시 이름도 같은 원리입니다. 예를 들어 `BTN_SAVE`는 영어에서 `Save`, 한국어에서 `저장`이라는 문장으로 바뀝니다.

### 8.2 MessageService의 네 가지 작업

`MessageService.java`는 아래 약속을 선언하고 `MessageServiceImpl.java`가 구현합니다.

| 메서드 | 역할 |
|---|---|
| `languages()` | 활성 지원 언어 목록을 읽는다 |
| `language(userLanguage, requestedLanguage, acceptLanguage)` | 이번 요청에서 우선 사용할 언어를 정한다 |
| `get(code, language)` | 정책과 실제 번역문을 함께 찾는다 |
| `text(code, language, args...)` | 번역문에 인자를 넣어 최종 문장을 만든다 |

언어를 고르는 `language()`와 번역문을 찾는 `get()`은 다른 작업입니다. 일본어를 골라도 일본어 번역이 없으면 결과 문장은 영어일 수 있습니다.

### 8.3 language(): 선호 언어의 우선순위

언어를 고르는 순서는 다음과 같습니다.

1. 인증된 사용자의 `TB_USER.LANG_CODE`.
2. URL의 `?lang=...` 요청 값.
3. 브라우저의 `Accept-Language` 헤더.
4. 활성 기본 언어.

사용자가 en이고 URL에 ko를 보냈다면 현재 구현에서는 en이 먼저입니다. 언어 설정 없는 사용자나 익명 요청에서는 요청 lang이 사용될 수 있습니다.

`match()`는 지원 언어 코드와 대소문자를 무시하고 비교합니다. 정확한 것이 없고 `en-US`처럼 지역 부분이 있으면 앞의 `en`으로 다시 찾습니다. 중국어 등에서는 지원 목록에 어떤 코드가 있는지에 따라 결과가 달라집니다. 지원하지 않는 임의 언어를 자동 번역하지는 않습니다.

Accept-Language의 예 `zh-TW;q=0.9,en;q=0.8`은 “대만 중국어를 더 선호하고 영어도 가능하다”는 뜻입니다. Java가 선호도 순서로 읽고 q=0은 제외합니다. 헤더가 잘못되었으면 무시하고 기본 언어로 넘어갑니다.

`defaultLanguage()`는 활성 목록에서 DEFAULT_YN=Y인 언어를 고르고, 없으면 ko를 반환합니다. 목록을 읽는 DB가 장애라면 `language()`가 예외를 잡고 ko를 반환합니다.

### 8.4 get(): 번역이 없을 때 연결을 따라간다

기본 seed의 대체 경로는 다음과 같습니다.

```text
ja    → en → ko
zh-CN → en → ko
zh-TW → en → ko
en    → ko
ko    → 끝
```

일본어로 `ERR_NOT_FOUND`를 찾는다고 합시다.

1. `get("ERR_NOT_FOUND", "ja")`가 활성 언어 목록을 읽습니다.
2. `MessageMapper.find(code, "ja")`를 부릅니다.
3. 메시지 master는 있지만 일본어 번역문이 없으면 다음 언어 en을 찾습니다.
4. 영어 번역문이 있으면 그 `MessageInfo`를 반환합니다.
5. 없다면 ko까지 이어 찾습니다. 대체 경로에서 못 찾으면 아직 확인하지 않은 기본 언어도 확인합니다.

여기서 응답의 langCode는 실제 찾은 번역의 언어입니다. 요청은 ja였어도 en 문장을 썼다면 en으로 응답합니다. bootstrap 묶음의 선택 언어와 개별 이름 문장의 실제 fallback 언어가 항상 같다고 보장되지는 않습니다.

`visited`는 이미 찾은 언어를 기억합니다. DB에 실수로 ja → en → ja 같은 순환이 생겨도 무한 반복하지 않습니다. 관리 입력의 `validateLanguage()`는 순환을 저장하기 전에 막고, 조회의 visited는 기존 잘못된 데이터에도 대비합니다.

### 8.5 MessageMapper.find()의 LEFT JOIN이 필요한 이유

`MessageMapper.xml`은 master에 번역을 LEFT JOIN합니다. 쉬운 말로 하면 “메시지 정책은 가져오고, 이 언어 번역이 있으면 같이 가져온다”입니다.

INNER JOIN만 쓰면 번역이 없는 경우 master까지 없는 것으로 보일 수 있습니다. 현재 방식은 master가 있는데 번역만 없는 상황을 구분할 수 있습니다. 비활성 master는 읽지 않고, 번역은 활성이고 TRANSLATED_YN=Y인 것만 사용합니다.

`LanguageInfo.java`는 언어 목록의 결과 상자입니다. `MessageInfo.java`는 messageCode, messageType, httpStatus, severity, 실제 langCode, messageText, argCount를 담는 결과 상자입니다.

### 8.6 아무 번역도 없거나 DB가 고장 난 경우

master는 있는데 모든 번역이 없으면 `get()`은 master의 상태·종류·인자 수를 유지하고 메시지 코드 자체를 문장 자리에 넣습니다. 화면에 최소한 어떤 메시지인지 이름표가 남습니다.

master가 없거나 DB 조회가 실패하면 private `fallback()`이 작은 안전 결과를 만듭니다. 문장은 `The request could not be completed.`, 언어는 en입니다. 이때만 Java 내부의 최소 정책을 사용합니다.

| fallback 코드 | 상태 |
|---|---|
| ERR_AUTH_REQUIRED | 401 |
| ERR_ACCESS_DENIED | 403 |
| ERR_INVALID_INPUT | 400 |
| ERR_NOT_FOUND | 404 |
| ERR_CONFLICT | 409 |
| 그 밖의 코드 | 500 |

평상시 전체 번역 DB를 Java에 복제한 것은 아닙니다. DB 장애 때 오류 설명을 만들려고 다시 DB를 무한히 부르는 일도 없습니다. `MessageOutageTests.databaseOutageHasSafeFallbackWithoutRecursion()`이 확인합니다.

HTTP 405·415·406 메시지는 정상 DB에서는 통합 V002의 메시지 정책으로 응답하지만 최소 Java fallback에 별도로 모두 등록되어 있지는 않습니다. DB 장애 시 이 코드들은 500 fallback을 받을 수 있습니다. 현재 구현의 실제 범위입니다.

### 8.7 text(): 문장의 빈칸에 값을 넣는다

```java
messages.text("ERR_MESSAGE_NOT_FOUND", "en", "LBL_UNKNOWN");
```

seed의 문장은 `Message {0} was not found.`입니다. `text()`가 MessageFormat으로 첫 인자를 넣으면 `Message LBL_UNKNOWN was not found.`가 됩니다.

인자 배열이 null이면 빈 배열을 사용합니다. 포맷 처리 중 예외가 나면 서버에 기록하고 메시지 코드를 반환합니다. `ApiErrorWriter.response()`도 오류 문장을 만들 때 MessageFormat을 사용합니다.

### 8.8 공개 메시지 API를 따라가기

`ReferenceController.java`의 `languages()`는 GET `/api/common/languages`를 받습니다. `MessageService.languages()` → `MessageMapper.languages()` → TB_LANGUAGE 순서입니다. 인증이 없어도 지원 언어를 볼 수 있습니다.

같은 파일의 `message()`는 GET `/api/common/messages/{code}`를 받습니다. 가능한 사용자 언어, 요청 lang, Accept-Language를 모아 `messages.language()`를 부르고 `messages.get()`으로 `MessageInfo`를 응답합니다. 이 API는 인자를 넣은 최종 문자열을 받는 전용 창구가 아니라 메시지 정보 조회입니다.

알 수 없는 메시지 코드의 get() 결과는 현재 fallback MessageInfo일 수 있습니다. 그 안의 httpStatus=500이라는 정책 값과 GET API 자체의 HTTP 응답 상태는 별개입니다. Controller가 MessageInfo를 그대로 반환하므로 정상 조회 메서드의 응답은 보통 HTTP 200입니다.

<a id="chapter-9"></a>
## 9. 오류가 같은 모양으로 나오는 이유

### 9.1 throw는 작업을 멈추고 오류를 알리는 것

`throw new ApiException("ERR_CONFLICT")`는 “여기서 정상 작업을 계속하지 말고 충돌 사실을 알려라”라는 뜻입니다. 예외는 호출한 쪽으로 전달됩니다. transaction 안의 저장에서 예외가 나면 변경이 되돌아갈 수 있습니다.

`ApiException.java`는 messageCode와 args만 갖습니다. 생성자는 args를 복사하고 `args()`도 복사본을 돌려줍니다. 밖에서 배열을 바꿔 예외의 내용을 바꾸지 못하게 합니다. `messageCode()`는 코드 이름을 꺼냅니다.

### 9.2 ApiExceptionHandler가 오류를 분류한다

`ApiExceptionHandler.java`는 여러 Controller의 오류를 공통으로 받습니다.

| 메서드 | 어떤 상황인가 | 사용하는 메시지 코드 |
|---|---|---|
| `api()` | 우리가 던진 ApiException | 예외에 들어 있던 코드 |
| `validation()` | JSON 오류, 필수값 누락, 타입 오류, bean validation | ERR_INVALID_INPUT |
| `conflict()` | FK·UNIQUE 등 DB 자료 제약 위반 | ERR_CONFLICT |
| `missing()` | 요청 주소의 자료/리소스를 찾지 못함 | ERR_NOT_FOUND |
| `method()` | 지원하지 않는 HTTP 방식 | ERR_METHOD_NOT_ALLOWED |
| `media()` | 지원하지 않는 요청 Content-Type | ERR_UNSUPPORTED_MEDIA_TYPE |
| `accept()` | 제공할 수 없는 응답 형식 요청 | ERR_NOT_ACCEPTABLE |
| `unexpected()` | 그 밖의 예상하지 못한 오류 | ERR_INTERNAL_SERVER |

`method()`는 Spring 예외에 담긴 Allow 같은 헤더도 유지합니다. DB 제약 오류는 간단한 오류 종류를 log에 남기고, 예상하지 못한 오류는 서버 진단용 log에 자세히 남깁니다. 사용자 응답에는 DB SQL이나 Java stack trace를 붙이지 않습니다.

### 9.3 ApiErrorWriter.response()가 결과를 만든다

오류 분류가 끝나면 `ApiErrorWriter.java`의 `response()`로 들어갑니다.

1. `RequestUserService.language()`로 가능한 사용자 언어를 확인합니다. DB 장애 등은 잡아서 오류 응답 생성이 다시 실패하지 않도록 합니다.
2. 요청 lang과 Accept-Language도 넣어 `MessageService.language()`를 부릅니다.
3. `MessageService.get()`으로 코드의 문장과 정책을 읽습니다.
4. 오류 상태로 사용할 수 있는 400~599인지 확인합니다. 그 범위가 아니거나 없으면 500입니다.
5. MessageFormat으로 인자를 넣습니다. 문장 처리 실패 시 코드 자체를 사용합니다.
6. `ApiError` 상자를 만들어 JSON 응답을 반환합니다.

정상 안내 메시지의 HTTP_STATUS=null을 오류 예외로 던져도 HTTP 200으로 바뀌지 않습니다. 오류 응답 writer가 500으로 제한합니다.

### 9.4 ApiError.java의 여섯 칸

```json
{
  "messageCode": "ERR_AUTH_REQUIRED",
  "message": "Authentication is required.",
  "langCode": "en",
  "status": 401,
  "path": "/api/common/admin/users",
  "timestamp": "2026-10-07T00:00:00Z"
}
```

문서용 예시입니다. 시간은 실제 오류 생성 시각인 `Instant.now()`로 정해집니다.

| 칸 | 뜻 |
|---|---|
| messageCode | 프로그램이 구분하는 오류 이름표 |
| message | 사람에게 보여 줄 번역문 |
| langCode | 실제 오류 문장의 언어 |
| status | HTTP 오류 상태 번호 |
| path | 오류가 발생한 요청 경로 |
| timestamp | 오류 결과를 만든 시각 |

화면은 message를 안내하고 필요하면 messageCode를 기준으로 처리할 수 있습니다. 한국어 문장 내용을 문자열로 비교해 프로그램을 분기하기보다 코드 이름을 비교하는 것이 안정적입니다.

### 9.5 ApiErrorWriter.write()는 Level 2를 위한 공통 출구

Controller 이전의 로그인 필터에서 오류가 나면 보통 ControllerAdvice 경로와 다르게 직접 응답을 써야 합니다. 그때 Level 2가 사용할 수 있는 것이 `ApiErrorWriter.write(request, response, exception)`입니다.

`write()`는 `response()`가 만든 같은 결과를 가져와 status, UTF-8, JSON content type을 설정하고 ObjectMapper로 JSON을 씁니다. 그래서 필터 오류와 Controller 오류의 모양을 일치시킬 수 있습니다.

현재 Level 1에 실제 로그인 필터가 구현된 것은 아닙니다. 공통 writer를 준비하고 테스트에서 직접 호출해 확인한 상태입니다.

### 9.6 CORS 오류까지 모두 이 모양인가

아닙니다. 현재 공통 writer는 Controller/서비스 예외와 이를 호출하는 Level 2 필터를 위한 것입니다. Spring의 CORS 거절 등 프레임워크가 직접 처리하는 모든 응답을 이 구조로 바꾼 것은 아닙니다.

공통 오류 기능의 적용 범위를 정확히 알고 있어야, 브라우저의 CORS 거절 메시지를 보고 ApiExceptionHandler 문제로 착각하지 않습니다.

<a id="chapter-10"></a>
## 10. 화면을 시작할 때 필요한 데이터

### 10.1 화면이 준비물을 한 번에 받는다

로그인이 끝나면 화면 프로그램은 “나는 누구인가”, “어떤 메뉴를 보여 줄 것인가”, “각 메뉴에 어떤 버튼을 보여 줄 것인가”를 알아야 합니다. 이를 준비하는 API가 GET `/api/common/bootstrap`입니다.

이때 bootstrap은 “화면 시작에 필요한 자료 준비”라는 뜻입니다. 설정 파일의 `erpcom.bootstrap` 관리자 계정 초기화 예약 설정과는 다른 것입니다. 이 API가 ADMIN 계정을 만드는 것은 아닙니다.

### 10.2 ReferenceDataController.bootstrap()의 흐름

`ReferenceDataController.java`의 `bootstrap()`은 먼저 `users.require(request)`로 인증 사용자를 확인합니다. 그다음 `messages.language()`로 선호 언어를 정하고 `ReferenceDataService.bootstrap(userId, language)`를 호출합니다.

`ReferenceDataService.java`는 약속이고 `ReferenceDataServiceImpl.java`가 실제 구현입니다. Controller가 `ReferenceMapper`를 직접 부르지 않습니다.

### 10.3 ReferenceDataServiceImpl.bootstrap()의 여섯 단계

1. `PermissionService.user()`로 활성 사용자 기본정보를 얻습니다.
2. `PermissionService.get()`으로 최종 메뉴·버튼 권한을 계산합니다.
3. 메뉴 번호별 권한 목록과 `메뉴:버튼` 허용 짝을 빠르게 찾을 수 있도록 모읍니다.
4. `ReferenceMapper.buttons()`로 활성 배치와 버튼을 읽고 허용된 짝만 남깁니다. 이름은 `MessageService.text()`로 번역합니다.
5. `ReferenceMapper.menus()`로 활성 메뉴를 읽고 READ_PARTIAL을 허용하는 메뉴만 남깁니다. 메뉴 이름도 번역합니다.
6. 사용자, 선택 언어, 메뉴 목록을 `ReferenceModels.Bootstrap`에 묶어 응답합니다.

역할 이름만 보고 메뉴를 임의 추가하지 않습니다. 부모 메뉴를 보여 줬다고 자식 메뉴를 자동 허용하지도 않고, 자식 메뉴가 허용됐다고 부모 권한을 자동 부여하지도 않습니다.

### 10.4 결과는 부모 번호가 있는 평평한 목록이다

아래는 결과의 이해를 돕는 축약 예시입니다. 실제 테스트 조회 사용자는 더 많은 메뉴를 받을 수 있습니다.

```json
{
  "user": {
    "userId": 2,
    "companyId": 100,
    "siteId": 1000,
    "deptId": 10000,
    "langCode": null
  },
  "langCode": "en",
  "menus": [
    {
      "menuId": 1010,
      "parentMenuId": 1000,
      "menuCode": "PM_PROJECT_LIST",
      "name": "Project Management",
      "menuType": "SCREEN",
      "screenId": "PM_PJ010",
      "formPath": "pm/PM_PJ010.xfdl",
      "iconName": null,
      "sortOrder": 10,
      "grants": [{ "accessLevel": "READ_ALL", "dataScope": "OWN" }],
      "buttons": [
        { "buttonId": 1, "buttonCode": "BTN_SEARCH", "name": "Search", "sortOrder": 10 },
        { "buttonId": 5, "buttonCode": "BTN_EXCEL", "name": "Excel", "sortOrder": 50 }
      ]
    }
  ]
}
```

`menus` 안에 children 배열을 중첩해 나무를 완성해서 주는 구조는 아닙니다. 화면이 parentMenuId를 이용해 계층을 만들 수 있습니다. 상위 메뉴가 권한 목록에서 빠져 있을 수 있으므로 화면은 그런 경우의 표시 방법도 정해야 합니다.

`user.langCode`는 사용자 DB의 원래 언어이고, 바깥 langCode는 이번 요청에서 선택한 언어입니다. 사용자 언어가 없는데 lang=en을 요청했다면 위처럼 둘이 다를 수 있습니다.

### 10.5 코드 목록의 흐름

GET `/api/common/codes/USER_STATUS?lang=en`은 사용자 상태 선택 상자에 넣을 자료를 가져오는 예입니다.

```text
ReferenceDataController.codes()
  → RequestUserService.require()
  → MessageService.language()
  → ReferenceDataServiceImpl.codes()
  → PermissionService.user()
  → ReferenceMapper.codes("USER_STATUS")
  → MessageService.text(CODE_NAME_MSG_CODE, language)
  → CodeInfo 목록
```

`ReferenceMapper.xml`의 codes SQL은 코드와 코드 분류 모두 USE_YN=Y인지 확인하고 SORT_ORDER, CODE 순서로 정렬합니다. 분류가 없거나 비활성이면 현재 빈 목록을 반환하며 별도 ERR_NOT_FOUND를 던지지 않습니다.

이 조회는 인증 사용자에게 제공됩니다. 별도 메뉴 annotation은 없습니다. 특정 업무에서 일부 코드만 보여 줘야 하는 정책이 있으면 그 업무에 추가해야 합니다.

### 10.6 ReferenceModels.java의 일곱 상자

| record | 쉬운 역할 |
|---|---|
| `CodeRow` | DB에서 읽은 코드. 번역 전의 메시지 코드가 들어 있다 |
| `CodeInfo` | 화면에 줄 코드. 번역된 name이 들어 있다 |
| `MenuRow` | DB에서 읽은 메뉴. 메시지 코드와 화면 정보가 들어 있다 |
| `ButtonRow` | DB에서 읽은 화면별 버튼. 메시지 코드가 들어 있다 |
| `ButtonInfo` | 화면에 줄 번역된 버튼 이름 |
| `MenuInfo` | 화면에 줄 메뉴 이름·화면 정보·Grant·허용 버튼 |
| `Bootstrap` | 사용자 + 선택 언어 + 메뉴 목록 |

Row와 Info를 나눈 이유는 DB 원본 자료와 화면용 결과가 다르기 때문입니다. `CodeRow.codeNameMsgCode()`로 코드의 이름표를 꺼내고, 번역 서비스가 얻은 문장을 `CodeInfo.name`에 넣습니다.

현재 `ReferenceDataServiceImpl.bootstrap()`과 위 record들의 응답은 유지했습니다.
단위·통화·금액 표시 정책은 `BasisController.java`의 별도 조회 API로 제공합니다.
`GET /api/common/units`, `/api/common/currencies`, `/api/common/amount-formats/{menuCode}`의
응답 record는 `BasisModels.java`에 있으며, 17.12에서 실제 연결을 설명합니다.

### 10.7 내 권한만 보는 API

GET `/api/common/permissions/me`는 `ReferenceController.permissions()`가 받습니다. `users.require()`의 userId로 `PermissionService.get()`을 호출해 `PermissionSnapshot`을 돌려줍니다.

bootstrap은 권한에 메뉴·버튼 이름과 화면 정보를 합친 결과이고, permissions/me는 권한 계산 결과 자체입니다. 문제를 찾을 때 먼저 permissions/me에서 해당 메뉴의 Grant와 버튼 짝이 있는지 확인한 다음 bootstrap을 보면 좋습니다.

<a id="chapter-11"></a>
## 11. application-dev.yaml과 mysettings.ini의 역할

### 11.1 소스 코드와 설정은 다른 것이다

소스는 “무슨 일을 어떻게 할지”를 정합니다. 설정은 “어느 DB로 연결할지, 몇 번 포트로 열지, 어느 화면 주소와 통신할지”를 정합니다. 같은 프로그램을 개발 PC와 운영 서버에서 쓰되 연결 대상만 바꿀 수 있습니다.

현재 두 YAML 파일과 개인 파일의 관계는 다음과 같습니다.

```text
application.yaml              : 기본 설정
  + dev 프로필을 켰을 때만
application-dev.yaml          : 개발용 덮어쓰기와 개인 설정 import
  + 개발용 import로 읽는
.idea/mysettings.ini          : 로컬 개인 설정의 ERPCOM_* 값
  + 환경변수/실행 인자 등 더 높은 우선순위 설정
최종 실행 설정
```

개발 프로필을 켜지 않으면 application-dev.yaml의 import도 적용하지 않으므로 개인 INI는 이 방식으로 읽지 않습니다. 파일을 추가하기만 하고 프로필을 안 켜면 기대한 값이 적용되지 않을 수 있습니다.

### 11.2 import 한 줄을 천천히 읽기

`application-dev.yaml`의 설정은 다음과 같습니다.

```yaml
spring:
  config:
    import: "${ERPCOM_SETTINGS_LOCATION:optional:file:./.idea/mysettings.ini[.properties]}"
```

- `ERPCOM_SETTINGS_LOCATION` 값이 있으면 그 위치를 사용합니다.
- 없으면 `./.idea/mysettings.ini`를 사용합니다.
- `optional:`은 그 파일이 없어도 그것만으로 실행을 실패시키지 않는다는 뜻입니다.
- `file:`은 외부 파일을 읽는다는 뜻입니다.
- `[.properties]`는 이름이 ini여도 Java Properties 형식으로 해석하라는 표시입니다.

실제 대상 파일명은 `mysettings.ini`입니다. 앞에 점을 붙인 `.mysettings.ini`가 아닙니다. `./`는 실행 작업 폴더를 뜻하므로 IDE의 작업 폴더도 프로젝트 루트로 맞춰야 합니다.

### 11.3 Properties 형식의 개인 파일

파일 이름이 INI여도 현재 로더는 `[database]` 같은 섹션 구문을 사용하는 전통적인 INI parser가 아닙니다. 간단한 key=value Properties 형식을 사용합니다. 테스트에서는 key:value 형태도 확인합니다.

아래는 실제 개인 파일을 복사한 것이 아닌 설명용 형식 예입니다.

```properties
ERPCOM_API_PORT=8083
ERPCOM_DB_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1
ERPCOM_DB_USERNAME=example_schema
ERPCOM_DB_PASSWORD=replace_with_local_value
ERPCOM_NEXACRO_ORIGIN=http://localhost:9093
ERPCOM_SWAGGER_ENABLED=true
```

`.idea`는 .gitignore로 저장소에서 제외됩니다. 이 파일은 src/main/resources에 있지 않으므로 현재 JAR에 함께 묶이지 않습니다. 배포 대상에서는 별도의 외부 설정이나 환경변수를 준비해야 합니다.

### 11.4 ${이름:기본값}을 읽는 방법

```yaml
port: ${ERPCOM_API_PORT:8083}
```

이것은 “ERPCOM_API_PORT 설정이 있으면 그 값을 쓰고, 없으면 8083을 사용한다”는 뜻입니다. 파일에 8183을 적었다면 기본값 8083 대신 8183입니다.

기본 설정의 DB 값에는 예전 `ERP_DB_URL` 등도 들어 있습니다. ERPCOM 값이 먼저이고, 없으면 ERP 별칭을 확인하고, 그것도 없으면 기본값을 사용합니다. 예전 별칭을 모두 새 파일로 바꿔야만 실행되는 것은 아닙니다.

일반적인 Spring 실행에서 환경변수와 command-line 설정은 파일 설정보다 높은 우선순위를 가질 수 있습니다. 같은 이름을 여러 곳에 적었다면 최종값을 확인해야 합니다. `DevelopmentConfigurationTests.explicitOverridesWinOverPrivateIni()`가 덮어쓰기 동작을 확인합니다.

### 11.5 실제 Level 1에서 쓰는 설정들

| 이름 | 실제로 바뀌는 것 | dev 기본값 |
|---|---|---|
| `ERPCOM_API_PORT` | 서버가 요청을 받을 포트 | 8083 |
| `ERPCOM_DB_URL` | Oracle JDBC 접속 대상 | 기본 YAML의 fallback 사용 |
| `ERPCOM_DB_USERNAME` | DB 스키마 사용자 | 기본 YAML의 fallback 사용 |
| `ERPCOM_DB_PASSWORD` | DB 암호 | 실제 값은 개인 설정 또는 환경에서 공급 |
| `ERPCOM_DB_MIN_IDLE` | 연결 풀의 최소 대기 연결 설정 | 5 |
| `ERPCOM_DB_MAX_POOL_SIZE` | 최대 DB 연결 수 | 10 |
| `ERPCOM_DB_CONNECTION_TIMEOUT_MS` | 풀에서 연결을 기다리는 시간 | 30000ms |
| `ERPCOM_FLYWAY_ENABLED` | migration 실행 여부 | true |
| `ERPCOM_FLYWAY_BASELINE_ON_MIGRATE` | 이력 없는 기존 스키마의 baseline 허용 여부 | true |
| `ERPCOM_FLYWAY_BASELINE_VERSION` | baseline 기준 번호 | 0 |
| `ERPCOM_NEXACRO_ORIGIN` | 브라우저의 허용 화면 origin | http://localhost:9093 |
| `ERPCOM_SWAGGER_ENABLED` | API 명세와 Swagger 화면 노출 | true |
| `ERPCOM_SETTINGS_LOCATION` | 개인 설정 import 위치 | 위 import 기본 경로 |

dev를 쓰지 않은 기본 설정은 서버 포트 8082, Swagger false, CORS origin 빈 값입니다. 표의 값은 **개인 파일에서 덮어쓰기 전의 코드상 기본값**입니다. 개인 파일의 실제 접속 값이 항상 localhost라는 뜻은 아닙니다.

연결 풀은 DB 연결을 매번 새로 만드는 대신 여러 개 준비해 돌려 쓰는 방식입니다. 이를 Hikari가 맡습니다. connection-timeout은 현재 풀에서 연결을 기다리는 제한이지, 모든 SQL 실행 시간이나 SSO 통신 시간을 한 번에 제한하는 설정은 아닙니다.

### 11.6 MyBatis 설정이 Java 결과에 미치는 영향

`mapper-locations: classpath:mapper/**/*.xml`은 MyBatis가 SQL 파일을 찾을 위치입니다. 설정 덕분에 BasisMapper를 포함한 다섯 Mapper 인터페이스와 XML을 연결합니다.

`map-underscore-to-camel-case=true`와 `arg-name-based-constructor-auto-mapping=true`는 DB의 USER_ID 같은 이름을 record의 userId 등 생성자 항목과 연결할 때 사용합니다. 관리 API의 Map 컬럼 이름을 입력 소문자로 자동 바꿔 주는 설정은 아닙니다.

`call-setters-on-nulls=true`는 null 자료도 결과에 포함하도록 돕고, `jdbc-type-for-null=NULL`은 null을 DB parameter로 보내는 기본 타입을 정합니다.

`local-cache-scope=STATEMENT`는 MyBatis의 지역 캐시 범위를 한 SQL 문장 수준으로 줄입니다. ROLE·메뉴 권한 변경을 같은 긴 세션의 예전 조회로 계속 읽는 일을 줄이기 위한 설정입니다. Redis 캐시가 생겼다는 뜻은 아닙니다.

### 11.7 CommonCorsProperties.java: 화면 주소를 확인한다

브라우저 화면이 `http://localhost:9093`에서 열리고 서버가 `http://localhost:8083`에 있으면 origin이 다릅니다. **origin**은 통신 방식(http/https), 호스트, 포트의 조합입니다. 브라우저가 다른 origin의 응답을 읽을 수 있게 서버가 허용 범위를 알려 주는 것이 CORS입니다.

`CommonCorsProperties`는 `erpcom.cors.allowed-origin`을 읽는 record입니다. 생성자는 null을 빈 문자열로 바꾸고 앞뒤 공백을 제거합니다. `isValidOrigin()`은 다음을 확인합니다.

- 빈 값은 허용합니다. 이 경우 CORS 규칙을 등록하지 않습니다.
- http 또는 https이며 정상 호스트가 있어야 합니다.
- `*` 와일드카드를 허용하지 않습니다.
- 경로, query, fragment, 사용자명·암호 부분이 없어야 합니다.
- 포트가 명시되면 1~65535 범위여야 합니다.

현재 검사에서는 `http://localhost:9093/`의 뒤쪽 `/`도 경로가 있으므로 허용하지 않습니다. `http://localhost:9093`처럼 origin만 적습니다. 여러 주소를 쉼표로 나열하는 목록 입력도 현재 record가 제공하는 기능은 아닙니다.

`@AssertTrue`의 영어 문장은 개발 설정이 잘못되었을 때 시작 단계에서 알려 주는 진단입니다. 사용자 업무 API 메시지와 다른 종류입니다.

### 11.8 CommonWebConfiguration의 CORS 메서드

`commonCorsConfigurer()`가 구성 객체를 만들고 그 안의 `addCorsMappings()`가 규칙을 등록합니다. 적용 경로는 `/api/common/**`, 방식은 GET/POST/PUT/DELETE/OPTIONS입니다. 요청 헤더는 허용하고 credentials=true이며 preflight 결과의 재사용 시간 maxAge는 3600초입니다.

**preflight**는 브라우저가 실제 요청 전에 OPTIONS로 “이 주소와 헤더로 통신해도 되나요?”라고 묻는 사전 질문입니다. 등록된 origin의 preflight는 인증 없이 처리됩니다. 실제 GET·POST가 인증이나 권한 검사를 통과했다는 뜻은 아닙니다.

예를 들어 정상 화면 origin의 사전 요청은 200이어도, 사용자가 로그인하지 않은 관리 목록 요청은 401입니다. `nexacroPreflightIsAllowedWithoutBypassingAuthentication()` 테스트가 두 요청을 함께 확인합니다.

CORS는 브라우저가 응답을 읽는 정책입니다. 사용자를 확인하는 인증이나 서버의 메뉴·버튼 권한을 대신하지 않습니다.

### 11.9 Swagger 설정의 연결

`erpcom.security.swagger-enabled`를 설정하면 기본 YAML의 `springdoc.api-docs.enabled`와 `springdoc.swagger-ui.enabled`가 함께 그 값을 사용합니다.

dev 기본값이 true이면 개발 기본 주소 `http://localhost:8083/swagger-ui.html`에서 API 안내 화면을 볼 수 있습니다. 포트를 개인 설정으로 바꾸었다면 주소의 포트도 바꾸어야 합니다. false이면 명세와 화면 모두 비활성화합니다.

`security`라는 설정 이름 아래 있다고 실제 로그인 보안 기능이 만들어진 것은 아닙니다. Swagger 노출 여부를 조절하는 값입니다. 보호된 관리 API는 Swagger를 켜도 실제 인증·권한이 필요합니다.

### 11.10 지금은 값만 있고 실제 구현은 없는 설정들

dev YAML에는 다음 Level 2 연결용 설정도 있습니다.

| 설정 영역 | 예약 내용 | 현재 Level 1 동작 |
|---|---|---|
| `erpcom.auth.session-ttl-hours` | 세션 유지 시간 | 세션 생성·만료 구현 없음 |
| `erpcom.auth.cleanup-cron` | 만료 세션 정리 일정 | 이를 실행하는 스케줄러 없음 |
| `erpcom.auth.local-login` | LOCAL 로그인, 실패 횟수, 잠금, BCrypt 강도 | 로그인·해시·잠금 구현 없음 |
| `erpcom.sso` | SSO provider, callback, state, 외부 검증 주소 | SSO 요청·callback Controller 없음 |
| `erpcom.bootstrap` | 초기 관리자 이름·회사·암호 | 시작 시 관리자 자동 생성 없음 |
| 개인 설정의 Redis 관련 값 | 후속 세션 저장소 연결 정보 | 현재 Redis 연결·세션 저장 구현 없음 |

`ERPCOM_SESSION_TTL_HOURS`, `ERPCOM_LOGIN_FAILURE_THRESHOLD`, `ERPCOM_LOGIN_LOCK_MINUTES`, `ERPCOM_BCRYPT_STRENGTH`, `ERPCOM_ADMIN_LOGIN_ID`, `ERPCOM_ADMIN_NAME`, `ERPCOM_ADMIN_PASSWORD`, `ERPCOM_BOOTSTRAP_COMPANY_CODE` 등이 일부 예약 YAML 값으로 연결되어 있어도 그것을 실행할 Java 담당자가 아직 없는 것입니다.

특히 `ERPCOM_BOOTSTRAP_ENABLED=true`를 적었다고 ADMIN이 생기지 않습니다. SSO enabled=true와 provider=mock도 실제 mock 로그인 구현을 만들지 않습니다. SSO callback 예시 포트는 별도 설정이므로 서버 포트를 바꿨다고 자동으로 맞춰 바뀌지 않습니다.

### 11.11 개발 실행을 이해하기

프로젝트 루트를 작업 폴더로 사용하고 dev 프로필을 켭니다.

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

이 명령은 설명을 위한 실행 방법입니다. 실제 실행은 설정된 Oracle에 연결하고 Flyway가 미적용 migration을 실행할 수 있습니다. 문서 작성 과정에서 이 개발 실행을 수행한 것은 아닙니다.

IDE에서도 같은 의미로 active profile을 dev, working directory를 `F:\java\workspace\erpcom`으로 지정합니다. 패키징된 JAR를 다른 폴더에서 실행하면 상대 import 위치도 그 폴더 기준이므로 필요하면 ERPCOM_SETTINGS_LOCATION으로 외부 설정 위치를 지정합니다.

<a id="chapter-12"></a>
## 12. DB를 만들고 초기 자료를 넣는 두 개의 SQL

### 12.1 Migration은 순서 있는 DB 준비 기록

현재 최초 설치 구성은 V001과 V002입니다. 기존 V003의 구조 변경은 V001에, 자료는 V002에 합쳤습니다.
아직 DB에 적용하지 않은 최초 설치 구성을 사용자의 승인으로 통합한 것입니다.
이미 DB에 적용된 Migration을 수정하지 않는 원칙은 여전히 적용됩니다.

| 파일 | 하는 일 |
|---|---|
| [V001__create_common_tables.sql](F:/java/workspace/erpcom/src/main/resources/db/migration/V001__create_common_tables.sql) | 21개 표, 감사·버전 컬럼, FK·제약·인덱스, SQ_COMMON_ID 생성 |
| [V002__seed_common_reference_data.sql](F:/java/workspace/erpcom/src/main/resources/db/migration/V002__seed_common_reference_data.sql) | 테이블별 공통 기준·다국어·조직·역할·메뉴·권한·단위·통화·표시 예제 |

현재 실행 대상에는 V003이 없습니다. 통합 내용과 같은 V003을 함께 실행하면 중복 생성·등록 오류가 납니다.
소스에 SQL이 있다고 실제 Oracle DB가 이미 초기화된 것은 아닙니다.

### 12.2 V001: 21개 표와 연결 규칙을 만든다

PK는 자료의 이름표, FK는 다른 표와의 연결, UNIQUE는 중복 방지, CHECK는 값의 조건입니다.
모든 표에 공통 다섯 칸과 VERSION_NO가 처음부터 있습니다.
새 표는 TB_UNIT·TB_CURRENCY·TB_MENU_AMOUNT_FORMAT이며 표시명은 기존 메시지를 참조합니다.
SQ_COMMON_ID는 1,000,000부터 API의 숫자 번호를 발급합니다.
일부 숫자 PK의 DB identity와 API sequence는 별개이므로 외부 등록도 번호 정책을 맞춰야 합니다.

### 12.3 V002: 테이블별로 초기 자료를 한 번에 넣는다

V002는 21개 테이블 구역으로 나눴습니다. 자료가 있는 17개는 각 구역에 초기·추가 자료를 모두 모았고,
TB_USER·TB_USER_ROLE·사용자 메뉴/버튼 예외권한 네 표는 초기 자료가 없다는 설명을 표시했습니다.
외래키가 참조하는 표를 먼저 넣고, 같은 표의 부모·기준 행도 먼저 넣습니다.
테이블 안의 컬럼 순서를 통일했으며 버튼 일괄 배정 INSERT ... SELECT도 해당 구역에 있습니다.

현재 예제는 지원 언어 5개, 메시지 53개, 번역 102개, 단위 9개, 통화 3개, 금액 표시 정책 3개를 포함합니다.
실제 사용자·비밀번호는 생성하지 않습니다. 조직과 업무 메뉴는 샘플이며 실제 화면 파일을 만들지는 않습니다.
메뉴 1030은 PM_TOTAL_ESTIMATED_COST 예제이고, DEFAULT와 TOTAL_ESTIMATED_COST는 억원·소수 1자리,
UNIT_PRICE는 원 단위·소수 0자리입니다.

### 12.4 이전 V003의 내용은 어디로 갔는가

| 이전 보강 내용 | 현재 위치 |
|---|---|
| SQ_COMMON_ID, 모든 VERSION_NO, 조직 복합 FK·Unique | V001의 Sequence·CREATE TABLE |
| 언어 fallback FK, 활성 기본 언어 Unique | V001 |
| 고정 CK_USER_STATUS 제거 | V001에서 해당 CHECK를 만들지 않음 |
| 인증 필요·자료 없음·충돌·HTTP 오류 메시지 | V002 메시지·번역 구역 |
| COMMON_ADMIN 메뉴 9000과 ROLE 10의 메뉴·버튼 권한 | V002의 해당 테이블 구역 |

복합 FK에서 선택 값이 null인 조합은 Service 검증도 필요합니다.
`MasterServiceImpl.validateOrganization()`과 새 기준정보 검증 메서드가 DB 제약을 보완합니다.
고객별 실제 화면 설정과 운영 초기값은 공통 예제와 구분해 후속 Migration 또는 관리자 등록으로 준비합니다.

### 12.5 baseline-version=0의 뜻

Flyway 이력은 없는데 스키마에 기존 자료가 있으면 baseline은 “여기까지를 출발점으로 기록한다”는 의미입니다. 기준 버전보다 높은 migration을 이어 적용합니다.

dev 기본 기준이 0이면 V001도 0보다 높은 버전이므로 실행 대상입니다. 기준을 1로 잘못 지정하면 V001을 건너뛸 수 있어 테이블 생성과 Java 기대가 어긋날 수 있습니다.

baseline이 기존 DB의 테이블을 검사해서 자동으로 원하는 모양으로 고치는 기능은 아닙니다. 기존 DB에 같은 표가 있는데 이력만 없으면 version 0으로 V001을 실행해 CREATE TABLE 충돌이 생길 수도 있습니다. 현재 DB 구조와 migration 이력의 관계를 맞춰야 합니다.

### 12.6 MyBatisConfiguration.databaseIdProvider()

`MyBatisConfiguration.java`의 `databaseIdProvider()`는 DB 제품 이름을 알아보고 Oracle이면 oracle, H2면 h2라는 식별값을 제공합니다.

`MasterMapper.xml`의 lockHierarchy는 Oracle 기본 SQL과 H2용 SQL이 따로 있습니다. H2에서는 같은 Oracle 표 잠금 문장을 쓰지 않고 `UPDATE ... SET VERSION_NO = VERSION_NO`로 대체합니다. 테스트를 가능하게 하는 대체일 뿐 두 DB의 잠금 동작을 완전히 같게 만드는 것은 아닙니다.

### 12.7 generate_master_catalog.py는 실행 중인 서비스가 아니다

`scripts/generate_master_catalog.py`는 개발 보조 도구입니다. V001의 테이블·타입·길이·필수값·기본값을 읽어 `MasterCatalog.java`의 고정 허용 목록을 생성합니다. 인증 비공개 칸과 감사 칸은 걸러 냅니다.

API 요청마다 DDL을 분석하거나 임의 SQL을 받아 실행하는 프로그램이 아닙니다. 실행하면 MasterCatalog.java 파일을 다시 쓰므로, 앞으로 schema를 확장할 때 생성 결과와 서비스 규칙을 함께 검토해야 합니다. 현재 원본 생성기는 아직 18개 이름을 DDL 순서와 zip으로 묶는 이전 버전입니다.
지금의 21개 표·NUMBER(p,s)와 맞지 않으므로 그대로 재실행하면 안 됩니다.
검토 폴더의 [생성기 수정안](F:/java/workspace/erpcom/docs/review/Level1_20261014/proposal/scripts/generate_master_catalog.py)은
21개 테이블명 대응과 소수 자릿수를 처리합니다. 원본 생성기 동기화는 이번 문서 수정에 포함하지 않았습니다.

<a id="chapter-13"></a>
## 13. 자동 테스트가 확인하는 것

### 13.1 테스트는 작은 연습 문제다

테스트는 가짜 자료를 준비하고 “이 상황에서는 성공해야 한다”, “이 상황에서는 오류여야 한다”를 확인하는 프로그램입니다. 사람이 매번 같은 버튼을 눌러 확인하지 않아도 중요한 규칙이 깨지지 않았는지 알 수 있습니다.

현재 저장된 최근 결과는 `CommonCoreIntegrationTests` 23개, `DevelopmentConfigurationTests` 5개, `MessageOutageTests` 1개로 합계 **29개 성공, 실패 0, 오류 0**입니다. 이 문서 작성에서 테스트를 다시 실행한 것은 아니며, 기존 `build/test-results/test` 결과를 확인했습니다.

이 결과는 실제 Oracle 접속·실제 로그인·실제 Nexacro 화면까지 검증했다는 뜻은 아닙니다. 아래의 범위를 확인한 결과입니다.

### 13.2 TestDatabaseConfiguration.testSchema(): 연습용 DB 준비

`TestDatabaseConfiguration.java`는 H2 연습 DB에 사용할 schema를 만듭니다. `testSchema(DataSource)`가 통합 V001·V002를 읽고 일부 Oracle 구문을 H2에서 읽을 수 있게 바꿉니다.

- Oracle INSERT ALL을 여러 INSERT로 펼칩니다.
- 일부 Oracle 함수 기반 unique index를 제거합니다.
- 로그인 대문자 중복과 기본 언어 유일성은 테스트용 생성 컬럼·index로 다시 만듭니다.
- 기간 겹침은 서비스 검사로 확인합니다.

통합 테스트에서는 Flyway 자동 실행을 끄고 이 초기화 도구를 사용합니다. 그러므로 H2 테스트 성공은 원본 Oracle Flyway SQL과 실제 Oracle 잠금을 모두 직접 검증한 결과가 아닙니다.

### 13.3 CommonCoreIntegrationTests의 준비 메서드

`users()`는 각 테스트 전에 가짜 사용자 1(admin)과 2(viewer)를 넣고, 1에게 ROLE 10, 2에게 ROLE 50을 연결합니다. `@Transactional` 테스트의 변경은 연습이 끝나면 되돌립니다.

`@MockitoBean CurrentUserProvider identity`는 실제 로그인을 대신하는 가짜 신분 제공자입니다. 테스트에서 1L이나 2L을 반환하게 해 각 권한을 확인합니다. 이런 테스트용 교체 코드를 일반 요청 헤더 인증으로 쓰고 있는 것은 아닙니다.

private `req(values)`는 간단한 신규 MasterRequest를 만듭니다. private `code(expected, action)`는 action이 ApiException을 던지고 그 messageCode가 기대값과 같은지 검사합니다.

### 13.4 통합 테스트 23개를 쉬운 말로 읽기

| 테스트 메서드 | 확인하는 연습 문제 |
|---|---|
| `allTablesLoad()` | 21개 resource 정의가 있고 각 표를 읽을 수 있는가 |
| `anonymousRequestsCannotSpoofActor()` | 헤더에 번호를 적어도 익명 사용자가 관리자를 사칭하지 못하는가 |
| `nexacroPreflightIsAllowedWithoutBypassingAuthentication()` | 허용 origin의 사전 통신은 되지만 실제 익명 요청은 막는가 |
| `unregisteredCorsOriginIsDenied()` | 등록 안 된 화면 origin의 사전 요청을 막는가 |
| `apiDocumentationIsDisabledByDefault()` | Swagger를 끈 테스트 설정에서 API 명세가 안 보이는가 |
| `messageFallbackUsesConfiguredChainAndStatus()` | 일본어 번역이 없으면 설정대로 영어로 가고 상태를 유지하는가 |
| `languageCyclesTerminate()` | 잘못된 언어 순환이 있어도 조회가 끝나고 인자 문장을 만들 수 있는가 |
| `inactiveFallbackLanguageAndMalformedHeaderUseActiveDefault()` | 비활성 대체 언어·잘못된 헤더에서 활성 기본 언어를 쓰는가 |
| `standardHttpErrorsUseCommonFormat()` | 405·406·415 오류가 공통 JSON 메시지로 나오는가 |
| `crudControlsAuditVersionAndLogicalDeletion()` | 만든 사람·고친 사람·버전·비활성화가 맞고 옛 버전 수정을 막는가 |
| `identifiersAndPrivateColumnsCannotBeInjected()` | 임의 SQL 이름·암호 칸·감사 칸 조작·과도한 페이지 크기를 막는가 |
| `compositeKeysAndNullableOverridesWork()` | 코드 복합키와 개인 null 상속이 올바른가 |
| `crossCompanyOrganizationAndHierarchyCyclesRejected()` | 다른 회사 소속 연결과 부서·메뉴 순환을 막는가 |
| `expiredInactiveAndFutureRolesAreExcluded()` | 만료·비활성·미래 역할은 지금 권한에 포함하지 않는가 |
| `userDeniesOverrideRoleAllowsAndMenuGatesButtons()` | 개인 버튼 금지가 ROLE 허용을 지우고 메뉴 금지가 버튼도 막는가 |
| `roleUnionDoesNotWidenWriteScope()` | 회사 조회 + 자기 수정이 회사 수정으로 확대되지 않는가 |
| `overlappingAssignmentsAndInvalidPeriodsRejected()` | 기간 중복과 시작보다 빠른 종료를 막는가 |
| `disabledMasterAndUserRevokePermissionsImmediately()` | 배치나 사용자 비활성 뒤 다시 계산한 권한이 제거되는가 |
| `authenticatedAdminApiAndViewerDenial()` | 관리자 등록은 되고 조회 사용자의 공통 관리는 막는가 |
| `referenceDataIsLocalizedAndFilteredByEffectivePermissions()` | 허용 메뉴·버튼만 주고 표시 이름을 번역하는가 |
| `limitedScopeCannotAccessCentralAdministration()` | OWN 범위로 중앙 관리 목록·등록을 이용하지 못하는가 |
| `filterErrorsUseSameMessageAndStatus()` | 필터용 write()도 같은 상태·문장으로 응답하는가 |
| `everyResourceSupportsCreateUpdateDeactivateAndReenable()` | 기존 18개 자료의 등록·수정·비활성·재활성화와 버전 증가. 새 3개는 Level1BasisReviewTests에서 별도로 확인 |

마지막 테스트는 관계가 있는 자료들을 먼저 모두 등록한 다음 바꾸어 보는 방식입니다. 앞에서 참조할 언어·메시지 등을 미리 비활성화해 뒤 자료를 만들 수 없는 순서 실수를 피합니다.

### 13.5 DevelopmentConfigurationTests의 다섯 검사

이 테스트는 실제 `.idea/mysettings.ini`를 읽지 않습니다. `dev()`가 임시 폴더에 가짜 설정 파일을 만들고 그 파일 위치로 import합니다. 실제 비밀번호나 DB를 사용할 필요가 없습니다.

| 메서드 | 확인하는 것 |
|---|---|
| `devImportsIniAndResolvesApplicationSettings()` | INI의 포트·DB 값·풀·Flyway·Swagger·CORS·예약 설정이 연결되는가 |
| `explicitOverridesWinOverPrivateIni()` | 명시적으로 준 설정이 파일 값을 덮는가 |
| `missingOptionalIniStillAllowsEnvironmentConfiguration()` | optional 파일이 없어도 외부 값으로 설정할 수 있는가 |
| `nonDevProfileDoesNotImportPrivateIni()` | dev 아닌 프로필은 개인 파일을 안 읽는가 |
| `corsRejectsWildcardAndOriginsWithPaths()` | 와일드카드·경로·사용자 정보가 있는 origin을 막는가 |

중첩 `BindingConfiguration`은 CommonCorsProperties를 테스트 환경에 등록하는 구성 클래스입니다. 로그인 처리나 DB 테이블을 만드는 클래스가 아닙니다.

### 13.6 MessageOutageTests의 장애 검사

`databaseOutageHasSafeFallbackWithoutRecursion()`은 `MessageMapper.languages()`가 계속 예외를 던지게 합니다. `MessageServiceImpl.get()`이 403의 안전 fallback을 만들고 `language()`가 ko를 반환하며, DB를 무한히 호출하지 않는지 확인합니다.

전체 서버를 실제로 꺼뜨리는 테스트가 아니라 Mapper를 가짜로 바꿔 메시지 서비스의 장애 처리만 확인합니다.

### 13.7 실행 방법과 검증의 경계

```powershell
.\gradlew.bat test
.\gradlew.bat bootJar
```

test는 자동 연습을 실행하고, bootJar는 실행 가능한 JAR를 만듭니다. 현재 build.gradle은 Java 21, Spring Boot 4.0.8, MyBatis starter 4.0.1, springdoc 3.1.0을 사용합니다. Oracle JDBC와 Oracle Flyway 지원은 실행 의존성, H2는 테스트 의존성입니다.

환경에 따라 의존성 다운로드가 필요할 수 있습니다. `--offline`은 이미 준비한 의존성만 사용하므로 모든 PC에서 처음부터 성공하는 옵션은 아닙니다.

이 PC에서 Windows 임시 소켓 경로 오류를 피하려고 기존 검증 때 사용했던 방법은 다음과 같습니다.

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp'
.\gradlew.bat test bootJar --offline --no-daemon
```

이것은 특정 Gradle 환경 문제에 대한 방법이지 회사 업무 소스의 기능이 아닙니다. 실제 Oracle migration, Oracle 동시 잠금, Level 2 인증, 업무 데이터 범위와 화면 동작은 해당 환경과 구현이 준비된 뒤 별도로 확인해야 합니다.

### 13.8 단위·통화·표시 설정 테스트 10개

현재 원본에도 [Level1BasisReviewTests.java](F:/java/workspace/erpcom/src/test/java/com/erpcom/erpcom/Level1BasisReviewTests.java)가 있습니다.
아래 메서드는 새 기능을 검증하는 연습 문제입니다.

| 메서드 | 확인할 내용 |
|---|---|
| schemaHas21MastersAndZeroVersions() | 21개 정의와 초기 버전, 단위·통화·표시 Seed 개수 |
| anonymousReferenceQueriesAndHeaderSpoofingAreRejected() | 미인증 조회와 사용자 헤더 위장 거절 |
| quantitiesUseExactMassDecimalAndBinaryConversions() | KG/G·GB/MB·GIB/MIB 환산과 서로 다른 기준 거절 |
| amountDisplayPreservesOriginalAndUsesFieldThenMenuThenSystemFallback() | 표시 설정 우선순위·원본 금액 보존·통화 일치 |
| policyLookupRequiresItsMenuAndAdminWritesRequireButtons() | 메뉴 조회·관리 버튼 권한 |
| decimalFactorsRemainDecimalsAndRejectPrecisionLoss() | 소수 계수 보존과 허용 정밀도 초과 거절 |
| sameDimensionCanonicalRootsAndStableUnitDefinitionsAreEnforced() | 같은 종류·최종 기준·정의 변경 제한·활성 참조 |
| newMasterCrudVersionsAndAuditUseActor() | 새 세 자료의 관리·감사·버전 충돌·재활성화 |
| invalidCodesScalesDivisorsAndRoundingAreRejected() | 코드·소수 자릿수·배율·반올림 입력 검사 |
| databaseRejectsMissingHalfOfUnitConversionPair() | 기준 단위와 계수 중 하나만 있는 자료 거절 |

평면 basis 검토 사본에서는 기존 29개와 새 10개, 총 39개가 격리 H2 환경에서 통과했습니다.
이번 문서 갱신은 그 결과와 현재 파일을 설명하는 작업이며 원본 테스트나 실제 Oracle을 다시 실행하지 않았습니다.

<a id="chapter-14"></a>
## 14. 실제로 읽고 추적하는 연습

### 14.1 처음부터 모든 import를 읽지 않아도 된다

Java 파일 맨 위의 package와 import는 “이 파일의 주소”와 “이 파일에서 사용할 다른 도구의 주소”입니다. 처음에는 메서드 본문과 연결되는 파일 이름부터 보세요.

회사 등록을 읽는다면 다음 순서가 좋습니다.

1. `MasterController.java`의 `create()`에서 URL과 전달 인자를 봅니다.
2. `MasterService.java`의 `create()`에서 약속의 입력·결과 모양을 봅니다.
3. `MasterServiceImpl.java`의 `create()`에서 처리 순서를 봅니다.
4. `MasterCatalog.java`의 `companies`에서 허용 칸을 봅니다.
5. `MasterMapper.java`의 `insert()`와 `MasterMapper.xml`의 같은 id를 나란히 봅니다.
6. `V001`의 TB_COMPANY를 보고 저장 대상의 컬럼을 확인합니다.
7. 통합 `V001`의 VERSION_NO와 sequence를 보고 추가 규칙을 확인합니다.

이 순서로 보면 SQL이나 annotation을 먼저 외우지 않아도 전체 길을 알 수 있습니다.

### 14.2 디버거에서 멈춰 볼 위치

**breakpoint**는 실행을 잠시 멈추고 값이 무엇인지 보는 표시입니다. IDE에서 해당 줄 왼쪽에 표시할 수 있습니다. 코드 이해에 도움이 되는 위치는 다음과 같습니다.

| 보고 싶은 것 | 멈출 파일·메서드 | 확인할 값 |
|---|---|---|
| 인증 사용자가 연결되었는가 | `RequestUserService.require()` | provider가 돌려준 id |
| 어떤 API 규칙이 적용되는가 | `CommonWebConfiguration`의 `preHandle()` | rule의 menu·button·level, 적용 scopes |
| ROLE과 개인 예외가 어떻게 합쳐지는가 | `PermissionServiceImpl.get()` | menus, buttons, override |
| 입력에 어떤 기본값이 붙었는가 | `MasterServiceImpl.normalize()` | input과 반환할 values |
| 등록 번호가 무엇인가 | `MasterServiceImpl.create()` | nextId 이후 values의 기본키 |
| 수정할 전체 자료가 무엇인가 | `MasterServiceImpl.update()` | 기존 행과 merged, expectedVersion |
| 번역이 어떤 언어로 넘어가는가 | `MessageServiceImpl.get()` | next, visited, info.messageText() |
| 오류가 어떤 문장으로 나오는가 | `ApiErrorWriter.response()` | info, status, text |
| 화면 메뉴가 왜 빠졌는가 | `ReferenceDataServiceImpl.bootstrap()` | grants의 메뉴와 permission.allows() |

private 검사 메서드에 바로 값을 넣어 실행하기보다 공개 요청이나 테스트에서 흐름을 시작하면 앞뒤 맥락을 같이 볼 수 있습니다.

### 14.3 “회사 등록이 401”이면 어디를 볼까

1. 기본 `CurrentUserProvider`는 null을 반환하므로 현재 Level 1 단독 실행에서는 예상 가능한 결과입니다.
2. Level 2를 연결했다면 그 provider의 `userId()`가 검증된 번호를 주는지 봅니다.
3. `PermissionMapper.user()`가 그 사용자를 읽는지 봅니다.
4. 사용자의 USE_YN, USER_STATUS, 소속 회사·사업장·부서가 활성인지 봅니다.

이 단계에서는 회사 이름의 JSON을 바꾸어도 인증 문제를 해결하지 못합니다. 아직 MasterController.create()까지 도달하지 않았기 때문입니다.

### 14.4 “로그인한 것 같은데 403”이면 어디를 볼까

1. `PermissionServiceImpl.get()`에서 COMMON_ADMIN 메뉴 9000의 Grant가 있는지 봅니다.
2. 등록이면 WRITE_ALL과 COMPANY를 **같은 Grant**가 만족하는지 봅니다.
3. COMMON_ADMIN의 BTN_NEW가 메뉴-버튼 배치에 있는지 봅니다.
4. ROLE 버튼 허용이나 개인 Y가 있고 개인 N으로 지워지지 않았는지 봅니다.
5. 사용자-ROLE과 개인 예외의 기간이 현재 시각에 유효한지 봅니다.

ROLE의 이름이 SYSTEM_ADMIN이라는 이유만으로 통과하지 않습니다. 저장된 명시적 허용을 읽습니다.

### 14.5 “수정이 409”이면 어디를 볼까

먼저 `ApiError.messageCode`가 ERR_CONFLICT인지 확인합니다. 409는 버전 불일치뿐 아니라 UNIQUE·FK·기간 겹침에도 사용됩니다.

버전 문제면 다시 GET으로 최신 VERSION_NO를 읽고 새 expectedVersion을 보냅니다. 같은 코드 중복이면 다른 코드 또는 기존 행 수정이 필요합니다. 기간 중복이면 예전 연결·예외의 기간과 USE_YN을 확인합니다. 언제나 expectedVersion을 0으로 보내는 것은 해결 방법이 아닙니다.

### 14.6 “한국어를 보냈는데 영어가 나온다”면 어디를 볼까

`MessageServiceImpl.language()`는 사용자 설정을 요청 lang보다 먼저 사용합니다. 사용자 LANG_CODE가 en인지 확인하세요. 선택된 언어와 번역문의 언어도 다를 수 있습니다.

TB_MESSAGE_LANG에 원하는 언어의 행이 있는지, USE_YN과 TRANSLATED_YN이 Y인지 봅니다. 없다면 TB_LANGUAGE의 fallback을 따라 다른 언어를 찾습니다. `MessageInfo.langCode`나 `ApiError.langCode`가 실제 문장 언어를 알려 줍니다.

### 14.7 “화면에 저장 버튼이 안 보인다”면 어디를 볼까

1. TB_BUTTON에 BTN_SAVE가 활성인지 봅니다.
2. 해당 메뉴와 버튼의 TB_MENU_BUTTON 배치가 활성인지 봅니다.
3. permissions/me에 그 메뉴와 버튼 짝이 있는지 봅니다.
4. 메뉴가 최소 READ_PARTIAL을 만족해 bootstrap에 포함되는지 봅니다.
5. bootstrap에는 있는데 화면에 없다면 화면 프로그램의 표시 처리를 봅니다.

서버는 버튼 정보를 반환할 뿐 실제 Nexacro 컴포넌트를 그리지 않습니다. 같은 BTN_SAVE라도 다른 메뉴의 허용은 별도입니다.

### 14.8 “개인 설정이 안 먹힌다”면 어디를 볼까

dev 프로필, 실행 작업 폴더, 파일명 `.idea/mysettings.ini`, Properties 문법, ERPCOM 변수 철자를 확인합니다. 환경변수나 실행 인자가 같은 키를 덮는지도 봅니다. dev를 안 켠 기본 실행은 INI를 읽지 않습니다.

CORS는 `http://localhost:9093`처럼 origin만 적어야 합니다. `/index.html`을 뒤에 붙이면 `CommonCorsProperties.isValidOrigin()`이 시작 단계에서 거절합니다. Swagger 주소의 포트는 최종 server.port에 맞춥니다.

### 14.9 회사·사업장·부서·사용자 등록의 순서 연습

새 소속을 만들려면 부모가 먼저 필요합니다. 아래 values는 형식을 보여 주는 예이며, 실제 권한이 준비된 뒤 사용해야 합니다.

**회사부터 등록합니다.**

```json
{ "values": { "COMPANY_CODE": "LEARN", "COMPANY_NAME": "배움 회사" } }
```

POST companies 응답에서 받은 COMPANY_ID를 기억합니다. 아래 예의 1000000이 그 번호라고 가정합니다.

**그 회사의 사업장을 등록합니다.**

```json
{
  "values": {
    "COMPANY_ID": 1000000,
    "SITE_CODE": "MAIN",
    "SITE_NAME": "본사"
  }
}
```

POST sites 응답에서 SITE_ID를 받습니다. 아래 예에서는 1000001로 가정합니다. 실제 sequence 번호는 꼭 연속일 필요가 없습니다.

**그 사업장의 부서를 등록합니다.**

```json
{
  "values": {
    "COMPANY_ID": 1000000,
    "SITE_ID": 1000001,
    "DEPT_CODE": "DEV",
    "DEPT_NAME": "개발부"
  }
}
```

POST departments 결과의 DEPT_ID가 1000002라고 가정합니다. 첫 부서라 상위 부서를 보내지 않았습니다.

**소속에 맞는 사용자 기본정보를 등록합니다.**

```json
{
  "values": {
    "LOGIN_ID": "learning.user",
    "USER_NAME": "배움 사용자",
    "COMPANY_ID": 1000000,
    "SITE_ID": 1000001,
    "DEPT_ID": 1000002,
    "LANG_CODE": "ko"
  }
}
```

POST users로 등록합니다. USER_STATUS 기본값은 ACTIVE입니다. 비밀번호를 보낸 예가 아닌 이유는 이 API가 사용자 공통정보를 만드는 API이기 때문입니다. 이 자료만으로 LOCAL 로그인까지 완성되지 않습니다.

이 네 번의 요청 모두 `MasterController.create()`와 `MasterServiceImpl.create()`를 지나지만 resource 정의와 조직 검사 대상이 다릅니다. 이 점이 공통 처리 구조의 핵심입니다.

### 14.10 새로운 번역 이름을 만드는 순서 연습

새 공통코드의 이름을 추가한다고 합시다. 먼저 messages에 코드와 정책을 만들고, message-translations에 번역을 만들고, codes에서 그 메시지 코드를 연결합니다.

```json
{ "values": { "MESSAGE_CODE": "COD_EDU_READY", "MESSAGE_TYPE": "CODE_NAME" } }
```

```json
{ "values": { "MESSAGE_CODE": "COD_EDU_READY", "LANG_CODE": "ko", "MESSAGE_TEXT": "준비" } }
```

```json
{ "values": { "MESSAGE_CODE": "COD_EDU_READY", "LANG_CODE": "en", "MESSAGE_TEXT": "Ready" } }
```

새 분류라면 code-groups를 먼저 등록한 뒤 codes를 등록합니다.

```json
{ "values": { "CODE_GROUP": "EDU_STAGE", "DESCRIPTION": "설명용 단계" } }
```

```json
{
  "values": {
    "CODE_GROUP": "EDU_STAGE",
    "CODE": "READY",
    "CODE_NAME_MSG_CODE": "COD_EDU_READY"
  }
}
```

인증 사용자가 codes/EDU_STAGE를 조회하면 `ReferenceDataServiceImpl.codes()`가 CODE_NAME_MSG_CODE를 MessageService에 보내 표시 이름을 얻습니다. 코드 READY는 그대로이고 표시 문장만 언어에 따라 달라집니다.

### 14.11 사용자 예외의 null과 NONE을 직접 비교하기

조회 사용자가 ROLE에서 READ_ALL+OWN을 받았다고 가정합니다. user-menu-permissions에 아래 값을 넣으면 조회 수준은 그대로 두고 범위만 DEPT로 바꿉니다.

```json
{
  "values": {
    "USER_ID": 2,
    "MENU_ID": 1010,
    "ACCESS_LEVEL": null,
    "DATA_SCOPE": "DEPT"
  }
}
```

반대로 `ACCESS_LEVEL="NONE"`을 넣으면 메뉴 금지입니다. 같은 기간에 새 예외를 추가로 겹쳐 넣지 말고, 기존 예외를 최신 버전으로 수정하거나 유효기간을 조정해야 합니다. 이것이 `validatePeriod()`와 `PermissionServiceImpl.get()`의 규칙입니다.

<a id="chapter-15"></a>
## 15. 모든 Java 파일과 메서드 찾기

여기는 앞에서 배운 내용을 실제 소스로 찾아갈 때 쓰는 찾아보기입니다. 현재 실행 소스는 **41개 Java 파일**, 테스트·테스트 준비 소스는 **5개 Java 파일**입니다. 모든 파일을 아래에 연결했습니다.

public 메서드는 다른 담당자가 부르는 출입구이고 private 메서드는 그 클래스 안의 보조 작업입니다. 인터페이스에는 작업의 약속만 있고, record에는 Java가 자동 생성하는 꺼내기 메서드가 있습니다. 생성자는 파일 이름과 같은 이름이며 Spring이 담당자들을 연결할 때 호출합니다.

### 15.1 시작 파일 1개

| 파일 | 메서드·역할 |
|---|---|
| [ErpcomApplication.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/ErpcomApplication.java) | `main()` — Spring Boot 실행 시작 |

### 15.2 공통 관리 파일 9개

| 파일 | 메서드·역할 |
|---|---|
| [MasterCatalog.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/master/MasterCatalog.java) | private 생성자 — 목록 보관 클래스를 임의 생성하지 않게 함. `RESOURCES`는 21개 고정 정의. `Kind`는 TEXT/NUMBER/DECIMAL/TIME. `Column`은 scale을 포함한 필드 정의 record |
| [MasterResource.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/master/MasterResource.java) | 생성자 — key·column 목록 복사, USE_YN 추가. `named()` — resource 허용 목록 조회. `getTable()`, `getKeys()` — XML용 값 제공. `getOrderBy()` — PK 정렬 문자열. `getProjection()` — 공개 조회 컬럼 문자열 |
| [MasterRequest.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/master/MasterRequest.java) | 입력 record. 자동 `values()`, `expectedVersion()`으로 값을 꺼냄. NotNull/Size/Min 기본 입력 검사 |
| [MasterPage.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/master/MasterPage.java) | 페이지 결과 record. 자동 `items()`, `total()`, `offset()`, `limit()` |
| [MasterController.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/master/MasterController.java) | 생성자 — service·users 연결. `resources()`, `list()`, `get()`, `create()`, `update()`, `deactivate()` — 여섯 HTTP 관리 창구 |
| [MasterService.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/master/MasterService.java) | `list()`, `get()`, `create()`, `update()`, `deactivate()`의 약속 |
| [MasterServiceImpl.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/master/MasterServiceImpl.java) | 생성자 — mapper 연결. 위 다섯 작업의 실제 구현. 아래 private 메서드 목록 참고 |
| [MasterMapper.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/master/MasterMapper.java) | DB 작업 약속 11개: `list()`, `count()`, `find()`, `nextId()`, `now()`, `insert()`, `update()`, `deactivate()`, `lockUser()`, `lockHierarchy()`, `overlaps()` |
| [MyBatisConfiguration.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/master/MyBatisConfiguration.java) | `databaseIdProvider()` — Oracle/H2 식별과 XML의 DB별 SQL 선택 |

`MasterCatalog.Column`의 자동 꺼내기 메서드는 `name()`, `kind()`, `length()`, `required()`, `defaultValue()`, `scale()`입니다. `MasterResource`의 record 꺼내기는 `name()`, `table()`, `keys()`, `identity()`, `columns()`입니다. `getTable()` 등은 record 꺼내기와 별도로 XML에서 쓰기 편하게 명시한 메서드입니다.

**MasterServiceImpl의 private 메서드 전체**

| 메서드 | 무엇을 맡는가 | 부르는 쪽 |
|---|---|---|
| `find()` | Mapper 한 행 조회, 없으면 ERR_NOT_FOUND | get/create/update/deactivate 및 관계 검사 |
| `keys()` | URL의 ~ 구분 키를 검사하고 타입 변환 | get/update/deactivate |
| `normalize()` | 허용 필드 확인, 변환, 신규 기본값 채움 | create/update |
| `convert()` | TEXT/NUMBER/TIME, 필수·길이·Y/N 등 검사 | normalize/validate/keys/validatePeriod |
| `validate()` | 전체 자료와 resource별 규칙 선택 | create/update |
| `validateOrganization()` | 회사·사업장·부서 소속 정합성 | validate |
| `validateHierarchy()` | 부모 활성·같은 조직·순환 검사 | validate |
| `same()` | 숫자·문자열·null의 의미상 동일 비교 | 조직·계층 검사 |
| `idToken()` | 계층 번호를 일정한 문자열로 표현 | validateHierarchy |
| `validatePeriod()` | 유효기간과 중복 확인, 사용자 잠금 | validate |
| `validateLanguage()` | 표준 언어·기본값·fallback 순환 검사 | validate |
| `validateTranslation()` | MessageFormat 문법과 인자 번호 검사 | validate |
| `invalid()` | ERR_INVALID_INPUT 예외 생성 | 입력 검사 전반 |

**MasterMapper.java ↔ XML 전체 대응**

SQL 파일은 [MasterMapper.xml](F:/java/workspace/erpcom/src/main/resources/mapper/common/MasterMapper.xml)입니다.

| Java 메서드 / XML id | SQL의 일 |
|---|---|
| `list` | 공개 projection, 활성 조건, 키 정렬, OFFSET/FETCH |
| `count` | 같은 활성 조건의 전체 개수 |
| `find` | PK로 한 행 조회, 비활성도 조회 가능 |
| `nextId` | SQ_COMMON_ID.NEXTVAL |
| `now` | DB SYSDATE |
| `insert` | 허용 컬럼과 CREATED_BY 저장 |
| `update` | 보낸 컬럼 수정, 감사 정보·버전 증가, PK+버전 조건 |
| `deactivate` | USE_YN=N, 감사 정보·버전 증가, PK+버전 조건 |
| `lockUser` | 사용자 행 SELECT FOR UPDATE |
| `lockHierarchy` | Oracle 표 잠금, H2용 대체 문장 |
| `overlaps` | 같은 연결 짝의 활성 기간 겹침 개수 |

XML의 `keyWhere`는 Java 메서드가 아닙니다. 여러 SQL이 공통으로 사용하는 “PK 조건 부분”입니다.

### 15.3 웹 연결 파일 6개

| 파일 | 메서드·역할 |
|---|---|
| [CurrentUserProvider.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/web/CurrentUserProvider.java) | `userId(request)` — Level 2가 검증한 신분을 연결하는 인터페이스 |
| [RequestUserService.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/web/RequestUserService.java) | 생성자 — provider·permissions 연결. `require()` — 유효 사용자 필수 확인. `language()` — 사용자 언어, ApiException이면 null |
| [RequiresPermission.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/web/RequiresPermission.java) | annotation 항목 `menu()`, `button()`, `level()`, `scopes()` — API 필요 권한 설정 |
| [CommonWebConfiguration.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/web/CommonWebConfiguration.java) | `commonCorsConfigurer()`와 내부 `addCorsMappings()` — CORS. `unauthenticatedUserProvider()` — 인증 연결 없으면 null. `commonPermissionConfigurer()`와 내부 `addInterceptors()`, `preHandle()` — API 앞 권한 검사 |
| [CommonCorsProperties.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/web/CommonCorsProperties.java) | 생성자 — origin 기본값·trim. 자동 `allowedOrigin()`. `isValidOrigin()` — 정확한 HTTP(S) origin인지 검증 |
| [ReferenceController.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/web/ReferenceController.java) | 생성자 — messages·permissions·users 연결. `languages()`, `message()` — 공개 언어·메시지. `permissions()` — 인증된 내 권한 |

`RequestUserService.language()`는 ApiException만 잡습니다. 그 밖의 DB RuntimeException을 모든 곳에서 삼키는 메서드는 아닙니다. 오류 응답의 `ApiErrorWriter.response()`가 사용자 언어 조회 장애를 추가로 잡습니다.

### 15.4 권한 파일 6개

| 파일 | 메서드·역할 |
|---|---|
| [AccessLevel.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/permission/AccessLevel.java) | enum 생성자 — mask 보관. `allows()` — 필요한 능력 포함 검사 |
| [DataScope.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/permission/DataScope.java) | OWN/DEPT/SITE/COMPANY 선언. 직접 작성한 업무 메서드 없음 |
| [PermissionModels.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/permission/PermissionModels.java) | private 생성자 — 모델 묶음 용도. 8개 record. `MenuPermission` 생성자 — Grant 복사. `MenuPermission.allows()` — 메뉴의 수준 허용 |
| [PermissionService.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/permission/PermissionService.java) | `user()`, `get()`, `requireMenu()`, `requireButton()`, `requireScope()`, `requireData()` 약속 |
| [PermissionServiceImpl.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/permission/PermissionServiceImpl.java) | 생성자 — mapper 연결. 위 여섯 작업 구현. private `key()` — 메뉴·버튼 짝 식별 |
| [PermissionMapper.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/permission/PermissionMapper.java) | `user()`, `roleMenus()`, `userMenus()`, `roleButtons()`, `userButtons()`, `menuId()`, `buttonId()` DB 약속 |

SQL은 [PermissionMapper.xml](F:/java/workspace/erpcom/src/main/resources/mapper/common/PermissionMapper.xml)이며 Java 메서드와 같은 id가 있습니다. XML의 `activeRoles`는 활성 역할 연결·기간의 공통 SQL 조각이며 Java 메서드가 아닙니다.

record의 꺼내기 이름도 확인해 봅시다.

- UserContext: `userId()`, `companyId()`, `siteId()`, `deptId()`, `langCode()`.
- Grant: `accessLevel()`, `dataScope()`.
- MenuGrant·MenuOverride: `menuId()`, `accessLevel()`, `dataScope()`.
- ButtonGrant: `menuId()`, `buttonId()`, `accessYn()`.
- MenuPermission: `menuId()`, `grants()`.
- DataTarget: `companyId()`, `siteId()`, `deptId()`, `ownerUserId()`.
- PermissionSnapshot: `menus()`, `buttons()`.

### 15.5 메시지 파일 5개

| 파일 | 메서드·역할 |
|---|---|
| [LanguageInfo.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/message/LanguageInfo.java) | 언어 결과 record. `langCode()`, `langName()`, `nativeName()`, `fallbackLangCode()`, `defaultYn()` 자동 생성 |
| [MessageInfo.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/message/MessageInfo.java) | 메시지 결과 record. `messageCode()`, `messageType()`, `httpStatus()`, `severity()`, `langCode()`, `messageText()`, `argCount()` |
| [MessageService.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/message/MessageService.java) | `languages()`, `get()`, `text()`, `language()` 약속 |
| [MessageServiceImpl.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/message/MessageServiceImpl.java) | 생성자 — mapper 연결. 위 네 작업 구현. private `match()`, `defaultLanguage()`, `fallback()` — 지원 언어 맞춤·기본값·장애 대응 |
| [MessageMapper.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/message/MessageMapper.java) | `languages()` — 활성 언어. `find()` — 메시지 정책과 번역 조회 |

SQL은 [MessageMapper.xml](F:/java/workspace/erpcom/src/main/resources/mapper/common/MessageMapper.xml)입니다. XML의 languages와 find가 같은 Java 메서드에 연결됩니다.

### 15.6 오류 파일 4개

| 파일 | 메서드·역할 |
|---|---|
| [ApiException.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/error/ApiException.java) | 생성자 — 코드·인자 복사. `messageCode()`, `args()` — 오류 정보 제공 |
| [ApiError.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/error/ApiError.java) | 오류 응답 record. `messageCode()`, `message()`, `langCode()`, `status()`, `path()`, `timestamp()` |
| [ApiErrorWriter.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/error/ApiErrorWriter.java) | 생성자 — messages·users·ObjectMapper 연결. `response()` — 공통 오류 ResponseEntity. `write()` — 필터용 servlet 응답 쓰기 |
| [ApiExceptionHandler.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/error/ApiExceptionHandler.java) | 생성자 — writer 연결. `api()`, `validation()`, `conflict()`, `missing()`, `method()`, `media()`, `accept()`, `unexpected()` — 예외 분류 |

여기서 ResponseEntity는 본문뿐 아니라 HTTP 상태와 헤더를 함께 담는 Spring의 응답 상자입니다. ObjectMapper는 Java 상자를 JSON 글자로 바꾸는 도구입니다.

### 15.7 화면용 기준정보 파일 5개

| 파일 | 메서드·역할 |
|---|---|
| [ReferenceModels.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/reference/ReferenceModels.java) | private 생성자 — 모델 묶음. CodeRow/CodeInfo/MenuRow/ButtonRow/ButtonInfo/MenuInfo/Bootstrap record |
| [ReferenceDataController.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/reference/ReferenceDataController.java) | 생성자 — service·users·messages 연결. `bootstrap()`, `codes()` — 인증 사용자 화면 자료 요청 |
| [ReferenceDataService.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/reference/ReferenceDataService.java) | `codes()`, `bootstrap()` 약속 |
| [ReferenceDataServiceImpl.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/reference/ReferenceDataServiceImpl.java) | 생성자 — mapper·messages·permissions 연결. `codes()` — 코드 이름 번역. `bootstrap()` — 허용 메뉴·버튼과 이름 조립 |
| [ReferenceMapper.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/reference/ReferenceMapper.java) | `codes()`, `menus()`, `buttons()` — 활성 기준정보 DB 조회 |

SQL은 [ReferenceMapper.xml](F:/java/workspace/erpcom/src/main/resources/mapper/common/ReferenceMapper.xml)이며 codes/menus/buttons가 같은 Java 메서드에 연결됩니다.

ReferenceModels의 record 꺼내기는 다음과 같습니다.

| record | 자동 꺼내기 메서드 |
|---|---|
| CodeRow | `codeGroup()`, `code()`, `codeNameMsgCode()`, `sortOrder()`, `extValue1()`, `extValue2()`, `extValue3()` |
| CodeInfo | `codeGroup()`, `code()`, `name()`, `sortOrder()`, `extValue1()`, `extValue2()`, `extValue3()` |
| MenuRow | `menuId()`, `parentMenuId()`, `menuCode()`, `menuNameMsgCode()`, `menuType()`, `screenId()`, `formPath()`, `iconName()`, `sortOrder()` |
| ButtonRow | `menuId()`, `buttonId()`, `buttonCode()`, `buttonNameMsgCode()`, `sortOrder()` |
| ButtonInfo | `buttonId()`, `buttonCode()`, `name()`, `sortOrder()` |
| MenuInfo | `menuId()`, `parentMenuId()`, `menuCode()`, `name()`, `menuType()`, `screenId()`, `formPath()`, `iconName()`, `sortOrder()`, `grants()`, `buttons()` |
| Bootstrap | `user()`, `langCode()`, `menus()` |

record에는 위의 값 꺼내기 외에도 기본 비교 `equals()`, hash 값 `hashCode()`, 글자 표현 `toString()`이 자동 생성됩니다. 일반 업무 처리 메서드처럼 따로 구현한 코드는 아닙니다. enum의 `values()`·`valueOf()`도 Java가 제공합니다. `validate()`에서 AccessLevel.valueOf()를 쓰는 이유는 입력 이름이 실제 enum 값인지 확인하기 위해서입니다.

### 15.8 테스트 파일 5개

| 파일 | 어디를 읽으면 되는가 |
|---|---|
| [CommonCoreIntegrationTests.java](F:/java/workspace/erpcom/src/test/java/com/erpcom/erpcom/CommonCoreIntegrationTests.java) | `users()` 준비, 23개 테스트 메서드, private `req()`·`code()` — 13.3~13.4 참고 |
| [DevelopmentConfigurationTests.java](F:/java/workspace/erpcom/src/test/java/com/erpcom/erpcom/DevelopmentConfigurationTests.java) | private `dev()` 준비, BindingConfiguration, 5개 테스트 — 13.5 참고 |
| [MessageOutageTests.java](F:/java/workspace/erpcom/src/test/java/com/erpcom/erpcom/MessageOutageTests.java) | `databaseOutageHasSafeFallbackWithoutRecursion()` — 13.6 참고 |
| [TestDatabaseConfiguration.java](F:/java/workspace/erpcom/src/test/java/com/erpcom/erpcom/TestDatabaseConfiguration.java) | `testSchema()` — 통합 SQL 두 개의 H2 준비와 Oracle 구문 변환 |
| [Level1BasisReviewTests.java](F:/java/workspace/erpcom/src/test/java/com/erpcom/erpcom/Level1BasisReviewTests.java) | 단위·통화·표시·권한·정밀도·FK·버전 검사 10개 |

### 15.9 설정·빌드·개발 규칙 파일

| 파일 | 읽을 이유 |
|---|---|
| [AGENTS.md](F:/java/workspace/erpcom/AGENTS.md) | 이 구현의 개발 표준과 Level 1/2 경계 |
| [application.yaml](F:/java/workspace/erpcom/src/main/resources/application.yaml) | 기본 DB·Flyway·MyBatis·Swagger·서버 설정 |
| [application-dev.yaml](F:/java/workspace/erpcom/src/main/resources/application-dev.yaml) | dev INI import·연결 풀·CORS·예약 설정 |
| [.idea/mysettings.ini](F:/java/workspace/erpcom/.idea/mysettings.ini) | 개발 PC의 개인 설정. 비밀번호는 공유 문서에 옮기지 않음 |
| [build.gradle](F:/java/workspace/erpcom/build.gradle) | Java 버전·사용 도구·의존성·테스트 실행 정의 |
| [settings.gradle](F:/java/workspace/erpcom/settings.gradle) | Gradle 프로젝트 이름 |
| [generate_master_catalog.py](F:/java/workspace/erpcom/scripts/generate_master_catalog.py) | 고정 관리 목록 재생성 도구 |
| [.gitignore](F:/java/workspace/erpcom/.gitignore) | .idea·build 등 저장소 제외 대상 |

이 문서가 전체 흐름을 설명하고, 기존 COMMON_API.md·LEVEL1_DESIGN.md·LEVEL2_INTEGRATION.md는 API 목록이나 연결 계약을 추가로 확인할 때 사용할 수 있습니다.

### 15.10 common/basis의 Java 파일 5개

다섯 파일은 모두 `src/main/java/com/erpcom/erpcom/common/basis`에 함께 있습니다.
package는 `com.erpcom.erpcom.common.basis`이며 역할별 하위 폴더는 없습니다.

| 파일 | 주요 메서드·자료 |
|---|---|
| [BasisController.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/basis/BasisController.java) | units()·currencies()·amountFormats() HTTP 조회 |
| [BasisService.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/basis/BasisService.java) | 조회·convertQuantity()·displayAmount()의 약속 |
| [BasisServiceImpl.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/basis/BasisServiceImpl.java) | 실제 처리와 private readableMenu() 권한 확인 |
| [BasisMapper.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/basis/BasisMapper.java) | units()·unit()·currencies()·currency()·menu()·amountFormats() SQL 연결 |
| [BasisModels.java](F:/java/workspace/erpcom/src/main/java/com/erpcom/erpcom/common/basis/BasisModels.java) | UnitRow·UnitInfo·CurrencyRow·CurrencyInfo·MenuIdentity·AmountFormatRow·AmountFormatInfo·AmountDisplay record |

SQL은 [BasisMapper.xml](F:/java/workspace/erpcom/src/main/resources/mapper/common/BasisMapper.xml)입니다.
폴더가 하나여도 HTTP 입구·처리·DB 조회·자료 전달이라는 역할은 각 클래스가 계속 나누어 맡습니다.

<a id="chapter-16"></a>
## 16. 자주 생기는 오해와 용어 사전

### 16.1 구현 상태를 정확하게 기억하기

| 생각하기 쉬운 내용 | 현재 코드의 실제 동작 |
|---|---|
| 사용자 등록 = 로그인 완성 | 사용자 공통정보만 저장. 실제 인증은 Level 2 |
| dev 설정에 SSO enabled=true면 SSO 된다 | 예약 값이며 실제 SSO 처리 코드 없음 |
| bootstrap enabled=true면 ADMIN 자동 생성 | 시작 계정 생성 코드 없음 |
| /bootstrap은 계정 생성 API | 인증 사용자에게 화면 준비 자료를 주는 조회 API |
| SYSTEM_ADMIN 이름이면 무조건 통과 | 저장된 메뉴·버튼·범위를 명시적으로 검사 |
| DELETE면 DB 행이 지워짐 | USE_YN=N으로 비활성화 |
| PUT은 안 보낸 필드를 모두 비움 | 현재는 보낸 필드만 바꾸는 부분 수정 |
| VERSION_NO를 values에 넣는다 | expectedVersion으로 별도 전달, 실제 버전은 서버가 증가 |
| null 권한은 금지 | null은 상속, NONE은 메뉴 금지, 버튼 N은 개인 금지 |
| ROLE의 N이 다른 ROLE의 Y를 없앰 | ROLE 기본은 허용 합집합. 개인 N이 허용을 제거 |
| 회사 조회 + 자기 수정이면 회사 수정 가능 | 수준과 범위의 짝을 유지해 확장을 막음 |
| DEPT 범위면 모든 하위 부서 포함 | 현재 requireData는 같은 부서 번호만 확인 |
| 모든 업무 목록이 자동으로 범위 제한됨 | 업무 Service·SQL에서 별도 적용 필요 |
| 버튼 허용이면 메뉴 금지도 통과 | 메뉴 권한이 먼저 필요 |
| CORS 허용이면 로그인·권한도 허용 | 실제 요청의 인증·권한은 별도로 검사 |
| 캐시에 권한이 계속 남음 | 현재 결과를 DB에서 다시 계산, 영속 권한 캐시 없음 |
| 모든 문장이 요청 언어와 같음 | 번역이 없으면 설정 fallback의 실제 문장을 사용 |
| 테이블에 메뉴가 있으면 실제 화면도 존재 | 화면 경로를 담는 기준정보이며 화면 구현과 다름 |
| SQL·Java에 새 세 기준정보가 있으면 DB도 초기화됨 | 원본 소스 반영과 실제 Oracle Migration 실행은 별개 |
| CurrencyInfo record를 만들면 통화 테이블도 생김 | record는 전달 객체. DB 생성·조회·저장은 별도 |
| 원화의 기본 소수 자릿수가 0이면 592.3억원 표시 불가 | 통화 기본 자릿수와 화면 표시 자릿수는 다름 |
| 억원 화면에서는 DB에도 592.3을 저장 | 실제 금액 59,230,000,000 KRW를 보존하고 표시할 때 나눔 |
| 한국어를 영어로 바꾸면 원화가 달러로 바뀜 | 언어·표시 배율·환율에 의한 통화 변환은 서로 다름 |
| H2 테스트가 성공하면 Oracle 검증도 완료 | 원본 Oracle migration·잠금은 실제 환경에서 별도 확인 |

### 16.2 어려운 단어를 다시 찾아보기

| 단어 | 이 문서에서의 쉬운 뜻 |
|---|---|
| Level 1 | 조직·사용자·권한·메시지 등 모든 업무가 함께 쓰는 기반 |
| Level 2 | 실제 로그인과 세션·토큰 등 신분 확인 구현 |
| 인증(Authentication) | 이 사람이 누구인지 확인 |
| 인가(Authorization) | 확인된 사람이 무엇을 해도 되는지 확인 |
| Master / 기준정보 | 업무에서 반복해서 참고하는 회사·역할·코드 등의 자료 |
| Resource | 공통 관리 API에서 다루는 자료 종류의 이름 |
| Controller | HTTP 요청의 입구 |
| Service | 처리 순서와 규칙 담당 |
| Interface | 가능한 작업 모양의 약속 |
| Impl | 약속을 실제로 수행하는 구현 |
| Mapper | Java 작업과 SQL을 연결하는 DB 창구 |
| SQL | DB에서 읽고 저장하고 바꾸는 명령 |
| DDL | 테이블·인덱스·제약 같은 DB 구조를 만드는 SQL |
| DTO / record | 자료를 담아 전달하는 상자 |
| UNIT | 수량이 무엇을 기준으로 세어졌는지 표시하는 단위. EA·KG 등 |
| CURRENCY | 금액이 어떤 돈인지 표시하는 통화. KRW·USD 등 |
| 표시 배율 / DIVISOR | 화면에서 숫자를 줄여 보여 주기 위해 원래 금액을 나눌 수 |
| MINOR_UNIT_DIGITS | 통화 자체의 기본 소수 자릿수 |
| DECIMAL_PLACES | 화면에서 보여 줄 소수 자릿수 |
| ROUNDING_MODE | 반올림·버림 등 끝자리를 처리하는 규칙 |
| Bean | Spring이 만들어 연결해 사용하는 구성 객체 |
| DI / 의존성 주입 | 필요한 담당 객체를 Spring이 건네주는 방식 |
| Annotation | @로 붙여 프로그램에 알려 주는 표시 |
| Interceptor | Controller에 들어가기 전 등 MVC 요청 중간의 검사기 |
| Filter | MVC Controller보다 앞쪽 등 servlet 요청 흐름의 처리기 |
| Grant | 권한 수준과 범위를 함께 담은 허용 한 묶음 |
| Override | 기본 ROLE 결과의 일부를 개인 예외로 바꾸는 것 |
| DataScope | 소유자·부서·사업장·회사 중 자료 접근 범위 |
| Snapshot | 이번에 계산한 권한 결과 묶음 |
| Validation | 잘못된 입력과 관계를 검사 |
| Audit 컬럼 | 누가 언제 만들고 고쳤는지 기록하는 칸 |
| Transaction | 함께 성공하거나 실패하면 함께 되돌릴 DB 작업 묶음 |
| 낙관적 잠금 | 저장할 때 읽었던 버전과 현재 버전을 비교 |
| FOR UPDATE | 특정 DB 행을 transaction 동안 잠그는 조회 |
| Sequence | DB에서 새 번호를 발급하는 장치 |
| FK / 외래키 | 다른 표의 존재하는 키와 연결하는 규칙 |
| Unique | 같은 값이나 값의 짝이 둘 생기지 않게 하는 규칙 |
| CHECK | DB에서 허용할 값의 조건 |
| Projection | 조회 결과에 포함할 컬럼 목록 |
| Parameter binding | 사용자 값을 SQL 명령과 분리해 전달 |
| Migration | 순서 있는 DB 구조·자료 변경 파일 |
| Seed | 처음 사용할 기본 기준자료 |
| Checksum | 적용한 파일이 바뀌었는지 확인하는 요약값 |
| Baseline | migration 이력에서 출발 기준을 기록 |
| Profile | dev처럼 특정 실행 환경의 설정 묶음 |
| Placeholder | `${이름:기본값}`처럼 실행 시 값을 채울 자리 |
| Connection pool | 준비한 DB 연결을 여러 요청이 돌려 쓰는 장치 |
| Locale / BCP-47 | 언어·지역을 표시하는 규칙과 Java 언어 정보 도구 |
| Fallback | 원하는 번역이나 저장소를 못 쓸 때 사용할 대체 |
| MessageFormat | 문장의 {0}, {1} 같은 자리에 값을 넣는 도구 |
| Origin | http/https + 호스트 + 포트 조합 |
| CORS | 브라우저의 다른 origin 응답 접근 허용 규칙 |
| Preflight | 브라우저의 실제 요청 전 OPTIONS 사전 질문 |
| Swagger | API 주소와 입력·결과를 살펴보는 안내 도구 |
| Mock | 테스트에서 실제 담당자를 대신하는 가짜 객체 |
| H2 | 자동 연습에 사용하는 별도 테스트 DB |

### 16.3 이 문서를 이해했는지 확인하는 질문

아래 질문에 답을 찾아보세요. 모르면 오른쪽 파일부터 다시 보면 됩니다.

| 질문 | 다시 볼 곳 |
|---|---|
| 회사 API가 왜 CompanyController가 아니라 MasterController에 있는가? | MasterCatalog, MasterController, 5.1 |
| 이름 수정인데 왜 expectedVersion이 필요한가? | MasterServiceImpl.update, MasterMapper.xml update, 5.10~5.11 |
| 사용자가 보낸 CREATED_BY를 왜 그대로 저장하지 않는가? | RequestUserService.require, normalize, 4.1 |
| 같은 저장 버튼이 화면마다 다른 권한을 가질 수 있는가? | PermissionMapper.buttonId, requireButton, 4.7 |
| ROLE이 여러 개면 자료 범위까지 하나로 크게 합쳐도 되는가? | Grant, requireData, 7.4 |
| 한국어 요청에서 왜 영어가 나올 수 있는가? | language, get, 8.3~8.4 |
| DB가 고장 났는데 오류 응답은 어떻게 만드는가? | MessageServiceImpl.fallback, ApiErrorWriter.response, 8.6 |
| dev 파일을 추가했는데 왜 INI를 안 읽을 수 있는가? | dev 프로필과 import, 11.1~11.2 |
| 메뉴 목록 API를 호출하면 관리 계정이 생기는가? | ReferenceDataServiceImpl.bootstrap, 10.1 |
| 지금 로그인할 수 있는가, 후속 구현이 필요한가? | CurrentUserProvider, unauthenticatedUserProvider, 1.2 |

파일 이름을 모두 외울 필요는 없습니다. 실제 요청 하나를 정하고 “입구 → 사용자 확인 → 권한 검사 → 처리 담당 → SQL → 결과 또는 오류” 순서로 따라가면 각 파일이 왜 필요한지 이해할 수 있습니다.

<a id="chapter-17"></a>
## 17. 재고 단위·통화·금액 표시와 basis 구현

### 17.1 무엇이 새로 추가되었고, 어디까지 만들어졌는가

이 장은 현재 원본에 반영된 단위·통화·표시 기준과 `common/basis` 구현을 설명합니다.
SQL 정의와 Java 소스는 있으며 실제 Oracle 실행·화면 연동·Level 2 인증 완료와는 구분합니다.

마트에서 감자를 산다고 생각해 봅시다.

```text
감자 수량: 2 KG
감자 금액: 10,000 KRW
화면에 금액을 천원 단위로 표시: 10.0 (단위: KRW 천)
```

KG는 감자의 무게를 재는 **수량 단위**입니다. KRW는 돈의 종류인 **통화**입니다.
천원은 화면에서 숫자를 줄여 보여 주는 **금액 표시 단위**입니다.
이 세 가지를 모두 UNIT이라고 부르면 무엇을 곱하고 나누어야 하는지 헷갈립니다.

| 담당할 정보 | 테이블 | 기존 자료와의 연결 |
|---|---|---|
| 수량 단위와 공통 환산 기준 | `TB_UNIT` | 단위 종류는 공통코드, 표시명은 메시지 |
| 지원 통화와 기본 소수 자릿수 | `TB_CURRENCY` | 표시명은 메시지 |
| 메뉴·금액 항목의 표시 방법 | `TB_MENU_AMOUNT_FORMAT` | 기존 `TB_MENU`, 통화, 메시지 |

기존 테이블 18개를 없애거나 같은 목적의 표를 다시 만들지 않습니다.
새 세 테이블을 추가하는 이유는 공통코드 목록만으로는 수량 환산·통화 정밀도·메뉴별 숫자 표시를
명확한 자료형과 관계로 검사하기 어렵기 때문입니다.

### 17.2 UNIT: 단위 목록에 계산에 필요한 규칙을 함께 둔다

선택 상자에서 EA·KG만 고르게 한다면 공통코드 그룹 `UNIT`으로도 가능합니다.
하지만 재고에서는 `1.25 KG`를 입고한 뒤 `250 G`를 출고할 수 있습니다.
단위를 바꾸어 같은 기준으로 계산해야 하므로, 전용 `TB_UNIT`을 기준정보 원본으로 사용합니다.

| 컬럼 | 어린 학생에게 설명하면 | 예 |
|---|---|---|
| `UNIT_CODE` | 단위를 찾는 이름표 | `KG` |
| `UNIT_NAME_MSG_CODE` | 단위 이름의 번역을 찾는 이름표 | `COD_UNIT_KG` |
| `UNIT_TYPE_GROUP` | 공통코드 연결을 UNIT_TYPE 분류로 고정하는 보조 칸 | DB 기본값 UNIT_TYPE, API 편집 제외 |
| `UNIT_TYPE_CODE` | 개수를 세는가, 무게를 재는가, 용량을 재는가 | 무게 분류 |
| `QUANTITY_SCALE` | 수량에서 소수 몇 자리까지 기본으로 허용하는가 | 업무 기준으로 3 |
| `BASE_UNIT_CODE` | 같은 종류의 수량을 맞출 기준 단위 | `G` |
| `TO_BASE_FACTOR` | 기준 단위로 바꾸려고 곱할 숫자 | `1000` |
| `SORT_ORDER` | 단위 선택 목록에서 나타날 순서 | `10` |

단위 종류는 기존 `TB_COMMON_CODE`의 `UNIT_TYPE` 그룹에서 관리할 수 있습니다.
단위 자체는 `TB_UNIT`에만 두어 같은 목록을 두 군데에서 수정하는 일을 피합니다.
환산계수는 문자열 EXT_VALUE가 아닌 소수 계산에 맞는 숫자로 저장합니다.

실제 예 1: 밀가루의 KG와 G를 맞추기

```text
G 행:  BASE_UNIT_CODE=G, TO_BASE_FACTOR=1
KG 행: BASE_UNIT_CODE=G, TO_BASE_FACTOR=1000

입고: 1.25 KG = 1.25 × 1000 = 1250 G
출고: 250 G
같은 기준으로 계산한 잔량: 1250 - 250 = 1000 G = 1 KG
```

위 입고·출고 계산은 이해를 돕는 업무 예제입니다. 실제 재고 수불 API를 이번에 구현한 것은 아닙니다.
Level 1은 공통 단위 기준을 제공하고 재고 업무가 이를 사용해 수불을 처리합니다.

실제 예 2: EA와 PCS

볼펜 3 EA와 나사 3 PCS는 각각 개수를 셀 때 사용하는 표현일 수 있습니다.
하지만 코드가 비슷하다고 서버가 자동으로 같은 단위라고 판단하면 안 됩니다.
우리 업무에서 같은 의미로 쓰기로 정의한 경우에만 동일하게 취급합니다.
무게인 KG를 개수인 EA로 바꾸려면 개당 무게 같은 품목 정보가 필요합니다.

수량의 소수 자릿수도 업무 기준입니다. 일반적인 개수 품목은 0자리,
무게 품목은 소수 3자리처럼 정할 수 있지만 모든 품목에 무조건 적용할 국제 공통값은 아닙니다.
후속 품목 설정에서 더 구체적인 정밀도를 정할 수 있습니다.

### 17.3 공통 단위 환산과 품목별 포장 환산은 다르다

```text
볼펜 A: 1 BOX = 12 EA
음료 B: 1 BOX = 24 EA
```

TB_UNIT에 BOX의 계수를 12로 고정하면 음료 계산이 틀립니다.
BOX라는 단위 목록은 공통 기반에 둘 수 있지만, BOX 안에 몇 개가 들어 있는지는
후속 재고 업무의 **품목별 단위 관계**에 둬야 합니다.

공통 환산에서는 같은 종류의 기준 단위와 양수 계수를 검증합니다.
기준 단위 자신의 계수는 1이며 자기 참조를 허용합니다. 그 외 기준을 따라갔을 때 모순이나 순환이 없어야 합니다.
환산이 없는 단위는 기준 단위와 계수를 함께 비워 두는 정책을 사용할 수 있습니다.
현재 `MasterServiceImpl.validateUnit()`은 등록된 단위의 종류·기준 단위·환산계수 변경을 막습니다.
과거 수량의 의미가 바뀌지 않게 하는 정책입니다. 의미가 달라지면 새 코드나 별도 이력 설계가 필요합니다.

### 17.4 MB·GB와 MiB·GiB를 구별한다

표준에서는 다음처럼 구별합니다.

```text
1 GB  = 1000 MB
1 GiB = 1024 MiB
```

따라서 MB·GB를 등록하면서 1024로 계산한다고 암묵적으로 가정하면 안 됩니다.
1000 기준과 1024 기준이 모두 필요하면 각각의 단위 코드를 따로 관리합니다.
[NIST의 이진 배수 단위 설명](https://pml.nist.gov/cuu/Units/binary.html)

### 17.5 CURRENCY: 돈의 종류를 보관하는 기준정보

`TB_CURRENCY`에는 시스템이 지원하는 통화를 저장합니다.
통화 코드는 ISO 4217을 기준으로 준비하고 운영 중 사용할 목록을 관리합니다.
공식 유지관리 기관인 SIX에서 기준자료를 제공합니다.
[SIX 통화 코드 안내](https://www.six-group.com/en/products-services/financial-information/market-reference-data/data-standards.html)

| 컬럼 | 역할 | 예 |
|---|---|---|
| `CURRENCY_CODE` | 통화 이름표, 기본키 | `KRW` |
| `CURRENCY_NAME_MSG_CODE` | 번역할 이름의 메시지 코드 | `COD_CURRENCY_KRW` |
| `MINOR_UNIT_DIGITS` | 통화 자체의 기본 소수 자릿수 | KRW=0, USD=2, EUR=2 |
| `NUMERIC_CODE` | 선택적 ISO 숫자 코드 | `410`, `840`, `978` |
| `SORT_ORDER` | 목록 표시 순서 | `10` |

NUMERIC_CODE는 3자리 문자열로 관리해 선행 0을 보존합니다.
기호를 추가하더라도 `$`만 보고 미국 달러라고 판단하면 안 됩니다. 통화 코드를 식별자로 사용합니다.
기본 소수 자릿수가 정의되지 않은 특수 코드를 사용할 필요가 생기면 처리 정책을 먼저 정합니다.

일반 통화 예:

| 통화 코드 | 이름 예 | 기본 소수 자릿수 | 일반적인 금액 표기 예 |
|---|---|---:|---|
| KRW | 대한민국 원 | 0 | 1,250원 |
| USD | 미국 달러 | 2 | 12.50달러 |
| EUR | 유로 | 2 | 12.50유로 |

기본 소수 자릿수는 OpenJDK 통화 자료에서도 확인할 수 있습니다.
[OpenJDK 통화 기준자료](https://raw.githubusercontent.com/openjdk/jdk21u/master/src/java.base/share/data/currency/CurrencyData.properties)

환율은 이 테이블의 통화 목록과 별개입니다. `100 USD`를 KRW로 바꾸려면
어느 날짜의 어느 환율을 적용할지 정해야 합니다. 통화 코드를 KRW로 바꿔 적는 것으로 환전이 되지 않습니다.
환율을 적용한 원가·회계 계산은 해당 업무 구현 시 별도로 설계합니다.

### 17.6 테이블과 record를 함께 사용하는 이유

DB 테이블은 자료를 오래 보관하는 창고이고, record는 창고에서 가져온 자료를 담는 상자입니다.
둘 중 하나를 고르는 것이 아닙니다.

아래 Java 코드는 현재 `common/basis/BasisModels.java` 안의 CurrencyInfo record입니다. 독립된 CurrencyInfo.java 파일은 만들지 않았습니다.

```java
public record CurrencyInfo(
        String currencyCode,
        String name,
        int minorUnitDigits,
        String numericCode
) {}
```

```text
TB_CURRENCY에 KRW, 이름 메시지 코드, 기본 자릿수 0 저장
  ↓ Mapper가 조회
  ↓ MessageService가 한국어 이름을 찾음
CurrencyInfo("KRW", "대한민국 원", 0, "410")으로 전달
```

`currencyCode()`를 호출하면 KRW, `name()`을 호출하면 대한민국 원,
`minorUnitDigits()`를 호출하면 0을 꺼낼 수 있습니다.
이 꺼내기 메서드들은 record 선언으로 Java가 자동으로 만들어 줍니다.
[Oracle record 설명](https://docs.oracle.com/en/java/javase/21/language/records.html)

record를 선언해도 CREATE TABLE이나 INSERT는 실행되지 않습니다.
통화가 추가될 때마다 Java 코드의 고정 목록을 수정하는 방식 대신 DB의 기준정보를 관리합니다.
단위 조회 결과나 금액 표시 정책 전달에도 같은 방식으로 record를 사용할 수 있습니다.

### 17.7 금액 표시: 실제 돈과 화면에 적는 숫자를 나눈다

총예정원가가 59,230,000,000원이라고 해 봅시다. 숫자가 길어서 화면에서는 억원 단위로 줄여 보여 줍니다.

```text
실제 금액: 59,230,000,000 KRW
표시할 때 나눌 수: 100,000,000
표시 소수 자릿수: 1

59,230,000,000 ÷ 100,000,000 = 592.3

화면: 총예정원가 592.3 (단위: KRW 억)
뜻: 592억 3천만원
```

DB에는 실제 금액과 통화를 보존합니다. 화면에 보이는 592.3으로 원래 금액을 바꿔 저장하지 않습니다.
같은 금액의 표시 설정을 바꾸면 아래와 같이 보일 수 있습니다.

| 표시 단위 | 나눌 수 | 표시 소수 자릿수 | 보이는 숫자 |
|---|---:|---:|---:|
| 원 | 1 | 0 | 59,230,000,000 |
| 천원 | 1,000 | 1 | 59,230,000.0 |
| 억원 | 100,000,000 | 1 | 592.3 |
| 억원 | 100,000,000 | 2 | 592.30 |

`KRW 억`은 통화 KRW와 표시 배율 억을 조합한 문구입니다.
이를 하나의 문자열로 저장하고 글자를 해석해서 계산하지 않습니다.

### 17.8 메뉴와 금액 항목별 설정이 필요한 이유

한 화면에 총예정원가와 개당 단가가 함께 있을 수 있습니다.
총예정원가는 억 단위로, 개당 단가는 원 단위로 보여 주면 편리합니다.
메뉴 전체에 소수 자릿수 하나만 붙이면 이런 차이를 표현하기 어렵습니다.

따라서 기존 `TB_MENU`를 참조하는 `TB_MENU_AMOUNT_FORMAT`을 추가합니다.

| 컬럼 | 쉬운 설명 | 총예정원가 예 |
|---|---|---|
| `MENU_ID` | 어느 메뉴인가 | 실제 총예정원가 메뉴 번호 |
| `FIELD_CODE` | 어느 금액 항목인가 | `TOTAL_ESTIMATED_COST` |
| `CURRENCY_CODE` | 고정 통화 화면이라면 어느 통화인가 | `KRW` |
| `DISPLAY_DIVISOR` | 몇으로 나눠 보여 줄 것인가 | `100000000` |
| `DECIMAL_PLACES` | 소수 몇 자리까지 보여 줄 것인가 | `1` |
| `ROUNDING_MODE` | 끝자리를 어떻게 처리할 것인가 | `HALF_UP` |
| `UNIT_LABEL_MSG_CODE` | 억·백만 같은 문구의 번역 이름표 | 관련 메시지 코드 |

`MENU_ID + FIELD_CODE`는 유일해야 합니다. FIELD_CODE의 예약값 `DEFAULT`는 메뉴 기본 설정을 뜻합니다.
실제 금액 항목 이름으로 DEFAULT를 쓰지 않습니다.

```text
1. 해당 금액 항목의 설정이 있으면 사용
2. 없으면 해당 메뉴의 DEFAULT 설정 사용
3. 그것도 없으면 시스템 기본 설정 사용
```

예를 들어 메뉴 DEFAULT를 억원·소수 1자리로 정하고,
UNIT_PRICE만 원·소수 0자리로 따로 정하면 총예정원가와 개당 단가를 다르게 표시할 수 있습니다.

현재 `BasisServiceImpl.displayAmount()`의 시스템 기본값은 실제 통화 유지, 배율 1,
통화의 기본 소수 자릿수, HALF_UP입니다.
CURRENCY_CODE가 비어 있는 정책에서는 실제 금액의 통화를 사용합니다.
KRW로 고정한 정책에서는 실제 자료가 KRW인지 확인해야 합니다.

‘총예정원가-한글’이라는 이름을 위한 별도 한국어 테이블은 만들지 않습니다.
기존 메뉴의 번역 이름과 단위 문구를 `TB_MESSAGE` / `TB_MESSAGE_LANG`으로 처리합니다.
언어를 영어로 바꾸어도 실제 통화는 자동으로 USD가 되지 않습니다.

### 17.9 소수 자릿수 세 가지와 반올림을 구별한다

| 구분 | 무엇을 정하는가 | 예 |
|---|---|---|
| 통화 기본 소수 자릿수 | 통화 자체의 기본 정밀도 | KRW는 0, USD는 2 |
| 화면 표시 소수 자릿수 | 화면에 몇 자리까지 보여 주는가 | 억원 화면은 1 |
| 내부 계산 정밀도 | 계산 중 얼마나 정밀한 값을 유지하는가 | 단가 계산에서는 더 많은 소수 자리 필요 가능 |

KRW의 기본 자릿수가 0이어도 억 단위의 화면에서 592.3을 표시할 수 있습니다.
반대로 화면에 1자리만 보여 준다고 내부 계산과 저장도 무조건 1자리로 줄이면 안 됩니다.

반올림 예:

```text
원래 금액: 59,234,567,890 KRW
억 단위로 나눈 값: 592.3456789
소수 1자리 HALF_UP 표시: 592.3

원래 금액은 계속 59,234,567,890 KRW로 보존
```

금액·소수 수량·환산계수는 Java BigDecimal로 계산하고 정해진 최종 단계에서 반올림합니다.
DB의 NUMBER(p,s)는 최대 금액과 필요한 계산 정밀도에 맞춰 결정합니다.
통화 기본 자릿수만 보고 모든 단가나 중간 계산값의 소수를 잘라 저장하지 않습니다.
[Oracle BigDecimal 설명](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html)

화면·보고서·Excel은 같은 표시 정책을 사용해야 합니다.
편집할 때는 입력 단위를 명확히 알려야 하며, 반올림된 조회값을 그대로 저장해 원본 정밀도를 잃지 않게 합니다.
다른 통화끼리의 합산에는 별도 환율 정책이 필요합니다.

### 17.10 기존 Java 파일과 현재 연결된 기능

| 파일·메서드 | 현재 역할 |
|---|---|
| MasterCatalog.java의 RESOURCES·Kind·Column | 21개 기준정보와 DECIMAL·scale 정의 |
| MasterServiceImpl.java의 normalize()·convert()·validate() | 허용 입력·소수 정밀도·관계 검증 |
| MasterServiceImpl.java의 validateUnit()·validateCurrency()·validateAmountFormat() | 단위·통화·메뉴 표시 설정 검증 |
| MasterServiceImpl.java의 lockBasis()·validateBasisDeactivation() | 공통 변경 잠금과 활성 참조 확인 |
| MasterServiceImpl.java의 create()·update()·deactivate() | 등록·변경·비활성화와 감사·VERSION_NO |
| MasterMapper.java / XML의 activeUnitDependents()·activeCurrencyPolicies()·activeUnitTypeUsers() | 사용 중인 기준정보의 비활성화 검사 |
| MessageServiceImpl.java의 text() | 단위명·통화명·표시 배율의 번역 |
| ReferenceDataServiceImpl.java의 bootstrap() | 기존 화면 준비 응답 유지 |
| RequestUserService.java의 require(), CommonWebConfiguration의 preHandle() | 현재 사용자와 공통 관리 권한 확인 |
| BasisController.java / BasisServiceImpl.java | 별도 기준정보 조회와 환산·금액 표시 |

Controller는 Service를 통해 처리하고 Mapper를 직접 호출하지 않습니다.
새 기준정보 관리도 기존 MasterController 창구를 사용합니다.
수량 환산과 화면 표시의 자세한 메서드는 17.12에서 따라가 보세요.

Java 파일을 폴더로 역할별 분리하지 않습니다. 기존 common/master·message·permission·reference·web처럼
basis도 한 폴더에 모두 두며, 각 클래스의 역할과 호출 순서는 유지합니다.

### 17.11 DB 변경·관리 권한·인증과의 경계

세 테이블 모두 공통 다섯 칸과 VERSION_NO를 포함합니다.
등록자·수정자에는 현재 인증 사용자 번호를 기록하고 Seed의 시스템 생성자만 0을 사용합니다.
소수 자릿수는 0 이상 정수이며 허용 상한을 정합니다. 배율·환산계수는 양수인지 검사합니다.
반올림 방식과 메뉴·통화·메시지 참조도 Backend에서 검사합니다.

현재 공통 예제는 통합 V002의 테이블별 구역에 있습니다.
고객별 실제 화면 설정은 별도 고객 Migration 또는 권한 있는 관리자 등록으로 분리합니다.
평면 basis 검토안은 격리 H2 환경에서 39개 테스트를 통과했습니다.
이번 문서 수정에서는 원본 애플리케이션이나 실제 Oracle을 실행하지 않았습니다.

변경 창구는 기존 COMMON_ADMIN과 기능 버튼 권한으로 보호합니다.
인증을 붙이는 Level 2가 새 단위·통화 테이블을 따로 만들 필요는 없습니다.
Level 2가 검증된 USER_ID를 제공하면 Level 1의 기준정보와 권한 서비스를 그대로 사용합니다.

장기 캐시를 도입한다면 기준정보 변경 커밋 후 관련 캐시를 무효화해야 합니다.
통화·표시 설정 전체를 로그인 토큰이나 세션에 고정하면 관리자가 수정한 설정이 오래된 값으로 남을 수 있습니다.
인증 정보와 공통 기준정보의 갱신 정책을 분리합니다.

품목별 포장 환산·재고 수불·환율을 적용한 업무 금액 계산은 해당 업무의 후속 구현 대상입니다.
관련 인증 연결은 [ErpComLevel2.md](F:/java/workspace/erpcom/docs/ErpComLevel2.md)에 설명합니다.


### 17.12 basis를 한 폴더에서 읽고 실행 흐름 따라가기

```text
common/
  master/       공통 등록·수정·비활성화
  message/      번역 메시지
  permission/   메뉴·버튼·자료 범위 권한
  reference/    기존 메뉴·버튼·공통코드 조회
  web/          사용자 연결·권한 인터셉터·웹 설정
  error/        공통 오류
  basis/
    BasisController.java
    BasisService.java
    BasisServiceImpl.java
    BasisMapper.java
    BasisModels.java
```

폴더는 ‘무슨 기능인가’를 구분하고, 파일 이름은 ‘그 기능에서 어떤 일을 맡는가’를 구분합니다.
한 폴더에 둔다고 Controller가 직접 DB를 읽거나 모든 처리를 한 클래스에 합치는 것은 아닙니다.

#### 17.12.1 통화 목록 요청을 따라가기

인증된 사용자가 GET `/api/common/currencies?lang=ko`를 요청한다고 가정해 봅시다.
실제 인증 연결이 없는 기본 상태에서는 먼저 401이므로 아래는 신분 확인을 통과한 뒤의 흐름입니다.

1. `BasisController.java`의 currencies()가 요청을 받습니다.
2. `RequestUserService.require()`로 실제 사용자 번호를 확인하고 MessageService.language()로 언어를 정합니다.
3. `BasisService.currencies()` 약속으로 일을 전달합니다. 실제 실행 담당자는 BasisServiceImpl입니다.
4. `BasisServiceImpl.currencies()`가 활성 사용자를 확인하고 BasisMapper.currencies()를 부릅니다.
5. `BasisMapper.xml`의 currencies SQL이 활성 TB_CURRENCY 행을 읽어 CurrencyRow에 담습니다.
6. MessageService.text()로 표시 이름을 번역하고 CurrencyInfo로 반환합니다.

이때 KRW의 예시 객체는 CurrencyInfo("KRW", "대한민국 원", 0, "410")입니다.
`currencyCode()`는 KRW, `name()`은 대한민국 원, `minorUnitDigits()`는 0, `numericCode()`는 410을 반환합니다.
수량 단위 목록은 BasisController.units() → BasisServiceImpl.units() → BasisMapper.units() 순서로 비슷하게 처리합니다.

#### 17.12.2 1.25 KG를 G로 바꾸기

`BasisServiceImpl.convertQuantity()`는 현재 사용자, 단위의 사용 상태, 입력 수량 정밀도를 확인합니다.
KG와 G의 종류가 MASS이고 기준 단위가 G로 같으면 다음 계산을 수행합니다.

```text
1.25 × KG 계수 1000 ÷ G 계수 1 = 1250 G
```

대상 단위의 소수 자릿수로 정확하게 나타낼 수 없으면 자동 반올림하지 않고 거절합니다.
EA와 PCS는 각자 기준 단위가 달라 자동 환산하지 않으며 BOX도 품목별 계수 없이는 EA로 바꾸지 않습니다.
이 메서드는 내부 Service에서 호출하는 계산입니다. 임의 재고를 조회·변경하는 공개 계산 API를 추가한 것은 아닙니다.

#### 17.12.3 총예정원가 592.3 표시를 따라가기

`BasisServiceImpl.displayAmount()`에 서버가 확인한 실제 금액 59,230,000,000 KRW,
메뉴 PM_TOTAL_ESTIMATED_COST, 항목 TOTAL_ESTIMATED_COST를 전달한다고 가정합니다.

1. readableMenu()가 메뉴 읽기 권한과 활성 메뉴를 확인합니다.
2. BasisMapper.currency()로 실제 통화를 확인하고 amountFormats()로 해당 메뉴의 활성 설정을 읽습니다.
3. 항목 설정 → DEFAULT → 시스템 기본 순서로 선택합니다.
4. 선택한 설정의 통화와 실제 통화가 다르면 ERR_CURRENCY_MISMATCH입니다.
5. BigDecimal로 100,000,000을 나누고 소수 1자리 HALF_UP를 적용합니다.
6. AmountDisplay에 원래 금액·실제 통화·표시 숫자·배율·자릿수·번역 문구를 담습니다.

```text
originalAmount = "59230000000"
currencyCode   = "KRW"
displayValue   = "592.3"
displayDivisor = "100000000"
decimalPlaces  = 1
unitLabel      = "억"
```

원래 금액과 배율은 문자열로 전달해 화면의 부동소수 변환으로 정밀도를 잃지 않게 합니다.
업무 원가 자료를 읽을 수 있는 회사·부서·소유자 범위 검사는 업무 Service가 먼저 해야 합니다.
메뉴 표시 설정을 읽는 일은 실제 원가 자료의 접근 권한을 대신하지 않습니다.
정책 조회 주소는 GET `/api/common/amount-formats/{menuCode}`이며, 기존 bootstrap 응답은 바꾸지 않았습니다.

#### 17.12.4 Java 주소와 XML 주소를 맞추기

Java 다섯 파일의 첫 선언은 모두 다음과 같습니다.

```java
package com.erpcom.erpcom.common.basis;
```

Mapper XML은 src/main/resources/mapper/common/BasisMapper.xml에 있습니다.

```xml
<mapper namespace="com.erpcom.erpcom.common.basis.BasisMapper">
  <!-- UnitRow는 BasisModels 안에 있으므로 $로 연결한 클래스 이름을 사용한다. -->
  <select id="units" resultType="com.erpcom.erpcom.common.basis.BasisModels$UnitRow">
    <!-- 실제 SQL은 BasisMapper.xml 참고 -->
  </select>
</mapper>
```

같은 원칙으로 CurrencyRow와 AmountFormatRow도 BasisModels$... 이름을 사용합니다.
패키지를 바꾸면 Java import·테스트 import·namespace·resultType을 함께 맞추고,
옛 역할별 폴더의 같은 클래스가 소스에 중복으로 남지 않게 합니다.
현재 원본에는 basis/controller·service·mapper·model 없이 평면 basis 파일 다섯 개가 있습니다.
