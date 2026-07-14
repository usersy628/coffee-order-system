# Implementation Plan

이 문서는 Coffee Order System 구현 작업의 상태 인덱스와 아직 완료되지 않은 작업의 준비·실행 상세를 관리한다. 완료된 작업의 상세와 실제 검증 결과는 [`IMPLEMENTATION_HISTORY.md`](IMPLEMENTATION_HISTORY.md)에서 보존한다.

## 문서 역할

- `README.md`: 요구사항, ERD, API 계약, 동시성·Outbox 전략과 선택 근거의 단일 기준
- `docs/IMPLEMENTATION_PLAN.md`: 전체 작업 상태 인덱스와 완료되지 않은 작업의 준비·실행 상세
- `docs/IMPLEMENTATION_HISTORY.md`: `DONE` 작업의 상세, issue·PR·merge commit·완료일과 실제 검증 결과
- `docs/PROJECT_STATUS.md`: 현재 작업과 다음 한 가지 행동을 전달하는 짧은 인수인계 문서
- `AGENTS.md`: 저장소 작업, Git, 보안과 문서 갱신 규칙

정책이 달라지면 `README.md`를 먼저 수정하고 승인받는다. 이 문서에는 정책 내용을 복사하지 않고 관련 섹션을 참조한다.

## 작업 상태

