# Implementation Plan

이 문서는 Coffee Order System 구현 작업의 순서와 완료 기준을 관리한다. Spec Kit 전체 도구와 산출물 구조를 도입하지 않고, 작업 ID·선행 관계·독립 검증·정확한 파일 경로 원칙만 적용한다.

## 문서 역할

- `README.md`: 요구사항, ERD, API 계약, 동시성·Outbox 전략과 선택 근거의 단일 기준
- `docs/IMPLEMENTATION_PLAN.md`: 구현 작업의 순서, 의존성, 대상 파일과 검증 기준
- `docs/PROJECT_STATUS.md`: 현재 단계, 차단 사항과 다음 행동을 전달하는 짧은 인수인계 문서
- `AGENTS.md`: 저장소 작업, Git, 보안과 문서 갱신 규칙

정책이 달라지면 `README.md`를 먼저 수정하고 승인받는다. 이 문서에는 정책 내용을 복사하지 않고 관련 섹션을 참조한다.

## 작업 상태

| 상태 | 의미 |
| --- | --- |
| `BACKLOG` | 순서는 정했지만 시작 조건이나 상세 경로가 아직 확정되지 않음 |
| `READY` | 선행 작업, 정확한 파일 경로, 사전 테스트 또는 검증, 완료 명령이 모두 확정됨 |
| `IN_PROGRESS` | 현재 수행 중이며 동시에 하나만 허용 |
| `BLOCKED` | 외부 결정이나 선행 작업이 없어 진행할 수 없음 |
| `DONE` | 완료 조건을 충족하고 정의된 검증을 통과함 |

`READY`가 아닌 작업은 구현하지 않는다. 아직 존재하지 않는 소스 경로는 프로젝트 구조가 승인된 뒤 정확한 경로로 확정하고 `READY`로 변경한다.

## 작업 준비 기준

각 작업은 시작 전에 다음 내용을 모두 가져야 한다.

1. 연결되는 `README.md` 요구사항 또는 설계 섹션
2. 먼저 끝나야 하는 작업 ID
3. 생성하거나 수정할 정확한 저장소 상대 경로
4. 구현 전에 작성하거나 실행할 테스트·검증
5. 독립적으로 확인할 수 있는 완료 조건
6. 실행할 검증 명령
7. 구현 작업에 필요한 정책 또는 기술 선택의 사용자 승인

선택안 수립과 승인 획득 자체가 목적인 의사결정 작업은 승인 전에도 `READY`가 될 수 있으며, 승인을 완료 조건으로 둔다. 그 결과를 사용하는 구현 작업은 승인이 끝난 뒤에만 `READY`로 변경한다.

기능 작업은 가능한 경우 테스트를 먼저 작성하고 실패를 확인한 뒤 구현한다. 문서 작업은 링크, 형식, 모순 여부와 `git diff --check`를 사전 검증으로 사용한다.

## 전체 작업 목록

