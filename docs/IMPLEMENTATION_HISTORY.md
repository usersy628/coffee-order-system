# Implementation History

이 문서는 `DONE` 작업의 전체 작업 상세와 실제 검증 결과를 보존한다. 전체 상태와 완료되지 않은 작업은 [`IMPLEMENTATION_PLAN.md`](IMPLEMENTATION_PLAN.md), 현재 인수인계는 [`PROJECT_STATUS.md`](PROJECT_STATUS.md)에서 확인한다.

작업을 완료하는 PR의 최종 문서 커밋에서 상세를 Plan에서 이 문서로 옮기고 issue, PR, merge commit, 완료일과 실제 검증 결과를 기록한다. PR이 아직 병합 전이면 merge commit은 `병합 후 기록`으로 두고, 병합을 명시적으로 승인받은 뒤 후속 상태 갱신에서 실제 해시로 교체한다.

## 완료 작업 상세

### [`DOC-01`](https://github.com/usersy628/coffee-order-system/issues/25) 구현 계획과 완료 이력 분리

- 상태: `DONE`
- issue: [#25](https://github.com/usersy628/coffee-order-system/issues/25)
- PR: [#26](https://github.com/usersy628/coffee-order-system/pull/26)
- merge commit: 병합 후 기록
- 완료일: 2026-07-15
- 완료 커밋: `318e53f`
- 목적: 완료된 작업 상세를 별도 History로 옮겨 활성 계획의 길이와 컨텍스트 비용을 줄이면서 계획과 실제 결과의 추적성을 유지한다.
- 요구사항 근거:
  - `AGENTS.md`의 `개발 흐름`, `구현 작업 관리`, `상태 인수인계`
  - `.github/pull_request_template.md`
- 선행 작업: PR #24가 `dev`에 병합되고 issue #4가 닫힘
- 작업 브랜치: 최신 `dev`에서 `feature/issue-25-implementation-history` 생성
- 대상 파일:
  - `AGENTS.md`
  - `.github/pull_request_template.md`
  - `docs/IMPLEMENTATION_PLAN.md`
  - `docs/IMPLEMENTATION_HISTORY.md`
  - `docs/PROJECT_STATUS.md`
- 먼저 수행할 테스트 또는 검증:
  1. Git과 GitHub를 조회해 `dev` HEAD, PR #24와 issue #4·#5 상태를 확인한다.
  2. `PROJECT_STATUS.md`가 아직 PR #24 검토 대기를 가리키고 History 파일이 없음을 확인한다.
  3. PR #24 본문을 UTF-8 `--body-file`로 복구한 뒤 GitHub 원본의 한글과 물음표 손실 여부를 확인한다.
- 구현 범위:
  - Plan에는 전체 상태 인덱스와 완료되지 않은 작업 상세만 유지
  - History에 기존 `DONE` 상세와 검증 결과를 내용 손실 없이 이동
  - Project Status를 현재 상태와 다음 한 가지 행동 중심으로 갱신
  - 완료 상세가 History로 이동한 경우 PR 본문 원본으로 History를 사용할 수 있도록 작업 규칙과 템플릿 보완
  - PR·이슈 본문을 UTF-8 파일로 전달하고 GitHub 원본을 재검증하는 규칙 추가
- 제외 범위:
  - `S8-01` 주문·결제 구현과 `READY` 전환
  - 애플리케이션 코드와 테스트 변경
  - PR 자동 병합
- 완료 조건:
  - Plan, History와 Project Status의 역할과 링크가 일치한다.
  - 기존 완료 작업 상세가 History에 보존된다.
  - 실제 Git 상태와 Project Status가 일치하고 `S8-01`은 `BACKLOG`다.
  - PR 본문 전체 상세의 출처와 UTF-8 검증 절차가 AGENTS와 PR 템플릿에 명시된다.
  - `git diff --check`와 문서 정합성 검증이 성공한다.
- 실제 검증 결과:
  - `dev`와 `origin/dev`가 `2fde3f2`로 일치하고 작업 시작 전 트리가 clean임을 확인했다.
  - PR #24가 `MERGED`, issue #4가 `CLOSED`, issue #5가 `OPEN`이고 `S8-01` 브랜치가 없음을 확인했다.
  - PR #24 본문을 UTF-8 `--body-file`로 복구하고 GitHub 원본에서 한글 제목이 존재하며 예상하지 않은 리터럴 `?`가 0건임을 확인했다.
  - issue #25를 별도로 만들고 UTF-8 본문과 한글 제목을 GitHub 원본에서 확인했다.
  - Plan의 기존 완료 상세 6개를 History로 이동하고 issue #1·PR #13을 근거로 `S5-01` 상세를 복원해 전체 `DONE` 작업 7개의 상세를 보존했다.
  - `S8-01` 네 정책은 2026-07-15 사용자 승인으로 기록하되 `BACKLOG`와 구현 브랜치 없음 상태를 유지했다.
  - 문서 상대 링크 대상이 존재하고 `git diff --check`가 성공했다.
  - PR #26의 최신 head에서 필수 `Build and test`가 성공했다.
- 계획 대비 변경 사항:
  - 기존 Plan에 `S5-01` 상세가 없어서 issue #1, PR #13과 완료 커밋을 근거로 History에 복원했다.
  - PR #24 본문 복구 결과와 재발 방지용 UTF-8 검증 절차를 작업 규칙에 함께 기록했다.
- 검증 명령:

```powershell
git status --short --branch
git diff --check
rg -n "IMPLEMENTATION_PLAN|IMPLEMENTATION_HISTORY|PROJECT_STATUS|body-file|UTF-8" AGENTS.md docs .github/pull_request_template.md
gh issue view 25 --json number,state,url,title,body
gh pr view 24 --json number,state,url,body
```

### [`S7-01`](https://github.com/usersy628/coffee-order-system/issues/4) 포인트 충전·이력·멱등성·동시성 구현

- 상태: `DONE`
- issue: [#4](https://github.com/usersy628/coffee-order-system/issues/4)
- PR: [#24](https://github.com/usersy628/coffee-order-system/pull/24)
- merge commit: `2fde3f2`
- 완료일: 2026-07-14
- 완료 커밋: `1971658`, `3227271`, `648f225`, `6a1a688`, `4c1ebbf`
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
  - `src/main/java/com/usersy628/coffeeorder/point/application/PointChargeRetryFailureException.java`
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
  - `README.md`
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
  - README 구현 상태를 포인트 충전 완료와 다음 주문·결제 준비 단계로 갱신
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
- 실제 검증 결과:
  - 구현 전 `PointChargeApiIntegrationTest`는 실제 MySQL 8.4.10에서 `404 ENDPOINT_NOT_FOUND`로 실패하는 RED를 확인했다.
  - 포인트 API·재시도·동시성 및 기존 회귀를 포함한 전체 테스트 45개가 성공했고 실패·오류·skip은 0개다.
  - 동일 사용자 100개 충전, 동일 멱등 키 100개 요청, 서로 다른 사용자 병렬 충전, 실제 락 타임아웃과 강제 데드락을 검증했다.
  - 이력 저장 실패 시 지갑 변경도 롤백되고 UTC 마이크로초 DB 시각과 `+09:00` 응답이 같은 순간인지 검증했다.
  - `bootJar`와 `git diff --check`가 성공했다.
  - PR #24 리뷰 후 503 재시도 실패가 시도 횟수와 원인 타입을 보존하고 민감정보 없이 WARN으로 기록되는 계약 테스트를 추가했다.
- 계획 대비 변경 사항:
  - 기능 완료 뒤 README 구현 상태가 뒤처지지 않도록 `README.md`를 대상 파일과 구현 범위에 추가했다.
  - PR #24 리뷰에 따라 `PointChargeRetryFailureException`을 추가해 503 응답 변환 전 운영 로그 문맥을 보존했다.
  - Flyway schema와 seed는 계획대로 변경하지 않았다.
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

## 후속 완료 작업

### [`S6-02`](https://github.com/usersy628/coffee-order-system/issues/22) 메뉴 시간 매핑과 README 상태 정합성 보완

- 상태: `DONE`
- issue: [#22](https://github.com/usersy628/coffee-order-system/issues/22)
- PR: [#23](https://github.com/usersy628/coffee-order-system/pull/23)
- merge commit: `8bc6c92`
- 완료일: 2026-07-14
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
- issue: [#20](https://github.com/usersy628/coffee-order-system/issues/20)
- PR: [#21](https://github.com/usersy628/coffee-order-system/pull/21)
- merge commit: `e272afa`
- 완료일: 2026-07-14
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

## 기반 완료 작업

### [`S5-03`](https://github.com/usersy628/coffee-order-system/issues/17) 리뷰 후속 기반 검증 보완

- 상태: `DONE`
- issue: [#17](https://github.com/usersy628/coffee-order-system/issues/17)
- PR: [#18](https://github.com/usersy628/coffee-order-system/pull/18)
- merge commit: `cdf44a8`
- 완료일: 2026-07-14
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
- issue: [#3](https://github.com/usersy628/coffee-order-system/issues/3)
- PR: [#19](https://github.com/usersy628/coffee-order-system/pull/19)
- merge commit: `8591064`
- 완료일: 2026-07-14
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
- issue: [#2](https://github.com/usersy628/coffee-order-system/issues/2)
- PR: [#14](https://github.com/usersy628/coffee-order-system/pull/14)
- merge commit: `c9fac6c`
- 완료일: 2026-07-14
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

### [`S5-01`](https://github.com/usersy628/coffee-order-system/issues/1) 기술 스택과 프로젝트 구조 확정

- 상태: `DONE`
- issue: [#1](https://github.com/usersy628/coffee-order-system/issues/1)
- PR: [#13](https://github.com/usersy628/coffee-order-system/pull/13)
- merge commit: `8662d50`
- 완료일: 2026-07-14
- 완료 커밋: `fe6d65b`
- 목적: 승인된 기술 스택, 패키지 경계, 설정과 테스트 구성을 설계 기준으로 확정하고 다음 구현 작업을 실행 가능한 상태로 준비한다.
- 요구사항 근거:
  - `README.md`의 `기술 스택과 프로젝트 구조`
  - 2~4단계에서 승인된 도메인·API·동시성·Outbox 설계
- 선행 작업: 2~4단계 설계 완료
- 작업 브랜치: 최신 `dev`에서 `feature/issue-1-tech-stack-structure` 생성
- 대상 파일:
  - `README.md`
  - `docs/IMPLEMENTATION_PLAN.md`
  - `docs/PROJECT_STATUS.md`
- 먼저 수행할 테스트 또는 검증:
  1. 승인된 Java, Spring Boot, Gradle, MySQL 버전과 패키지 경계가 README에 구체화되지 않은 상태를 확인한다.
  2. 문서 변경 후 링크, 상태와 다음 작업의 선행 관계가 일치하는지 확인한다.
- 구현 범위:
  - Java 17, Spring Boot 3.5.16, Gradle Wrapper와 MySQL 8.4.10 LTS 선택 및 대안·트레이드오프 기록
  - 기능 중심 패키지 구조와 api·application·domain·infrastructure 경계 기록
  - Flyway, Testcontainers, RestClient·Apache HttpClient 5, MockRestServiceServer·WireMock 적용 방식 기록
  - `S5-01` 완료와 다음 `S5-02` 준비 상태 반영
- 제외 범위:
  - Gradle Wrapper, `build.gradle`과 Spring Boot 소스 파일 생성
  - 기능 구현과 데이터베이스 migration 작성
- 완료 조건:
  - 승인 내용과 선택 이유가 README에 반영된다.
  - 다음 프로젝트 기본 구조 작업을 실행할 기준이 구체화된다.
  - 문서 간 상태와 작업 흐름이 서로 일치한다.
  - `git diff --check`와 작업 트리 확인이 성공한다.
- 실제 검증 결과:
  - 승인된 기술 스택, 기능 중심 패키지 구조와 테스트 도구 선택을 README에 기록했다.
  - 후속 `S5-02`가 프로젝트 기본 구조를 생성할 수 있도록 선행 관계와 검증 기준을 구체화했다.
  - 문서 변경만 수행하고 애플리케이션 코드는 생성하지 않았다.
  - `git diff --check`가 성공했다.
- 계획 대비 변경 사항: 없음
- 검증 명령:

```powershell
git diff --check
git status --short
```
새 완료 작업은 `완료 작업 상세`의 맨 위에 추가한다.