| 상태 | 의미 |
| --- | --- |
| `BACKLOG` | 순서는 정했지만 시작 조건이나 상세 경로가 아직 확정되지 않음 |
| `READY` | 선행 작업, 정확한 파일 경로, 사전 테스트 또는 검증, 완료 명령이 모두 확정됨 |
| `IN_PROGRESS` | 현재 수행 중이며 동시에 하나만 허용 |
| `BLOCKED` | 외부 결정이나 선행 작업이 없어 진행할 수 없음 |
| `DONE` | 완료 조건을 충족하고 정의된 검증을 통과했으며 상세를 History로 이동함 |

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
| [`DOC-01`](https://github.com/usersy628/coffee-order-system/issues/25) | `DONE` | PR #24 병합 | 활성 계획과 완료 이력 분리, 인수인계와 PR 본문 규칙 정합성 보완 | 문서 역할·링크·상태가 일치하고 PR 본문 UTF-8 검증 성공 |
| [`S5-01`](https://github.com/usersy628/coffee-order-system/issues/1) | `DONE` | 4단계 설계 완료 | 기술 스택·패키지 구조·설정 및 테스트 구성 추천안과 승인 | 선택 사항이 문서화되고 사용자가 승인함 |
| [`S5-02`](https://github.com/usersy628/coffee-order-system/issues/2) | `DONE` | `S5-01` | Spring Boot·빌드 도구 기본 구조, traceId·공통 오류 기반과 MySQL Testcontainers 환경 | 기본 컨텍스트·공통 예외 smoke test·MySQL smoke 테스트와 빌드 성공 |
| [`S5-03`](https://github.com/usersy628/coffee-order-system/issues/17) | `DONE` | `S5-02` | 리뷰 후속 공통 MVC 오류·Flyway 재실행 검증·PR CI 기반 보완 | 4xx 계약·migration 재실행·GitHub Actions 검증 성공 |
| [`S5-04`](https://github.com/usersy628/coffee-order-system/issues/20) | `DONE` | `S5-03` | PR 검토와 명시적 병합 승인 workflow 문서화 | PR·CI 후 검토 대기와 사용자 승인 전 병합 금지가 명시됨 |
| [`S6-01`](https://github.com/usersy628/coffee-order-system/issues/3) | `DONE` | `S5-03` | 메뉴 목록 조회 API와 테스트 | 메뉴 목록 계약·통합 테스트 성공 |
| [`S6-02`](https://github.com/usersy628/coffee-order-system/issues/22) | `DONE` | `S6-01`, `S5-04` | 메뉴 UTC 시간 매핑과 README 구현 상태 정합성 보완 | MySQL `DATETIME(6)`·`Instant` 정밀도 테스트와 문서 정합성 검증 성공 |
| [`S7-01`](https://github.com/usersy628/coffee-order-system/issues/4) | `DONE` | `S5-03` | 포인트 충전·이력·멱등성·동시성과 충전 요청 검증 오류 처리 | 실제 MySQL 단일·중복·경합 충전과 `INVALID_CHARGE_AMOUNT` 계약 테스트 성공 |
| [`S8-01`](https://github.com/usersy628/coffee-order-system/issues/5) | `DONE` | `S6-01`, `S7-01` | 여러 메뉴 주문·결제·멱등성, 트랜잭션 내 Outbox 저장과 주문 요청 검증 오류 처리 | 실제 MySQL 원자성·중복 요청·동시 주문과 `INVALID_ORDER_REQUEST` 계약 테스트 성공 |
| [`S9-01`](https://github.com/usersy628/coffee-order-system/issues/6) | `READY` | `S8-01` | Outbox 게시자와 Mock 데이터 수집 플랫폼 | 2xx 성공, 4xx 즉시 실패, 네트워크·timeout·5xx 최대 5회 재시도, lease·fencing·중복 제거 테스트 성공 |
| [`S10-01`](https://github.com/usersy628/coffee-order-system/issues/7) | `BACKLOG` | `S8-01` | 최근 168시간 인기 메뉴 TOP 3 조회 | 실제 MySQL 기간 경계·수량·동률 정렬 테스트 성공 |
| [`S11-01`](https://github.com/usersy628/coffee-order-system/issues/8) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01` | 기능 간 동시성·회귀, k6 부하 기준선과 인기 메뉴 `EXPLAIN ANALYZE` 검증 | p95·오류율·DB·락·Outbox 지표와 인덱스·확장 판단 근거 기록 |
| [`S12-01`](https://github.com/usersy628/coffee-order-system/issues/9) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01`, `S11-01` | 전역 예외 매핑·traceId·로그와 API 계약 정합성 최종 보강 | 검증·도메인·동시성·예상외 500 응답과 헤더 계약 전체 테스트 성공 |
| [`S13-01`](https://github.com/usersy628/coffee-order-system/issues/10) | `BACKLOG` | `S12-01` | README 실행 방법과 구현 근거 보강 | 새 환경에서 문서만으로 실행·테스트 가능 |
| [`S14-01`](https://github.com/usersy628/coffee-order-system/issues/11) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01`, `S11-01`, `S12-01` | 구현 중 수시 기록한 내용을 정리한 TIL 트러블슈팅 문서 | 문제·원인·해결·검증 근거가 기록됨 |
| [`S15-01`](https://github.com/usersy628/coffee-order-system/issues/12) | `BACKLOG` | `S13-01`, `S14-01` | 전체 테스트·보안정보·공개 저장소 제출 검증 | 깨끗한 clone 기준 빌드와 전체 테스트 성공 |

`S9-01`과 `S10-01`은 모두 `S8-01`만 직접 선행하므로 서로 독립적으로 진행할 수 있다. MySQL Testcontainers 기반은 `S5-02`에서 만들고 `S5-03`에서 migration 재실행 검증을 보강한 뒤 각 기능 단계에서 사용하며, `S11-01`에서는 도입이 아니라 기능 간 최종 회귀와 부하·실행계획을 검증한다. 공통 MVC 전송 오류는 `S5-03`, 충전과 주문의 `MethodArgumentNotValidException`은 각각 `S7-01`과 `S8-01`에서 기능별 오류 코드로 구현하고, `S12-01`에서는 전체 API 오류·traceId·로그 계약의 최종 회귀와 누락을 점검한다.

## 진행 중인 작업 상세

현재 `IN_PROGRESS` 작업은 없다. `S8-01`의 완료 상세와 실제 검증 결과는 [`IMPLEMENTATION_HISTORY.md`](IMPLEMENTATION_HISTORY.md)에 보존한다.

### [`S9-01`](https://github.com/usersy628/coffee-order-system/issues/6) Outbox 게시자와 Mock 데이터 수집 플랫폼 구현

- 상태: `READY`
- 목적: 주문 트랜잭션이 저장한 `PENDING` Outbox를 짧은 DB 선점 트랜잭션과 별도 HTTP 전송으로 처리하고, 과제용 Mock 소비자의 영속 중복 제거까지 실제 MySQL과 소켓 장애 테스트로 검증한다.
- 요구사항 근거:
  - [`README.md` 외부 데이터 플랫폼 정책](../README.md#외부-데이터-플랫폼)
  - [`README.md` Outbox 선점·lease·fencing·재시도](../README.md#outbox-상태-전이와-fencing)
  - [`README.md` Mock 데이터 플랫폼 수신 경계](../README.md#mock-데이터-플랫폼-수신-경계)
  - [`README.md` 테스트 전략](../README.md#테스트-전략)
- 선행 작업: `S8-01`이 PR #27로 `dev`에 병합되어 주문·포인트·이력·`PENDING` Outbox 저장이 완료됨
- 사용자 승인: 2026-07-15. Mock은 같은 애플리케이션의 `local`·`test` 전용 내부 HTTP 수신기로 두고, 게시자는 설정된 base URL의 `POST /internal/mock-data-platform/events`에 원본 payload와 `Idempotency-Key: eventId`를 전송한다. 수신기는 V3의 `event_id` 유니크·payload·`received_at`을 영속화하고 중복은 추가 저장 없이 `200 OK`를 반환한다. WireMock은 실제 HTTP 장애 재현에 사용한다.
- 작업 브랜치: `feature/issue-6-outbox-publisher`
- 대상 파일:
  - `README.md`
  - `build.gradle`
  - `src/main/resources/application.yml`
  - `src/main/resources/application-local.yml`
  - `src/test/resources/application-test.yml`
  - `src/main/resources/db/migration/V3__create_mock_data_platform_received_event.sql`
  - `src/main/java/com/usersy628/coffeeorder/outbox/application/OutboxPublisher.java`
  - `src/main/java/com/usersy628/coffeeorder/outbox/application/OutboxClaimTransactionExecutor.java`
  - `src/main/java/com/usersy628/coffeeorder/outbox/application/OutboxStateTransactionExecutor.java`
  - `src/main/java/com/usersy628/coffeeorder/outbox/application/OutboxRetryPolicy.java`
  - `src/main/java/com/usersy628/coffeeorder/outbox/application/OutboxPublisherProperties.java`
  - `src/main/java/com/usersy628/coffeeorder/outbox/application/DataPlatformClient.java`
  - `src/main/java/com/usersy628/coffeeorder/outbox/domain/ClaimedOutboxEvent.java`
  - `src/main/java/com/usersy628/coffeeorder/outbox/domain/DeliveryResult.java`
  - `src/main/java/com/usersy628/coffeeorder/outbox/infrastructure/OutboxHttpConfiguration.java`
  - `src/main/java/com/usersy628/coffeeorder/outbox/infrastructure/RestClientDataPlatformClient.java`
  - `src/main/java/com/usersy628/coffeeorder/outbox/infrastructure/OutboxPublisherScheduler.java`
  - `src/main/java/com/usersy628/coffeeorder/mockplatform/api/MockDataPlatformController.java`
  - `src/main/java/com/usersy628/coffeeorder/mockplatform/application/MockDataPlatformService.java`
  - `src/main/java/com/usersy628/coffeeorder/mockplatform/infrastructure/MockDataPlatformJdbcRepository.java`
  - `src/test/java/com/usersy628/coffeeorder/outbox/application/OutboxRetryPolicyTest.java`
  - `src/test/java/com/usersy628/coffeeorder/outbox/application/OutboxPublisherIntegrationTest.java`
  - `src/test/java/com/usersy628/coffeeorder/outbox/infrastructure/OutboxClaimIntegrationTest.java`
  - `src/test/java/com/usersy628/coffeeorder/outbox/infrastructure/RestClientDataPlatformClientWireMockTest.java`
  - `src/test/java/com/usersy628/coffeeorder/mockplatform/api/MockDataPlatformApiIntegrationTest.java`
  - `src/test/java/com/usersy628/coffeeorder/support/testcontainers/DatabaseSmokeTest.java`
  - `docs/IMPLEMENTATION_PLAN.md`
  - `docs/IMPLEMENTATION_HISTORY.md`
  - `docs/PROJECT_STATUS.md`
- 먼저 수행할 테스트 또는 검증:
  1. `test` 프로필의 실제 MySQL 통합 테스트에서 `POST /internal/mock-data-platform/events`가 아직 `404 ENDPOINT_NOT_FOUND`가 되는 RED를 확인한다.
  2. 순수 재시도 정책 테스트에서 실패 횟수별 1·2·4·8·16초 기준과 ±20% jitter 범위를 먼저 작성한다.
  3. 실제 MySQL에서 두 worker의 `FOR UPDATE SKIP LOCKED` claim ID 교집합이 없고, 만료 lease 회수와 이전 `claim_token`의 상태 갱신 0건을 검증하는 테스트를 작성한다.
  4. 가짜 `DataPlatformClient`로 2xx·4xx·5xx·timeout 상태 전이와 HTTP 호출이 DB 트랜잭션 밖에서 일어나는지 검증한다.
  5. WireMock으로 URI·원본 JSON·`Idempotency-Key`, 5xx·connection reset·timeout과 HTTP client 내부 재시도 없음(호출 1회)을 검증한다.
  6. Mock 수신의 첫 저장, 순차·동시 중복 수신의 `200 OK`와 수집 row 한 건을 실제 MySQL에서 검증한다.
- RED 확인:
  - `MockDataPlatformApiIntegrationTest.receivesAnEventAndReturnsOk`가 수신 endpoint 부재로 `404 ENDPOINT_NOT_FOUND`를 반환하는 상태를 먼저 확인한다.
- 구현 범위:
  - Apache HttpClient 5 기반 `RestClient` adapter와 WireMock 3.x 테스트 의존성 추가
  - Outbox가 소유하는 6회 시도·지수 백오프·±20% jitter와 HTTP 응답 분류
  - `FOR UPDATE SKIP LOCKED` claim, 30초 lease 회수, `claim_token` fencing과 DB UTC 상태 갱신
  - 게시 주기 1초, batch 10, 인스턴스별 외부 호출 동시성 10, 전체 HTTP deadline 5초를 외부 설정으로 분리
  - `local`·`test` 전용 Mock Controller, V3 수신 테이블의 `event_id` 유니크·payload·`received_at`과 중복 `200 OK`
  - test 프로필 scheduler 비활성화, local loopback base URL, 다른 환경의 명시 URL 검증
  - MySQL Testcontainers, MockMvc와 WireMock으로 Outbox 상태·HTTP·중복 소비를 분리 검증
- 제외 범위:
  - Kafka, Redis, message broker, Spring Retry, WebFlux, H2
  - 운영자 redrive, 보관·정리 배치, 감사 사유와 전용 관리 API
  - 실제 외부 데이터 플랫폼 계정·인증·배포, 별도 Mock 애플리케이션·컨테이너·포트
  - 주문·포인트 API와 S8 저장 로직의 정책 변경, 로컬 MySQL/서버 포트 변경
- 완료 조건:
  - 주문 트랜잭션 밖에서만 HTTP 호출하며 2xx는 `PUBLISHED`, 4xx는 즉시 `FAILED`, 네트워크·timeout·5xx는 6번째 시도까지 Outbox만 재시도한다.
  - 실제 MySQL에서 claim 중복이 없고 due 정렬·batch/worker 제한·lease 회수·모든 fencing 갱신 조건이 지켜진다.
  - `Idempotency-Key: eventId`, 원본 payload와 시도당 HTTP 한 번이 WireMock 실제 소켓 테스트에서 검증된다.
  - Mock 수신기는 `local`·`test`에서만 노출되고 `eventId`가 같은 이벤트를 여러 번 받아도 row 하나와 `200 OK`를 유지한다.
  - V3 migration, 전체 테스트, `bootJar`, `git diff --check`와 필수 CI가 성공한다.
  - PR은 별도 검토와 사용자의 명시적 승인 전까지 병합하지 않는다.
- 검증 명령:

```powershell
.\gradlew.bat test --tests "com.usersy628.coffeeorder.mockplatform.api.MockDataPlatformApiIntegrationTest"
.\gradlew.bat test --tests "com.usersy628.coffeeorder.outbox.*"
.\gradlew.bat clean test
.\gradlew.bat bootJar
git diff --check
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
