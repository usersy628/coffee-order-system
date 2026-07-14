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
| [`S10-01`](https://github.com/usersy628/coffee-order-system/issues/7) | `READY` | `S8-01` | 최근 168시간 인기 메뉴 TOP 3 조회 | 실제 MySQL 기간 경계·수량 합계·동률 정렬·빈 결과·UTC/KST 시간 경계 테스트 성공 |
| [`S11-01`](https://github.com/usersy628/coffee-order-system/issues/8) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01` | 기능 간 동시성·회귀, k6 부하 기준선과 인기 메뉴 `EXPLAIN ANALYZE` 검증 | p95·오류율·DB·락·Outbox 지표와 인덱스·확장 판단 근거 기록 |
| [`S12-01`](https://github.com/usersy628/coffee-order-system/issues/9) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01`, `S11-01` | 전역 예외 매핑·traceId·로그와 API 계약 정합성 최종 보강 | 검증·도메인·동시성·예상외 500 응답과 헤더 계약 전체 테스트 성공 |
| [`S13-01`](https://github.com/usersy628/coffee-order-system/issues/10) | `BACKLOG` | `S12-01` | README 실행 방법과 구현 근거 보강 | 새 환경에서 문서만으로 실행·테스트 가능 |
| [`S14-01`](https://github.com/usersy628/coffee-order-system/issues/11) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01`, `S11-01`, `S12-01` | 구현 중 수시 기록한 내용을 정리한 TIL 트러블슈팅 문서 | 문제·원인·해결·검증 근거가 기록됨 |
| [`S15-01`](https://github.com/usersy628/coffee-order-system/issues/12) | `BACKLOG` | `S13-01`, `S14-01` | 전체 테스트·보안정보·공개 저장소 제출 검증 | 깨끗한 clone 기준 빌드와 전체 테스트 성공 |

`S9-01`과 `S10-01`은 모두 `S8-01`만 직접 선행하므로 서로 독립적으로 진행할 수 있다. MySQL Testcontainers 기반은 `S5-02`에서 만들고 `S5-03`에서 migration 재실행 검증을 보강한 뒤 각 기능 단계에서 사용하며, `S11-01`에서는 도입이 아니라 기능 간 최종 회귀와 부하·실행계획을 검증한다. 공통 MVC 전송 오류는 `S5-03`, 충전과 주문의 `MethodArgumentNotValidException`은 각각 `S7-01`과 `S8-01`에서 기능별 오류 코드로 구현하고, `S12-01`에서는 전체 API 오류·traceId·로그 계약의 최종 회귀와 누락을 점검한다.

## 준비·진행 중인 작업 상세

### [`S10-01`](https://github.com/usersy628/coffee-order-system/issues/7) 최근 168시간 인기 메뉴 TOP 3 조회

- 상태: `READY`
- 목적: 현재 시각부터 정확히 168시간의 완료 주문을 MySQL 원본 데이터로 직접 집계하여 `GET /api/menus/popular`에서 최대 3개 인기 메뉴와 같은 조회 경계를 반환한다.
- 요구사항 근거:
  - [`README.md` 인기 메뉴 정책](../README.md#인기-메뉴)
  - [`README.md` 최근 168시간 인기 메뉴 TOP 3 조회 API](../README.md#최근-168시간-인기-메뉴-top-3-조회)
  - [`README.md` 시간 저장 기준](../README.md#시간-저장-기준)
  - [`README.md` 인덱스](../README.md#인덱스)
  - [`README.md` 테스트 전략](../README.md#테스트-전략)
- 선행 작업: `S8-01`이 PR #27로 `dev`에 병합되어 `orders`, `order_item`, UTC `paid_at`과 `quantity`가 준비됨. `S9-01`은 별도 선행 조건이 아니며 PR #28도 이미 병합됨.
- 대상 파일:
  - 추가: `src/main/java/com/usersy628/coffeeorder/popularity/api/{PopularMenuController,PopularMenuResponse}.java`
  - 추가: `src/main/java/com/usersy628/coffeeorder/popularity/application/{PopularMenuQueryService,PopularMenuQueryRepository,PopularMenu}.java`
  - 추가: `src/main/java/com/usersy628/coffeeorder/popularity/infrastructure/JdbcPopularMenuQueryRepository.java`
  - 추가: `src/test/java/com/usersy628/coffeeorder/popularity/api/PopularMenuApiIntegrationTest.java`
  - 갱신: `README.md`, `docs/IMPLEMENTATION_PLAN.md`, `docs/IMPLEMENTATION_HISTORY.md`, `docs/PROJECT_STATUS.md`
- 먼저 수행할 테스트 또는 검증:
  1. 고정 `Clock`을 주입한 `PopularMenuApiIntegrationTest`에서 아직 없는 `GET /api/menus/popular`에 `200 OK`와 계약 body를 기대하여 `404 ENDPOINT_NOT_FOUND` RED를 확인한다.
  2. 실제 MySQL fixture로 시작 포함·종료 제외·정확히 168시간·수량 합계·동률 `menuId` 오름차순·상위 3개·빈 결과와 현재 메뉴명, `DECIMAL` 합계의 `long` 변환을 검증한다.
  3. 응답 `from`·`to`가 한 번 얻은 나노초 포함 `Clock` 값을 마이크로초로 절삭한 같은 `[T - 168시간, T)` 경계를 `Asia/Seoul` `+09:00`으로 표현하는지 검증한다.
- 구현 범위:
  - `Clock`에서 `T`를 한 번 얻어 마이크로초로 절삭하고 `from = T - 168시간`, `to = T`를 계산한다.
  - `orders.paid_at` UTC 범위와 `order_item.quantity`를 `JdbcTemplate` 직접 SQL로 집계한다. 현재 모든 주문은 `PAID`이므로 추가 상태 조건은 두지 않는다.
  - 메뉴별 `SUM(quantity)` 내림차순, `menu_id` 오름차순으로 정렬해 최대 3개를 조회하고, 최종 정렬 순서대로 `rank` 1부터 부여한다.
  - 현재 `menu.name`을 반환하며 `STOPPED` 메뉴를 별도로 제외하지 않는다. MySQL `SUM(INT)`의 `DECIMAL`은 범위를 확인해 `longValueExact()`로 변환한다.
  - 저장·조회는 UTC `Timestamp`를 사용하고 API 경계에서만 `Asia/Seoul` `OffsetDateTime`으로 변환한다.
- 제외 범위:
  - 새 migration·인덱스, 캐시·Redis·집계 테이블·스트리밍 집계, read replica
  - 일 단위 또는 한국 날짜 단위 집계, 주문·Outbox·메뉴 목록 정책 변경, `EXPLAIN ANALYZE` 성능 프로필과 k6 부하 테스트
- 완료 조건:
  - `GET /api/menus/popular`가 `200 OK`로 `from`, `to`, 최대 3개 `items[{rank, menuId, menuName, totalQuantity}]`를 반환하고 빈 결과는 빈 배열이다.
  - 실제 MySQL에서 `[T - 168시간, T)` 시작 포함·종료 제외와 수량 합계·동률 `menuId` 정렬·현재 메뉴명·`DECIMAL` 변환이 검증된다.
  - 고정 `Clock`의 마이크로초 절삭과 UTC 조회·KST API 경계가 일치한다.
  - 전체 테스트, `bootJar`, `git diff --check`와 필수 CI가 성공한다.
  - PR은 별도 검토와 사용자의 명시적 승인 전까지 병합하지 않는다.
- 검증 명령:

```powershell
.\gradlew.bat test --tests "com.usersy628.coffeeorder.popularity.api.PopularMenuApiIntegrationTest"
.\gradlew.bat test
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