| ID | 상태 | 선행 작업 | 결과물 | 독립 완료 신호 |
| --- | --- | --- | --- | --- |
| [`S5-01`](https://github.com/usersy628/coffee-order-system/issues/1) | `DONE` | 4단계 설계 완료 | 기술 스택·패키지 구조·설정 및 테스트 구성 추천안과 승인 | 선택 사항이 문서화되고 사용자가 승인함 |
| [`S5-02`](https://github.com/usersy628/coffee-order-system/issues/2) | `READY` | `S5-01` | Spring Boot·빌드 도구 기본 구조, traceId·공통 오류 기반과 MySQL Testcontainers 환경 | 기본 컨텍스트·공통 예외 smoke test·MySQL smoke 테스트와 빌드 성공 |
| [`S6-01`](https://github.com/usersy628/coffee-order-system/issues/3) | `BACKLOG` | `S5-02` | 메뉴 목록 조회 API와 테스트 | 메뉴 목록 계약·통합 테스트 성공 |
| [`S7-01`](https://github.com/usersy628/coffee-order-system/issues/4) | `BACKLOG` | `S5-02` | 포인트 충전·이력·멱등성·동시성 처리 | 실제 MySQL 단일·중복·경합 충전 테스트 성공 |
| [`S8-01`](https://github.com/usersy628/coffee-order-system/issues/5) | `BACKLOG` | `S6-01`, `S7-01` | 여러 메뉴 주문·결제·멱등성과 트랜잭션 내 Outbox 저장 | 실제 MySQL 원자성·중복 요청·동시 주문 테스트 성공 |
| [`S9-01`](https://github.com/usersy628/coffee-order-system/issues/6) | `BACKLOG` | `S8-01` | Outbox 게시자와 Mock 데이터 수집 플랫폼 | 2xx 성공, 4xx 즉시 실패, 네트워크·timeout·5xx 최대 5회 재시도, lease·fencing·중복 제거 테스트 성공 |
| [`S10-01`](https://github.com/usersy628/coffee-order-system/issues/7) | `BACKLOG` | `S8-01` | 최근 168시간 인기 메뉴 TOP 3 조회 | 실제 MySQL 기간 경계·수량·동률 정렬 테스트 성공 |
| [`S11-01`](https://github.com/usersy628/coffee-order-system/issues/8) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01` | 기능 간 동시성·회귀, k6 부하 기준선과 인기 메뉴 `EXPLAIN ANALYZE` 검증 | p95·오류율·DB·락·Outbox 지표와 인덱스·확장 판단 근거 기록 |
| [`S12-01`](https://github.com/usersy628/coffee-order-system/issues/9) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01`, `S11-01` | 전역 예외 매핑·traceId·로그와 API 계약 정합성 최종 보강 | 검증·도메인·동시성·예상외 500 응답과 헤더 계약 전체 테스트 성공 |
| [`S13-01`](https://github.com/usersy628/coffee-order-system/issues/10) | `BACKLOG` | `S12-01` | README 실행 방법과 구현 근거 보강 | 새 환경에서 문서만으로 실행·테스트 가능 |
| [`S14-01`](https://github.com/usersy628/coffee-order-system/issues/11) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01`, `S11-01`, `S12-01` | 구현 중 수시 기록한 내용을 정리한 TIL 트러블슈팅 문서 | 문제·원인·해결·검증 근거가 기록됨 |
| [`S15-01`](https://github.com/usersy628/coffee-order-system/issues/12) | `BACKLOG` | `S13-01`, `S14-01` | 전체 테스트·보안정보·공개 저장소 제출 검증 | 깨끗한 clone 기준 빌드와 전체 테스트 성공 |

`S9-01`과 `S10-01`은 모두 `S8-01`만 직접 선행하므로 서로 독립적으로 진행할 수 있다. MySQL Testcontainers 기반은 `S5-02`에서 만들고 각 기능 단계에서 사용하며, `S11-01`에서는 도입이 아니라 기능 간 최종 회귀와 부하·실행계획을 검증한다. 공통 오류 응답과 전역 예외 처리의 최소 기반은 `S5-02`에서 만들고, `S12-01`에서 전체 API 계약을 최종 점검한다.

## 현재 READY 작업

### [`S5-02`](https://github.com/usersy628/coffee-order-system/issues/2) Spring Boot 기본 구조와 MySQL 통합 테스트 환경 구성

- 상태: `READY`
- 목적: 이후 기능이 공통으로 사용할 실행·DB migration·오류 응답·traceId·시간과 실제 MySQL 테스트 기반을 만든다.
- 요구사항 근거:
  - `README.md`의 `기술 스택과 프로젝트 구조`
  - `README.md`의 `예외 처리와 추적`, `테이블 설계`, `시간 저장 기준`, `테스트 전략`
- 선행 작업: `S5-01` 완료, GitHub issue #1
- 작업 브랜치: 최신 `dev`에서 `feature/issue-2-project-bootstrap` 생성
- 대상 파일:
  - `.gitattributes`
  - `.gitignore`
  - `.env.example`
  - `settings.gradle`
  - `build.gradle`
  - `gradlew`
  - `gradlew.bat`
  - `gradle/wrapper/gradle-wrapper.jar`
  - `gradle/wrapper/gradle-wrapper.properties`
  - `compose.yaml`
  - `src/main/java/com/usersy628/coffeeorder/CoffeeOrderApplication.java`
  - `src/main/java/com/usersy628/coffeeorder/global/time/TimeConfig.java`
  - `src/main/java/com/usersy628/coffeeorder/global/error/ApiErrorResponse.java`
  - `src/main/java/com/usersy628/coffeeorder/global/error/DomainException.java`
  - `src/main/java/com/usersy628/coffeeorder/global/error/ErrorCode.java`
  - `src/main/java/com/usersy628/coffeeorder/global/error/GlobalExceptionHandler.java`
  - `src/main/java/com/usersy628/coffeeorder/global/trace/TraceIdFilter.java`
  - `src/main/resources/application.yml`
  - `src/main/resources/application-local.yml`
  - `src/main/resources/db/migration/V1__create_schema.sql`
  - `src/main/resources/db/migration/V2__seed_assignment_data.sql`
  - `src/test/java/com/usersy628/coffeeorder/CoffeeOrderApplicationTests.java`
  - `src/test/java/com/usersy628/coffeeorder/global/error/GlobalExceptionHandlerTest.java`
  - `src/test/java/com/usersy628/coffeeorder/global/trace/TraceIdFilterTest.java`
  - `src/test/java/com/usersy628/coffeeorder/support/testcontainers/DatabaseSmokeTest.java`
  - `src/test/java/com/usersy628/coffeeorder/support/testcontainers/MySqlContainerConfiguration.java`
  - `src/test/java/com/usersy628/coffeeorder/support/testcontainers/MySqlIntegrationTest.java`
  - `src/test/resources/application-test.yml`
- 먼저 수행할 테스트 또는 검증:
  1. `java -version`, `docker version`, `docker compose version`으로 Java 17과 Docker 실행 조건을 확인한다.
  2. Spring Boot `3.5.16`, Gradle Wrapper `8.14.3`과 승인된 기본 의존성만 포함한 최소 빌드 구조를 만든다.
  3. `CoffeeOrderApplicationTests`의 context load 테스트를 먼저 실행한다.
  4. 성공·오류 응답의 `X-Trace-Id`, 오류 body의 `traceId`와 요청 종료 후 MDC 정리를 확인하는 실패 테스트를 작성한 뒤 오류·trace 기반을 구현한다.
  5. 빈 MySQL `8.4.10` 컨테이너의 Flyway 적용, Hibernate `validate`, seed 사용자별 0P 지갑과 session 락 대기 2초를 확인하는 실패 테스트를 작성한 뒤 설정과 migration을 구현한다.
- 구현 범위:
  - Java 17 toolchain, Spring Boot `3.5.16`, Gradle Wrapper `8.14.3` Groovy DSL
  - Spring Web·Validation·Data JPA·Actuator, MySQL Connector/J, Flyway와 MySQL Testcontainers
  - Flyway만 사용하는 schema 생성과 과제용 고정 초기 사용자·메뉴·0P 지갑
  - `Clock.systemUTC()` Bean과 UTC JDBC·Hibernate 설정
  - `code`, `message`, `details`, `traceId` 오류 body의 최소 구현
  - 서버 생성 traceId의 MDC·응답 헤더 연결과 요청 종료 시 정리
  - Hikari 최대 연결 10, 연결 획득 대기 2초, 검증 대기 1초와 MySQL session `innodb_lock_wait_timeout=2`
  - `@ServiceConnection`을 사용하는 MySQLContainer Spring Bean과 테스트 context당 공유
  - `@SpringBootTest`, test profile과 `MySqlContainerConfiguration` import를 묶은 `@MySqlIntegrationTest` meta-annotation을 context·DB smoke test에 적용
- 제외 범위:
  - 메뉴·포인트·주문·인기 메뉴 API와 Outbox 게시자 구현
  - Apache HttpClient 5와 WireMock 의존성 추가 및 외부 HTTP 호출
  - Redis, Kafka, H2, Spring Retry와 WebFlux 도입
  - 모든 API 예외의 최종 매핑과 로그 계약 완성은 `S12-01`에서 수행
- 완료 조건:
  - 애플리케이션 context가 Java 17과 Gradle Wrapper로 시작한다.
  - Flyway `V1`, `V2`가 빈 MySQL `8.4.10`에 적용되고 재실행 시 검증에 성공한다.
  - Hibernate가 schema를 생성하거나 변경하지 않고, 추가되는 JPA mapping에 `validate` 정책이 적용된다.
  - seed 사용자는 각각 정확히 하나의 0P 지갑을 가진다.
  - 테스트 연결의 `@@session.innodb_lock_wait_timeout`이 2다.
  - 공통 오류 body의 `traceId`와 `X-Trace-Id`가 일치하고 내부 예외 정보가 노출되지 않는다.
  - 성공 응답에도 서버가 생성한 `X-Trace-Id`가 포함되고 요청 종료 후 MDC가 정리된다.
  - Actuator health endpoint가 `UP`이며 불필요한 endpoint는 외부에 노출되지 않는다.
  - H2 없이 context·공통 오류·MySQL smoke test와 전체 테스트가 성공한다.
  - 저장소에 실제 비밀번호나 로컬 `.env`가 포함되지 않는다.
- 검증 명령:

```powershell
docker compose config
docker info
.\gradlew.bat --version
.\gradlew.bat test --tests "com.usersy628.coffeeorder.CoffeeOrderApplicationTests"
.\gradlew.bat test --tests "com.usersy628.coffeeorder.global.error.GlobalExceptionHandlerTest"
.\gradlew.bat test --tests "com.usersy628.coffeeorder.global.trace.TraceIdFilterTest"
.\gradlew.bat test --tests "com.usersy628.coffeeorder.support.testcontainers.DatabaseSmokeTest"
.\gradlew.bat clean test
.\gradlew.bat bootJar
git diff --check
git status --short
```

## 작업 상세 템플릿

다음 작업을 `READY`로 변경할 때 아래 형식을 복사해 구체화한다.

```markdown
### `[작업 ID]` 작업명

- 상태: `READY`
- 목적:
- 요구사항 근거:
- 선행 작업:
- 대상 파일:
  - `정확한/저장소/상대/경로`
- 먼저 수행할 테스트 또는 검증:
- 구현 범위:
- 제외 범위:
- 완료 조건:
- 검증 명령:
```

## 경량 적용 범위

- `.specify/`, `specs/`와 Spec Kit 명령 파일은 추가하지 않는다.
- 번호 기반 기능 브랜치를 자동 생성하지 않고 현재 `dev` 작업 흐름을 유지한다.
- README의 요구사항과 설계를 작업 문서에 중복 저장하지 않는다.
- 작업 목록 자동 생성 대신, 단계가 `READY`가 될 때 필요한 정보만 구체화한다.
- 프로젝트 규모나 협업 인원이 커져 추적성 이점이 유지 비용보다 커질 때 전면 도입을 다시 검토한다.
