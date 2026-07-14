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
| [`S9-01`](https://github.com/usersy628/coffee-order-system/issues/6) | `DONE` | `S8-01` | Outbox 게시자와 Mock 데이터 수집 플랫폼 | V3 수신 저장, claim·lease·fencing·고정 6회 재시도, 실제 HTTP 취소·종료 안전성과 중복 제거 검증 성공; PR #28 병합 완료 |
| [`S10-01`](https://github.com/usersy628/coffee-order-system/issues/7) | `DONE` | `S8-01` | 최근 168시간 인기 메뉴 TOP 3 조회 | 실제 MySQL 기간 경계·수량 합계·동률 정렬·빈 결과·UTC/KST 시간 경계 테스트 성공; PR #29 병합 완료 |
| [`S11-01`](https://github.com/usersy628/coffee-order-system/issues/8) | `IN_PROGRESS` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01` | 기능 간 동시성·회귀, k6 부하 기준선과 인기 메뉴 `EXPLAIN ANALYZE` 검증 | 실제 MySQL 교차 회귀, 30 RPS·5분 인기 메뉴 조회 기준선, p95·오류율·DB·락·Outbox 지표와 인덱스·확장 판단 근거 기록 |
| [`S12-01`](https://github.com/usersy628/coffee-order-system/issues/9) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01`, `S11-01` | 전역 예외 매핑·traceId·로그와 API 계약 정합성 최종 보강 | 검증·도메인·동시성·예상외 500 응답과 헤더 계약 전체 테스트 성공 |
| [`S13-01`](https://github.com/usersy628/coffee-order-system/issues/10) | `BACKLOG` | `S12-01` | README 실행 방법과 구현 근거 보강 | 새 환경에서 문서만으로 실행·테스트 가능 |
| [`S14-01`](https://github.com/usersy628/coffee-order-system/issues/11) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01`, `S11-01`, `S12-01` | 구현 중 수시 기록한 내용을 정리한 TIL 트러블슈팅 문서 | 문제·원인·해결·검증 근거가 기록됨 |
| [`S15-01`](https://github.com/usersy628/coffee-order-system/issues/12) | `BACKLOG` | `S13-01`, `S14-01` | 전체 테스트·보안정보·공개 저장소 제출 검증 | 깨끗한 clone 기준 빌드와 전체 테스트 성공 |

`S9-01`과 `S10-01`은 모두 `S8-01`만 직접 선행하므로 서로 독립적으로 진행할 수 있다. MySQL Testcontainers 기반은 `S5-02`에서 만들고 `S5-03`에서 migration 재실행 검증을 보강한 뒤 각 기능 단계에서 사용하며, `S11-01`에서는 도입이 아니라 기능 간 최종 회귀와 부하·실행계획을 검증한다. 공통 MVC 전송 오류는 `S5-03`, 충전과 주문의 `MethodArgumentNotValidException`은 각각 `S7-01`과 `S8-01`에서 기능별 오류 코드로 구현하고, `S12-01`에서는 전체 API 오류·traceId·로그 계약의 최종 회귀와 누락을 점검한다.

## 준비·진행 중인 작업 상세

현재 `IN_PROGRESS` 작업은 `S11-01` 하나다. `S9-01`과 `S10-01`의 완료 상세와 실제 검증 결과는 [`IMPLEMENTATION_HISTORY.md`](IMPLEMENTATION_HISTORY.md)에 보존한다.

### [`S11-01`](https://github.com/usersy628/coffee-order-system/issues/8) 기능 간 MySQL 회귀, 부하 기준선과 실행계획 검증

- 상태: `IN_PROGRESS`
- 목적: 이미 구현된 포인트 충전·주문·멱등성·Outbox·Mock 소비자·인기 메뉴를 실제 MySQL에서 하나의 흐름으로 회귀 검증하고, 인기 메뉴 조회의 단일 인스턴스 부하 기준선과 실행계획을 재현 가능하게 기록한다.
- 요구사항 근거:
  - [`README.md` 인덱스](../README.md#인덱스)
  - [`README.md` 부하 대응과 확장 기준](../README.md#부하-대응과-확장-기준)
  - [`README.md` 테스트 전략](../README.md#테스트-전략)
- 선행 작업: `S6-01`부터 `S10-01`까지 `DONE`; `S10-01`은 issue #7 `CLOSED`, PR #29 `MERGED`, merge commit `c2243a6`으로 확인했다.
- 사용자 승인·측정 정책: 2026-07-15.
  - 단일 애플리케이션 인스턴스 기준선만 측정한다. 사용자의 local 프로필·MySQL `3307`·서버 `18080` 정보는 실행 참고값이며, 기존 `3307` 개발 DB는 seed·초기화하지 않는다. 부하용 DB는 별도 Docker Compose 프로젝트와 기본 host port `3308`을 사용하고, 별도 `local,perf` 앱은 기본 `18081`로 실행해 일반 local `18080`을 유지한다.
  - Docker k6로 `GET /api/menus/popular`만 constant-arrival-rate `30 RPS`, `5분` 측정한다. 통과 기준은 `http_req_duration` p95 `500 ms` 이하, 시스템 오류율 `1%` 미만, 모든 응답 계약 check 성공이다.
  - 주문·포인트·Outbox 쓰기 경로를 k6에 섞지 않는다. 실제 MySQL 교차 회귀 테스트로 정합성을 확인한다. GET-only run에서는 Outbox 이벤트가 생기지 않으므로 `PENDING` 수와 queue age를 관찰값으로만 기록하며, `PENDING` 없음은 5초 기준 충족의 증거로 해석하지 않는다.
  - Testcontainers 성능 태그에서 최근 30일 `orders` 100,000건과 `order_item` 300,000건으로 인기 메뉴 SQL의 `EXPLAIN ANALYZE`를 실행한다. 절대 실행 시간이나 출력 문구는 CI 통과 조건으로 고정하지 않는다.
  - 측정 결과에는 commit·시각·OS/CPU/논리 코어/RAM·Docker/Java/MySQL 버전·애플리케이션/DB/부하 발생기 배치·데이터셋·시나리오·지연/오류/RPS·DB/락/Outbox 관찰값을 함께 남긴다. 비밀값은 기록하지 않는다.
- 작업 브랜치: `feature/issue-8-mysql-regression-load-baseline`
- 대상 파일:
  - `src/test/java/com/usersy628/coffeeorder/regression/CrossFeatureMySqlRegressionIntegrationTest.java`
  - `src/test/java/com/usersy628/coffeeorder/performance/PopularMenuExplainAnalyzeIntegrationTest.java`
  - `build.gradle`
  - `src/main/resources/application-perf.yml`
  - `src/main/java/com/usersy628/coffeeorder/global/config/PerfBaselineInfoContributor.java`
  - `src/main/java/com/usersy628/coffeeorder/global/config/PerfDataSourceConfigurationGuard.java`
  - `src/test/java/com/usersy628/coffeeorder/global/config/PerfBaselineInfoContributorTest.java`
  - `docker-compose.performance.yml`
  - `scripts/performance/{popular-menu.js,mysql-snapshot.sql,seed-popularity.sql,run-local-baseline.ps1}`
  - `docs/performance/S11_BASELINE.md`
  - `README.md`, `docs/IMPLEMENTATION_PLAN.md`, `docs/IMPLEMENTATION_HISTORY.md`, `docs/PROJECT_STATUS.md`
- 먼저 수행할 테스트 또는 검증:
  1. 기존 실제 MySQL 주문·포인트·Outbox·Mock·인기 메뉴 통합 테스트가 독립적으로 통과하는지 확인한다.
  2. 새 교차 회귀 테스트가 구현 전에는 존재하지 않아, 포인트부터 전달·집계까지 하나로 검증하는 보호망이 없음을 확인한다.
  3. native k6가 없는 환경에서는 설치하지 않고 Docker k6를 사용하며, 별도 `coffee_order_perf` DB 외에는 자동으로 초기화하거나 seed하지 않는지 점검한다.
- 구현 범위:
  - 동일 주문 키의 동시 재시도에서 포인트 차감·주문·Outbox·Mock 수신·인기 메뉴 집계가 각각 한 번만 반영되는 실제 MySQL 회귀 테스트와, 잔액 부족 주문이 ghost data를 남기지 않는 테스트를 추가한다.
  - `performance` JUnit 태그와 명시 실행용 `performanceTest` Gradle task를 추가해 일반 `test`에서 대량 fixture와 `EXPLAIN ANALYZE`를 분리한다.
  - 30일·주문 100,000건·항목 300,000건 fixture의 인기 메뉴 `EXPLAIN ANALYZE` 결과를 build report에 남기되, 실행 환경에 따라 달라지는 절대 시간은 assertion하지 않는다.
  - `local,perf` 전용 Actuator metrics, Flyway 이전의 설정 JDBC URL guard, 실제 JDBC 연결 identity 검증과 별도 MySQL Compose 구성을 추가한다. 비밀값은 process environment 또는 untracked `.env`에서만 전달하고 저장소에 기록하지 않는다.
  - 명시한 base URL로만 동작하는 인기 메뉴 GET k6 script, perf DB에만 동작하는 fixture script, 읽기 전용 MySQL 관찰 SQL, fixture 재생성·검증과 Hikari 주기 sample을 포함한 Windows 측정 환경·결과 수집 script와 결과 기록 양식을 추가한다.
- 제외 범위:
  - migration·인덱스·캐시·Redis·read replica·다중 인스턴스 증설 및 production metrics endpoint 추가
  - 주문·포인트 쓰기 부하와 기존 사용자 DB의 초기화·seed
  - S11과 무관한 PR lifecycle 문서 규칙의 전역 변경
- 완료 조건:
  - 교차 회귀가 포인트 충전 → 동시 멱등 주문 → `PENDING` Outbox → 중복 없는 Mock 수신 → 인기 메뉴 수량 집계를 실제 MySQL에서 확인하고, 거절 주문의 ghost data가 없음을 확인한다.
  - 일반 `test`는 성능 fixture를 실행하지 않고, 명시한 `performanceTest`가 100,000/300,000 fixture와 `EXPLAIN ANALYZE` report를 만든다.
  - k6·DB 관찰·환경 수집 명령은 비밀값과 기존 사용자 DB 변경 없이 재현 가능하며, 실제 datasource identity와 fixture count를 검증하고 결과 문서는 측정 환경과 판단 기준을 빠짐없이 기록할 수 있다. GET-only run의 Outbox 관찰 한계도 결과에 명시한다.
  - 인덱스·Redis·replica는 측정 전후를 비교할 수 있는 근거가 생기기 전까지 추가하지 않는다.
- 검증 명령:

```powershell
.\gradlew.bat test --tests "com.usersy628.coffeeorder.regression.CrossFeatureMySqlRegressionIntegrationTest"
.\gradlew.bat performanceTest --no-daemon
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\performance\run-local-baseline.ps1 -Seed -RunK6
.\gradlew.bat test --no-daemon --rerun-tasks
.\gradlew.bat bootJar --no-daemon
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
