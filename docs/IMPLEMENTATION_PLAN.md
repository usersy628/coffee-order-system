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
| [`S5-02`](https://github.com/usersy628/coffee-order-system/issues/2) | `DONE` | `S5-01` | Spring Boot·빌드 도구 기본 구조, traceId·공통 오류 기반과 MySQL Testcontainers 환경 | 기본 컨텍스트·공통 예외 smoke test·MySQL smoke 테스트와 빌드 성공 |
| [`S5-03`](https://github.com/usersy628/coffee-order-system/issues/17) | `DONE` | `S5-02` | 리뷰 후속 공통 MVC 오류·Flyway 재실행 검증·PR CI 기반 보완 | 4xx 계약·migration 재실행·GitHub Actions 검증 성공 |
| [`S5-04`](https://github.com/usersy628/coffee-order-system/issues/20) | `DONE` | `S5-03` | PR 검토와 명시적 병합 승인 workflow 문서화 | PR·CI 후 검토 대기와 사용자 승인 전 병합 금지가 명시됨 |
| [`S6-01`](https://github.com/usersy628/coffee-order-system/issues/3) | `DONE` | `S5-03` | 메뉴 목록 조회 API와 테스트 | 메뉴 목록 계약·통합 테스트 성공 |
| [`S6-02`](https://github.com/usersy628/coffee-order-system/issues/22) | `DONE` | `S6-01`, `S5-04` | 메뉴 UTC 시간 매핑과 README 구현 상태 정합성 보완 | MySQL `DATETIME(6)`·`Instant` 정밀도 테스트와 문서 정합성 검증 성공 |
| [`S7-01`](https://github.com/usersy628/coffee-order-system/issues/4) | `IN_PROGRESS` | `S5-03` | 포인트 충전·이력·멱등성·동시성과 충전 요청 검증 오류 처리 | 실제 MySQL 단일·중복·경합 충전과 `INVALID_CHARGE_AMOUNT` 계약 테스트 성공 |
| [`S8-01`](https://github.com/usersy628/coffee-order-system/issues/5) | `BACKLOG` | `S6-01`, `S7-01` | 여러 메뉴 주문·결제·멱등성, 트랜잭션 내 Outbox 저장과 주문 요청 검증 오류 처리 | 실제 MySQL 원자성·중복 요청·동시 주문과 `INVALID_ORDER_REQUEST` 계약 테스트 성공 |
| [`S9-01`](https://github.com/usersy628/coffee-order-system/issues/6) | `BACKLOG` | `S8-01` | Outbox 게시자와 Mock 데이터 수집 플랫폼 | 2xx 성공, 4xx 즉시 실패, 네트워크·timeout·5xx 최대 5회 재시도, lease·fencing·중복 제거 테스트 성공 |
| [`S10-01`](https://github.com/usersy628/coffee-order-system/issues/7) | `BACKLOG` | `S8-01` | 최근 168시간 인기 메뉴 TOP 3 조회 | 실제 MySQL 기간 경계·수량·동률 정렬 테스트 성공 |
| [`S11-01`](https://github.com/usersy628/coffee-order-system/issues/8) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01` | 기능 간 동시성·회귀, k6 부하 기준선과 인기 메뉴 `EXPLAIN ANALYZE` 검증 | p95·오류율·DB·락·Outbox 지표와 인덱스·확장 판단 근거 기록 |
| [`S12-01`](https://github.com/usersy628/coffee-order-system/issues/9) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01`, `S11-01` | 전역 예외 매핑·traceId·로그와 API 계약 정합성 최종 보강 | 검증·도메인·동시성·예상외 500 응답과 헤더 계약 전체 테스트 성공 |
| [`S13-01`](https://github.com/usersy628/coffee-order-system/issues/10) | `BACKLOG` | `S12-01` | README 실행 방법과 구현 근거 보강 | 새 환경에서 문서만으로 실행·테스트 가능 |
| [`S14-01`](https://github.com/usersy628/coffee-order-system/issues/11) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01`, `S11-01`, `S12-01` | 구현 중 수시 기록한 내용을 정리한 TIL 트러블슈팅 문서 | 문제·원인·해결·검증 근거가 기록됨 |
| [`S15-01`](https://github.com/usersy628/coffee-order-system/issues/12) | `BACKLOG` | `S13-01`, `S14-01` | 전체 테스트·보안정보·공개 저장소 제출 검증 | 깨끗한 clone 기준 빌드와 전체 테스트 성공 |

`S9-01`과 `S10-01`은 모두 `S8-01`만 직접 선행하므로 서로 독립적으로 진행할 수 있다. MySQL Testcontainers 기반은 `S5-02`에서 만들고 `S5-03`에서 migration 재실행 검증을 보강한 뒤 각 기능 단계에서 사용하며, `S11-01`에서는 도입이 아니라 기능 간 최종 회귀와 부하·실행계획을 검증한다. 공통 MVC 전송 오류는 `S5-03`, 충전과 주문의 `MethodArgumentNotValidException`은 각각 `S7-01`과 `S8-01`에서 기능별 오류 코드로 구현하고, `S12-01`에서는 전체 API 오류·traceId·로그 계약의 최종 회귀와 누락을 점검한다.

## 현재 작업 상세

### [`S7-01`](https://github.com/usersy628/coffee-order-system/issues/4) 포인트 충전·이력·멱등성·동시성 구현

- 상태: `IN_PROGRESS`
- 사용자 승인: 2026-07-14
- 목적: `POST /api/users/{userId}/points/charges`에서 충전 한도, 지갑과 이력의 원자성, 멱등 결과 재현과 MySQL 비관적 락 기반 동시성 제어를 구현한다.
- 요구사항 근거:
  - `README.md`의 `기술 스택과 프로젝트 구조 > 패키지와 계층 경계`
  - `README.md`의 `핵심 정책 > 포인트`
  - `README.md`의 `API 명세 > 공통 규칙`과 `포인트 충전`
  - `README.md`의 `동시성 및 트랜잭션 상세 전략 > 포인트 충전 흐름`과 `락 순서, 타임아웃과 재시도`
  - `README.md`의 `예외 처리와 추적`
  - `README.md`의 `테이블 설계 > point_wallet`과 `point_history`
  - `README.md`의 `테스트 전략`과 `주요 오류 정책`
- 선행 작업: `S5-03` 완료와 PR #18의 `dev` 병합
- 작업 브랜치: 최신 `dev`에서 `feature/issue-4-point-charge-api` 생성
- 대상 파일:
  - `src/main/java/com/usersy628/coffeeorder/point/api/PointChargeController.java`
  - `src/main/java/com/usersy628/coffeeorder/point/api/PointChargeRequest.java`
  - `src/main/java/com/usersy628/coffeeorder/point/api/PointChargeResponse.java`
  - `src/main/java/com/usersy628/coffeeorder/point/application/PointChargeCommand.java`
  - `src/main/java/com/usersy628/coffeeorder/point/application/PointChargeResult.java`
  - `src/main/java/com/usersy628/coffeeorder/point/application/PointChargeService.java`
  - `src/main/java/com/usersy628/coffeeorder/point/application/PointChargeTransactionExecutor.java`
  - `src/main/java/com/usersy628/coffeeorder/point/application/PointChargeReplayReader.java`
  - `src/main/java/com/usersy628/coffeeorder/point/application/PointChargeRequestHasher.java`
  - `src/main/java/com/usersy628/coffeeorder/point/application/PointChargeRetryProperties.java`
  - `src/main/java/com/usersy628/coffeeorder/point/application/PointWalletRepository.java`
  - `src/main/java/com/usersy628/coffeeorder/point/application/PointHistoryRepository.java`
  - `src/main/java/com/usersy628/coffeeorder/point/domain/PointWallet.java`
  - `src/main/java/com/usersy628/coffeeorder/point/domain/PointHistory.java`
  - `src/main/java/com/usersy628/coffeeorder/point/domain/PointHistoryType.java`
  - `src/main/java/com/usersy628/coffeeorder/point/infrastructure/PointWalletJpaRepository.java`
  - `src/main/java/com/usersy628/coffeeorder/point/infrastructure/PointHistoryJpaRepository.java`
  - `src/main/java/com/usersy628/coffeeorder/global/error/ErrorCode.java`
  - `src/main/java/com/usersy628/coffeeorder/global/error/GlobalExceptionHandler.java`
  - `src/main/resources/application.yml`
  - `src/test/java/com/usersy628/coffeeorder/point/api/PointChargeApiIntegrationTest.java`
  - `src/test/java/com/usersy628/coffeeorder/point/application/PointChargeServiceTest.java`
  - `src/test/java/com/usersy628/coffeeorder/point/infrastructure/PointChargeConcurrencyIntegrationTest.java`
  - `src/test/java/com/usersy628/coffeeorder/global/error/GlobalExceptionHandlerTest.java`
  - `docs/IMPLEMENTATION_PLAN.md`
  - `docs/PROJECT_STATUS.md`
- 먼저 수행할 테스트 또는 검증:
  1. 실제 MySQL `8.4.10` 통합 테스트에서 유효한 충전 요청을 보내고 아직 endpoint가 없어 `404 ENDPOINT_NOT_FOUND`로 실패하는 RED를 확인한다.
  2. 충전 endpoint 골격을 추가한 뒤 DTO 검증 오류가 아직 기능별로 매핑되지 않아 `500`으로 실패하는 RED를 확인한다.
  3. 구현 후 정상 충전, 정확히 300,000P 허용, 범위·총잔액 한도, 사용자 없음, 지갑·이력 원자성을 검증한다.
  4. 같은 사용자와 멱등 키의 같은 금액은 최초 결과를 재현하고 다른 금액은 `409 IDEMPOTENCY_KEY_REUSED`인지 검증한다.
  5. 동일 사용자 100개 동시 요청, 서로 다른 사용자 병렬 요청, 실제 락 타임아웃과 데드락 분류·전체 명령 재시도를 검증한다.
- 구현 범위:
  - `userId`, `amount`, `Idempotency-Key` 검증과 충전 기능별 400·404·409·503 오류 계약
  - `amount` canonical payload의 SHA-256 해시와 대소문자를 구분하는 원문 멱등 키 저장
  - `point_wallet`을 먼저 `SELECT ... FOR UPDATE`로 잠근 뒤 멱등 이력을 current read로 재확인
  - 지갑 증가와 `CHARGE point_history` 저장을 한 `@Transactional(timeout = 5)` 명령으로 커밋
  - 최초 요청은 `Idempotency-Replayed: false`, 같은 요청 재현은 `true`와 최초 잔액·시각 반환
  - 명령 트랜잭션 밖 coordinator에서 최초 시도 후 최대 2회, 50ms·100ms와 ±20% jitter로 전체 명령 재시도
  - 이름이 지정된 충전 멱등 유니크 제약 충돌만 전체 롤백 후 새 read-only transaction에서 재현 또는 409 판단
  - UTC `Instant`를 MySQL `DATETIME(6)`으로 저장하고 API 경계에서 `Asia/Seoul` 오프셋으로 변환
- 제외 범위:
  - 주문, 포인트 차감, Outbox와 인기 메뉴 구현
  - 외부 결제·PG API와 Redis 분산 락
  - 인증 principal과 `{userId}` 대조
  - Flyway schema와 seed 변경
  - 실패 응답·처리 중 상태·멱등 키 만료를 저장하는 별도 멱등성 테이블
- 완료 조건:
  - 충전 금액 범위와 총잔액 한도, 정확히 300,000P 경계가 API와 실제 MySQL에서 일치한다.
  - 지갑 잔액과 충전 이력이 한 트랜잭션으로 반영되고 실패 시 함께 롤백된다.
  - 같은 키·같은 금액은 한 번만 충전되고 최초 응답의 잔액과 시각을 재현하며, 다른 금액은 409가 된다.
  - 동시 요청에서도 잔액·이력이 유실되지 않고 락·데드락 재시도 소진은 503이 된다.
  - 오류 body `traceId`와 `X-Trace-Id`, `Idempotency-Replayed` 응답 헤더 계약이 지켜진다.
  - 전체 테스트와 `bootJar`, 필수 CI가 성공한다.
  - PR을 별도 검토와 사용자의 명시적 승인 전까지 병합하지 않는다.
- 검증 명령:

```powershell
docker info
.\gradlew.bat test --tests "com.usersy628.coffeeorder.point.api.PointChargeApiIntegrationTest"
.\gradlew.bat test --tests "com.usersy628.coffeeorder.point.application.PointChargeServiceTest"
.\gradlew.bat test --tests "com.usersy628.coffeeorder.point.infrastructure.PointChargeConcurrencyIntegrationTest"
.\gradlew.bat test --tests "com.usersy628.coffeeorder.global.error.GlobalExceptionHandlerTest"
.\gradlew.bat test --tests "com.usersy628.coffeeorder.point.*"
.\gradlew.bat clean test
.\gradlew.bat bootJar
git diff --check
git status --short
gh pr checks
```

## 최근 완료 작업

### [`S6-02`](https://github.com/usersy628/coffee-order-system/issues/22) 메뉴 시간 매핑과 README 상태 정합성 보완

- 상태: `DONE`
- 완료 커밋: `80252b8`, `525d985`, `fd26552`
- 목적: PR #19 사후 검토에서 확인된 메뉴 시간 타입과 README 구현 상태의 불일치를 UTC·마이크로초 정책과 현재 완료 상태에 맞춘다.
- 요구사항 근거:
  - `README.md`의 `시간 저장 기준`
  - `README.md`의 `테이블 설계 > menu`
  - `README.md`의 문서 상단 구현 상태와 `다음 단계`
  - PR #19 사후 검토 결과
- 선행 작업: `S6-01`, `S5-04` 완료와 PR #19, #21의 `dev` 병합
- 작업 브랜치: 최신 `dev`에서 `feature/issue-22-menu-time-readme-alignment` 생성
- 대상 파일:
  - `src/main/java/com/usersy628/coffeeorder/menu/domain/Menu.java`
  - `src/test/java/com/usersy628/coffeeorder/menu/infrastructure/MenuPersistenceIntegrationTest.java`
  - `README.md`
  - `docs/IMPLEMENTATION_PLAN.md`
  - `docs/PROJECT_STATUS.md`
- 먼저 수행할 테스트 또는 검증:
  1. 실제 MySQL `8.4.10`의 `menu.created_at`, `updated_at`에 서로 다른 6자리 소수 초 UTC 값을 삽입한다.
  2. JPA로 조회한 두 필드가 현재 `LocalDateTime`이어서 `Instant` 타입·UTC 순간 계약에 실패하는 RED를 확인한다.
  3. 구현 후 두 값이 예상한 `Instant`와 같고 나노초가 마이크로초 단위인지 검증한다.
  4. `rg`로 README에 완료된 메뉴 목록이 다음 구현 대상으로 남아 있지 않고 포인트 충전 준비 단계와 일치하는지 확인한다.
- 구현 범위:
  - `Menu.createdAt`, `Menu.updatedAt`을 `Instant`로 매핑
  - MySQL `DATETIME(6)` 조회 시 UTC 순간과 마이크로초 정밀도 검증
  - README 상단에서 메뉴 목록 완료와 다음 준비 대상인 포인트 충전을 명시
  - README `다음 단계`에서 완료된 메뉴 목록을 분리하고 남은 마일스톤 순서를 갱신
- 제외 범위:
  - 메뉴 API 응답 필드와 JSON 계약 변경
  - schema, Flyway migration과 seed 변경
  - 포인트 충전 API 구현 또는 `S7-01`의 `READY` 전환
  - PR #19 revert
- 완료 조건:
  - 메뉴 시간 필드가 애플리케이션 UTC `Instant` 정책과 일치한다.
  - 실제 MySQL `8.4.10`의 `DATETIME(6)` 값이 JPA 조회 후 같은 UTC 순간과 마이크로초 정밀도를 유지한다.
  - README와 `PROJECT_STATUS.md`의 현재 완료 상태와 다음 준비 작업이 일치한다.
  - 전체 테스트와 `bootJar`, 필수 CI가 성공한다.
  - PR을 별도 검토와 사용자의 명시적 승인 전까지 병합하지 않는다.
- 검증 명령:

```powershell
docker info
.\gradlew.bat test --tests "com.usersy628.coffeeorder.menu.infrastructure.MenuPersistenceIntegrationTest"
.\gradlew.bat test --tests "com.usersy628.coffeeorder.menu.*"
.\gradlew.bat clean test
.\gradlew.bat bootJar
rg -n "다음 구현 대상|메뉴 목록 조회 구현 및 테스트|포인트 충전" README.md docs\PROJECT_STATUS.md
git diff --check
git status --short
gh pr checks
```

### [`S5-04`](https://github.com/usersy628/coffee-order-system/issues/20) PR 검토와 명시적 병합 승인 규칙 추가

- 상태: `DONE`
- 완료 커밋: `d2d55bb`, `24fb9ef`, `b232396`
- 목적: PR 생성과 필수 CI 성공을 병합 승인으로 간주하지 않고, 별도 검토와 사용자의 명시적 승인을 `dev` 병합의 필수 조건으로 고정한다.
- 요구사항 근거:
  - `AGENTS.md`의 `개발 흐름`
  - issue #20에서 확정한 자동 병합 금지 정책
- 선행 작업: `S5-03` 완료와 필수 `Build and test` check 구성
- 작업 브랜치: 최신 `dev`에서 `feature/issue-20-explicit-merge-approval` 생성
- 대상 파일:
  - `AGENTS.md`
  - `docs/IMPLEMENTATION_PLAN.md`
  - `docs/PROJECT_STATUS.md`
- 먼저 수행할 테스트 또는 검증:
  1. 현재 `AGENTS.md`가 PR 생성·검토·병합을 한 문장에 두고 명시적 승인 주체와 대기 지점을 구분하지 않는지 확인한다.
  2. 자동 병합 금지, 별도 검토, 지적 사항 반영·재검증과 사용자 승인 조건을 서로 독립적인 규칙으로 작성한다.
  3. `rg`로 `자동 병합`, `별도 검토`, `명시적으로 병합` 문구가 모두 존재하는지 확인한다.
- 구현 범위:
  - PR 생성과 필수 CI 성공 후 자동 병합하지 않고 검토 대기 상태로 인수인계
  - 열린 PR을 별도 검토자가 검토하고 지적 사항을 같은 브랜치에 반영한 뒤 재검증
  - 사용자가 해당 PR의 병합을 명시적으로 승인한 경우에만 `dev`에 병합
  - 구현 요청, 다음 작업 진행 요청과 CI 성공을 병합 승인으로 해석하지 않음
- 제외 범위:
  - GitHub Actions, branch protection과 리뷰어 권한 설정 변경
  - 애플리케이션 코드와 테스트 변경
  - 이미 병합된 PR의 revert 또는 소급 변경
- 완료 조건:
  - 병합 전 검토 대기 지점과 병합 승인 주체가 `AGENTS.md`에 명확히 기록된다.
  - 모호한 진행 요청만으로 병합할 수 없다는 규칙이 기록된다.
  - 문서 검증과 필수 CI가 성공한 열린 PR을 만들고 병합하지 않은 채 검토 대기로 인수인계한다.
- 검증 명령:

```powershell
rg -n "자동 병합|별도 검토|명시적으로 병합|병합 승인" AGENTS.md
git diff --check
git status --short
gh pr checks
```

## 완료 작업 상세

### [`S5-03`](https://github.com/usersy628/coffee-order-system/issues/17) 리뷰 후속 기반 검증 보완

- 상태: `DONE`
- 완료 커밋: `a3fecab`, `45612f5`, `0319638`, `e018bf8`
- 목적: PR #13, #14, #16 리뷰에서 확인된 공통 MVC 예외 오분류, Flyway 재실행 검증 공백과 PR 자동 검증 부재를 후속 기능 구현 전에 보완한다.
- 요구사항 근거:
  - `README.md`의 `API 명세 > 공통 규칙`
  - `README.md`의 `예외 처리와 추적`
  - `README.md`의 `테스트 전략`
  - `README.md`의 `주요 오류 정책`
- 선행 작업: `S5-02` 완료와 issue #2의 `dev` 병합
- 작업 브랜치: 최신 `dev`에서 `feature/issue-17-foundation-review-fixes` 생성
- 대상 파일:
  - `src/main/java/com/usersy628/coffeeorder/global/error/ErrorCode.java`
  - `src/main/java/com/usersy628/coffeeorder/global/error/GlobalExceptionHandler.java`
  - `src/test/java/com/usersy628/coffeeorder/global/error/GlobalExceptionHandlerTest.java`
  - `src/test/java/com/usersy628/coffeeorder/support/testcontainers/DatabaseSmokeTest.java`
  - `.github/workflows/ci.yml`
  - `docs/IMPLEMENTATION_PLAN.md`
  - `docs/PROJECT_STATUS.md`
- 먼저 수행할 테스트 또는 검증:
  1. `GlobalExceptionHandlerTest`의 테스트용 endpoint에 `userId` 경로 변수, 필수 `Idempotency-Key`와 JSON 요청을 추가한다.
  2. 잘못된 `userId` 타입·범위, 필수 헤더 누락과 지원하지 않는 `Content-Type`이 현재 catch-all에 의해 `500 INTERNAL_SERVER_ERROR`로 실패하는 RED를 확인한다.
  3. 각 4xx 응답의 오류 코드, 빈 또는 제한된 `details`, body `traceId`와 `X-Trace-Id` 일치를 검증한다.
  4. `DatabaseSmokeTest`에서 이미 적용된 DB에 `flyway.migrate()`를 다시 호출하고 적용 건수가 0이며 이어지는 `validate()`가 성공하는지 검증한다.
  5. GitHub Actions를 추가한 PR에서 Java 17·Docker 기반 전체 테스트, `bootJar`와 `git diff --check`가 성공하는지 확인한다.
- 구현 범위:
  - `MethodArgumentTypeMismatchException`의 `userId` 오류를 `400 INVALID_USER_ID`로 변환
  - `HandlerMethodValidationException`의 `userId` 제약 위반을 `400 INVALID_USER_ID`로 변환
  - `MissingRequestHeaderException`의 `Idempotency-Key` 누락을 `400 IDEMPOTENCY_KEY_REQUIRED`로 변환
  - `HttpMediaTypeNotSupportedException`을 `415 UNSUPPORTED_MEDIA_TYPE`으로 변환
  - 예상된 4xx의 내부 예외 정보 비노출과 traceId 응답 계약 유지
  - Flyway 재실행 시 추가 적용 migration 0건과 `validate()` 성공 검증
  - `dev` 대상 PR에서 Gradle Wrapper, Java 17, Docker, `clean test`, `bootJar`, `git diff --check`를 검증하는 GitHub Actions
  - workflow 최초 실행 성공 후 `dev` 브랜치에 해당 required check 설정
  - `S7-01`과 `S8-01`에서 `MethodArgumentNotValidException`을 각각 기능별 오류 코드와 `details.fieldErrors`로 처리하도록 작업 순서 명시
- 제외 범위:
  - 메뉴·포인트·주문·인기 메뉴 API 구현
  - `MethodArgumentNotValidException`을 하나의 공통 오류 코드로 고정
  - 과거 PR과 당시 `PROJECT_STATUS.md`의 소급 수정
  - `S12-01`의 전체 API 오류·traceId·로그 최종 회귀 검증 제거
  - 새 Flyway migration, schema 또는 seed 변경
- 완료 조건:
  - 정의된 공통 MVC 4xx가 catch-all `500`으로 오분류되지 않는다.
  - 각 오류 body의 `traceId`와 `X-Trace-Id`가 일치하고 내부 예외 정보가 노출되지 않는다.
  - 이미 적용된 MySQL `8.4.10` DB에서 `flyway.migrate()` 재호출의 적용 건수가 0이고 `validate()`가 성공한다.
  - 로컬 전체 테스트와 `bootJar`, GitHub Actions가 성공한다.
  - `dev` 브랜치에서 CI check가 필수로 설정된다.
  - 기능별 DTO 검증 오류의 구현 책임이 `S7-01`과 `S8-01`에 명확히 남는다.
- 검증 명령:

```powershell
docker info
.\gradlew.bat test --tests "com.usersy628.coffeeorder.global.error.GlobalExceptionHandlerTest"
.\gradlew.bat test --tests "com.usersy628.coffeeorder.support.testcontainers.DatabaseSmokeTest"
.\gradlew.bat clean test
.\gradlew.bat bootJar
git diff --check
git status --short
gh pr checks
```

### [`S6-01`](https://github.com/usersy628/coffee-order-system/issues/3) 메뉴 목록 조회 API 구현

- 상태: `DONE`
- 완료 커밋: `94c8b28`, `eacde62`, `c7f599b`
- 목적: 판매 상태와 관계없이 전체 메뉴를 ID 오름차순으로 조회하는 `GET /api/menus`를 구현하고 HTTP 계약부터 실제 MySQL 조회까지 검증한다.
- 요구사항 근거:
  - `README.md`의 `기술 스택과 프로젝트 구조 > 패키지와 계층 경계`
  - `README.md`의 `API 명세 > 공통 규칙`
  - `README.md`의 `API 명세 > 메뉴 목록 조회`
  - `README.md`의 `테이블 설계 > menu`
  - `README.md`의 `테스트 전략`
- 선행 작업: `S5-03` 완료와 issue #17의 `dev` 병합
- 작업 브랜치: issue #17 병합 후 최신 `dev`에서 `feature/issue-3-menu-list-api` 생성
- 대상 파일:
  - `src/main/java/com/usersy628/coffeeorder/menu/domain/Menu.java`
  - `src/main/java/com/usersy628/coffeeorder/menu/domain/MenuStatus.java`
  - `src/main/java/com/usersy628/coffeeorder/menu/application/MenuQueryRepository.java`
  - `src/main/java/com/usersy628/coffeeorder/menu/application/MenuQueryService.java`
  - `src/main/java/com/usersy628/coffeeorder/menu/infrastructure/MenuJpaRepository.java`
  - `src/main/java/com/usersy628/coffeeorder/menu/api/MenuController.java`
  - `src/main/java/com/usersy628/coffeeorder/menu/api/MenuResponse.java`
  - `src/test/java/com/usersy628/coffeeorder/menu/api/MenuControllerTest.java`
  - `src/test/java/com/usersy628/coffeeorder/menu/api/MenuApiIntegrationTest.java`
- 먼저 수행할 테스트 또는 검증:
  1. 운영 클래스를 추가하기 전에 기존 `MockMvc`와 `JdbcTemplate`만 사용하는 `MenuApiIntegrationTest`를 작성한다.
  2. `GET /api/menus`가 아직 없어 `404 ENDPOINT_NOT_FOUND`로 실패하는 것을 확인한다.
  3. 테스트 트랜잭션에서 메뉴를 ID `30`, `10`, `20` 순서로 삽입하고 응답은 `10`, `20`, `30` 순서인지 검증한다.
  4. `STOPPED` 메뉴 포함, 네 응답 필드, 서버 생성 `X-Trace-Id`와 메뉴가 없을 때 `200 OK`, 빈 배열을 검증한다.
  5. 구현 후 `MenuControllerTest`에서 application service를 대체하여 같은 HTTP 계약을 DB 없이 빠르게 검증한다.
- 구현 범위:
  - `Menu`와 `MenuStatus.ON_SALE`, `MenuStatus.STOPPED` JPA 매핑
  - application 계층의 `MenuQueryRepository.findAllByOrderByIdAsc()` 조회 port
  - `@Transactional(readOnly = true)`인 `MenuQueryService`
  - `MenuJpaRepository`가 Spring Data repository와 application port를 함께 확장하여 프록시가 port를 직접 구현하는 최소 adapter 구성
  - 쿼리 메서드의 `ORDER BY id ASC`로 DB 조회 순서 보장
  - 엔티티를 직접 직렬화하지 않고 `MenuResponse(menuId, name, price, status)`로 변환
  - `GET /api/menus`, `200 OK`, `application/json`
- 제외 범위:
  - 메뉴 등록·수정·삭제와 판매 상태 변경 API
  - 페이지네이션, 검색, 필터링과 정렬 query parameter
  - Redis 캐시, read replica와 별도 조회 모델
  - `/api/v1` 경로
  - 메뉴가 없을 때 `404` 반환
  - 새 Flyway migration 또는 seed 변경
  - 기능 전용 오류 코드, UseCase interface, 범용 Mapper, BaseEntity와 수동 RepositoryAdapter
- 완료 조건:
  - `GET /api/menus`가 `menuId`, `name`, `price`, `status`만 반환한다.
  - 판매 중지 메뉴를 포함한 모든 메뉴가 ID 오름차순으로 반환된다.
  - 메뉴가 없으면 `200 OK`와 빈 JSON 배열을 반환한다.
  - 실제 MySQL `8.4.10`에서 JPA 매핑과 전체 HTTP 경로가 성공한다.
  - 통합 테스트가 V2 seed의 삽입 순서에 우연히 의존하지 않는다.
  - 기존 테스트를 포함한 전체 테스트와 `bootJar`가 성공한다.
  - H2, 캐시와 불필요한 계층·의존성이 추가되지 않는다.
- 검증 명령:

```powershell
docker info
.\gradlew.bat test --tests "com.usersy628.coffeeorder.menu.api.MenuApiIntegrationTest"
.\gradlew.bat test --tests "com.usersy628.coffeeorder.menu.api.MenuControllerTest" --tests "com.usersy628.coffeeorder.menu.api.MenuApiIntegrationTest"
.\gradlew.bat clean test
.\gradlew.bat bootJar
git diff --check
git status --short
```

### [`S5-02`](https://github.com/usersy628/coffee-order-system/issues/2) Spring Boot 기본 구조와 MySQL 통합 테스트 환경 구성

- 상태: `DONE`
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
  2. Spring Boot `3.5.16`, Gradle Wrapper `8.14.5`와 승인된 기본 의존성만 포함한 최소 빌드 구조를 만든다.
  3. `CoffeeOrderApplicationTests`의 context load 테스트를 먼저 실행한다.
  4. 성공·오류 응답의 `X-Trace-Id`, 오류 body의 `traceId`와 요청 종료 후 MDC 정리를 확인하는 실패 테스트를 작성한 뒤 오류·trace 기반을 구현한다.
  5. 빈 MySQL `8.4.10` 컨테이너의 Flyway 적용, Hibernate `validate`, seed 사용자별 0P 지갑과 session 락 대기 2초를 확인하는 실패 테스트를 작성한 뒤 설정과 migration을 구현한다.
- 구현 범위:
  - Java 17 toolchain, Spring Boot `3.5.16`, Gradle Wrapper `8.14.5` Groovy DSL
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

## PR 본문 연계 규칙

- PR을 만들 때 해당 작업의 상세 제목부터 `검증 명령` 코드 블록까지를 생략하거나 요약하지 않고 PR 본문의 `Implementation Plan 작업 상세`에 복사한다.
- 이 복사본은 리뷰 시점의 계획을 확인하기 위한 snapshot이며, 제품 요구사항과 작업 상태의 단일 기준은 계속 `README.md`와 이 문서다.
- 실제 구현 결과와 검증 결과는 작업 상세를 수정해 섞지 않고 PR의 별도 섹션에 기록한다.
- 대상 파일, 구현·제외 범위 또는 완료 조건이 계획과 달라졌다면 이 문서를 먼저 갱신하고 PR의 `계획 대비 변경`에 이유와 영향을 적는다. 차이가 없으면 `없음`으로 적는다.
- `.github/pull_request_template.md`의 placeholder와 안내 주석은 실제 내용으로 교체하거나 제거한다.

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
