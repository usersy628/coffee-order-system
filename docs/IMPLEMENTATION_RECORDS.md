# Implementation Records

이 문서는 DOC-02부터 PR에 제출한 작업의 불변 제출 기록을 보존한다. 각 기록은 제출 당시의 계획, 실제 구현 결과, 계획 대비 변경과 검증 근거를 담는다. issue·PR·CI·병합의 현재 상태는 기록의 링크를 따라 GitHub에서 확인한다.

## 읽는 방법

- PR_SUBMISSION은 PR에 제출한 변경의 정적 스냅샷이며, 검토·병합·CI의 현재 상태를 뜻하지 않는다.
- 기록에는 issue와 PR 링크, 구현 커밋, 실행한 검증 명령처럼 바뀌지 않는 근거만 적는다.
- 기록을 만든 뒤 GitHub의 상태가 바뀌어도 이 문서를 맞추기 위해 수정하지 않는다. 라이브 상태는 GitHub 링크가 단일 기준이다.
- 새 PR은 Plan의 전체 상세를 본문에 넣어 제출하고, PR URL이 생긴 뒤 같은 브랜치의 최종 문서 커밋에서 그 상세를 이 문서로 옮긴다.

## PR 제출 기록

### [S13-01](https://github.com/usersy628/coffee-order-system/issues/10) README 실행 방법과 구현 근거를 완성한다

- 기록 유형: PR_SUBMISSION
- 기록일: 2026-07-15
- 제출 브랜치: feature/issue-10-readme-runbook
- 연결:
  - 이슈: [#10](https://github.com/usersy628/coffee-order-system/issues/10)
  - PR: [#34](https://github.com/usersy628/coffee-order-system/pull/34)
  - 준비 계획 커밋: 2af6b0a
  - 구현 커밋: 3313265
  - 계획 정렬 커밋: 172eded
- 목적: 처음 받은 개발자가 README만으로 일반 로컬 MySQL, Spring Boot `local` 프로필, Testcontainers 테스트와 과제용 API 예시를 실제 구현과 같은 방식으로 실행·검증하게 한다.
- 요구사항 근거:
  - [README.md의 런타임과 빌드 도구](../README.md#런타임과-빌드-도구)
  - [README.md의 설정, 초기 데이터와 테스트 구성](../README.md#설정-초기-데이터와-테스트-구성)
  - [README.md의 API 명세](../README.md#api-명세)
  - [README.md의 테스트 전략](../README.md#테스트-전략)
  - [issue #10](https://github.com/usersy628/coffee-order-system/issues/10)
- 선행 작업: S12-01의 오류·traceId 계약과 PR #33의 제출 기록을 반영한 최신 `origin/dev`를 기준으로 한다.
- 작업 브랜치: feature/issue-10-readme-runbook
- 대상 파일:
  - README.md
  - .env.example
  - docs/IMPLEMENTATION_PLAN.md
  - docs/PROJECT_STATUS.md
- 먼저 수행한 검증:
  1. 일반 로컬 `compose.yaml`, `.env.example`, `application-local.yml`의 MySQL image·환경 변수·loopback port가 같은 계약인지 확인했다.
  2. `application-local.yml`, `application-test.yml`, Flyway V2 초기 데이터와 Controller·DTO를 읽어 환경 변수, 초기 사용자·메뉴, API 예시가 실제 값과 같은지 확인했다.
  3. 성능 전용 `docker-compose.performance.yml`의 별도 DB·포트·비밀값 규칙을 확인해 일반 로컬 실행 절차와 섞이지 않게 했다.
- 구현 범위:
  - 기존 일반 개발용 MySQL 8.4.10 `compose.yaml`의 데이터베이스·사용자·비밀번호·host port 환경 변수, loopback 바인딩·UTC·healthcheck·named volume을 README에 정확히 설명했다.
  - README에 Java 17·Docker 준비 조건, `.env.example` 복사, Compose 기동, `SPRING_PROFILES_ACTIVE=local` 실행, IntelliJ 환경 변수 입력, `/actuator/health` 확인, Flyway V2 초기 데이터, Testcontainers 전체 테스트와 `bootJar` 절차를 작성했다.
  - `MYSQL_PORT=3307`, `SERVER_PORT=18080` 같은 개인 로컬 오버라이드는 지원하되 저장소 기본값이나 애플리케이션 설정을 개인 포트로 바꾸지 않았다.
  - 실제 Controller·DTO와 V2 초기 데이터에 맞춘 `curl.exe` 메뉴 조회·포인트 충전·주문·인기 메뉴 예시와 멱등 키 재사용 주의사항을 작성했다.
  - README의 구현 현황·다음 단계에서 이미 끝난 S11·S12를 최신 기록 경로와 남은 S14~S15 순서에 맞게 정리했다.
- 제외 범위:
  - Java·Spring·DB schema·migration·API 계약·CI·성능 전용 Compose의 동작 변경
  - 운영용 비밀번호·외부 데이터 플랫폼 URL·개인 IntelliJ 설정의 저장
  - 일반 로컬 실행에 성능 기준선의 `local,perf` DB·포트·환경 변수를 섞는 일
- 완료 조건:
  - 새 환경이 README의 순서만으로 일반 MySQL을 기동하고 `local` 앱의 health 및 초기 메뉴 조회까지 확인할 수 있다.
  - `MYSQL_PORT`와 `SERVER_PORT` 오버라이드가 Compose·애플리케이션 실행 설명에서 일관되고, Testcontainers 테스트가 로컬 개발 DB를 사용하지 않는다고 명시된다.
  - API 예시가 V2의 사용자 1~3, 판매 중인 메뉴 1~2, 실제 request/response·Idempotency 헤더 계약과 일치한다.
  - README의 현재 단계, 파일 참조, 일반·성능 실행 경계가 실제 저장소와 일치하고 문서 공백 검증·전체 테스트·bootJar·diff 검사가 성공한다.
- 검증 명령:

    docker compose --env-file .env.example -f compose.yaml config
    docker compose --env-file .env.example -p coffee-order-system-s13 -f compose.yaml up -d --wait
    $env:SPRING_PROFILES_ACTIVE='local'; $env:MYSQL_PORT='3309'; $env:SERVER_PORT='18082'; .\gradlew.bat bootRun
    curl.exe --fail http://127.0.0.1:18082/actuator/health
    curl.exe --fail http://127.0.0.1:18082/api/menus
    .\gradlew.bat test --no-daemon --rerun-tasks
    .\gradlew.bat bootJar --no-daemon
    git diff --check

#### 실제 구현 결과

- 일반 Compose와 기존 MySQL(예: 3307)을 구분한 README 실행 가이드, Java 17·Docker·IntelliJ·`local` 프로필·Flyway 초기 데이터·Testcontainers·성능 프로필 경계를 추가했다.
- `.env.example`에 Compose와 Gradle/IntelliJ 환경 변수의 전달 경계, 개발용 값의 성격을 명시했다.
- Windows PowerShell에서 POST JSON의 따옴표가 손실되지 않도록 `curl.exe` 표준입력과 `--data-binary '@-'`를 사용하는 메뉴·충전·주문·인기 메뉴 예시를 추가했다.
- README의 구현 현황과 남은 마일스톤을 S11·S12 완료 근거 및 S14~S15 순서에 맞췄다.

#### 계획 대비 변경

- 착수 직후 `.yml`만 검색해 기존 `compose.yaml`을 놓친 것을 발견했다. 구현 전에 파일·해시를 재확인하고 Compose 설정 변경을 취소했으며, Plan도 기존 Compose를 문서화하는 범위로 바로잡았다. 최종 변경에는 Compose 설정이 포함되지 않는다.

#### 실제 검증 결과

| 검증 명령 또는 확인 | 결과 |
| --- | --- |
| docker compose --env-file .env.example -f compose.yaml config | 성공. MySQL 8.4.10, loopback 3306, UTC, healthcheck와 named volume 계약 확인 |
| 격리 Compose MYSQL_PORT=3309, -p coffee-order-system-s13 up -d --wait | 성공. 전용 MySQL 컨테이너 healthy |
| local 프로필, MySQL 3309, 서버 18082 bootRun | 성공. Flyway V1~V3 적용, health UP, 초기 메뉴 1~3 조회 성공 |
| README PowerShell API 예시 | 충전 200·Idempotency-Replayed false, 주문 201·false, 같은 키·같은 주문 replay 200·true, 인기 메뉴 조회 성공 |
| test --no-daemon --rerun-tasks | JUnit XML 23개 파일, 105개 테스트, 실패 0, 오류 0 |
| bootJar --no-daemon | 성공 |
| git diff --check | 성공 |

### [S12-01](https://github.com/usersy628/coffee-order-system/issues/9) 공통 예외 처리와 API 오류 계약을 최종 보강한다

- 기록 유형: PR_SUBMISSION
- 기록일: 2026-07-15
- 제출 브랜치: feature/issue-9-global-error-contract
- 연결:
  - 이슈: [#9](https://github.com/usersy628/coffee-order-system/issues/9)
  - PR: [#33](https://github.com/usersy628/coffee-order-system/pull/33)
  - 구현 커밋: a048e96
- 목적: 모든 API가 같은 code, message, details, traceId 오류 응답과 X-Trace-Id 헤더를 사용하게 하고, 일시적 인프라 오류와 구현 결함을 서로 다르게 표현한다.
- 요구사항 근거:
  - [README.md의 예외 처리와 추적](../README.md#예외-처리와-추적)
  - [README.md의 공통 오류 응답](../README.md#공통-규칙)
  - [README.md의 테스트 전략](../README.md#테스트-전략)
  - [issue #9](https://github.com/usersy628/coffee-order-system/issues/9)
- 선행 작업: S6-01부터 S11-01의 API, 재시도 예외, traceId filter와 통합 테스트 기반을 사용한다.
- 정책:
  - ErrorCode에 SERVICE_UNAVAILABLE를 추가하고, Spring의 DataAccessResourceFailureException만 503으로 변환한다. 이 범위는 DB 연결·커넥션 풀처럼 일시적으로 사용할 수 없는 인프라 오류에 한정한다.
  - DataIntegrityViolationException 전체를 409로 바꾸지 않는다. 명령 계층에서 이미 식별한 멱등 유니크 충돌 외의 제약 위반은 catch-all을 통해 500 INTERNAL_SERVER_ERROR로 남긴다.
  - PointChargeRetryFailureException과 OrderRetryFailureException의 503 CONCURRENT_REQUEST_TIMEOUT 계약은 그대로 둔다. 재시도되지 않은 락 예외나 다른 DataAccessException을 넓게 503으로 바꾸지 않는다.
  - 4xx는 정보 로그, 재시도 소진과 일시적 인프라 503은 원문 예외 메시지 없이 원인 타입과 traceId만 경고 로그, 예상외 500은 서버 로그에 traceId와 스택을 남긴다. 응답에는 내부 메시지·스택·원문 Idempotency-Key·payload·인증정보를 노출하지 않는다.
  - TraceIdFilter가 생성한 서버 traceId는 외부 입력을 신뢰하지 않고 오류 body와 X-Trace-Id 헤더에 같은 값으로 표현하며 요청 종료 뒤 MDC에서 제거한다.
- 대상 파일:
  - src/main/java/com/usersy628/coffeeorder/global/error/ErrorCode.java
  - src/main/java/com/usersy628/coffeeorder/global/error/GlobalExceptionHandler.java
  - src/test/java/com/usersy628/coffeeorder/global/error/GlobalExceptionHandlerTest.java
  - docs/IMPLEMENTATION_PLAN.md
  - docs/PROJECT_STATUS.md
- 먼저 수행한 검증:
  1. GlobalExceptionHandlerTest에 DataAccessResourceFailureException을 던지는 test endpoint와 503 SERVICE_UNAVAILABLE 기대를 먼저 추가해, 구현 전에는 기존 catch-all의 500 응답으로 실패함을 확인했다.
  2. DataIntegrityViolationException을 던지는 test endpoint가 409로 오분류되지 않고 안전한 500 응답으로 남는지 확인했다.
  3. 각 새 오류 응답에서 body traceId와 X-Trace-Id가 같고, 응답 및 503 경고 로그에 테스트용 민감 문자열이 없는지 확인했다.
- 구현 범위:
  - SERVICE_UNAVAILABLE 오류 코드와 보수적인 인프라 예외 handler를 추가했다.
  - 기존 GlobalExceptionHandlerTest에 503 인프라 오류, 500 제약 위반, traceId·응답 비밀값·로그 비밀값 계약을 보강했다.
  - 기존 validation, domain, retry exhaustion, malformed JSON, Content-Type, endpoint-not-found 계약이 회귀하지 않는지 같은 테스트 클래스와 전체 테스트로 확인했다.
- 제외 범위:
  - 오류 코드마다 별도 예외 클래스 생성
  - DataAccessException 전체, DataIntegrityViolationException 전체 또는 재시도되지 않은 락 예외의 광범위한 503 변환
  - 재시도 횟수, Hikari 설정, DB schema, API 버전, 오류 응답 형식 변경
  - 로그 수집 인프라 또는 외부 observability 도입
- 완료 조건:
  - DB 연결·리소스 실패는 503 SERVICE_UNAVAILABLE, 재시도 소진은 503 CONCURRENT_REQUEST_TIMEOUT, 예상외 제약 위반은 500 INTERNAL_SERVER_ERROR로 구분된다.
  - 모든 새 오류 body는 빈 details, 안전한 message, 32자리 서버 traceId를 가지며 X-Trace-Id와 일치한다.
  - 응답과 일시적 인프라 경고 로그에 테스트용 원문 민감값이 없고, catch-all 500 응답에 내부 예외 정보가 없다.
  - 기존 전역 오류 테스트와 전체 테스트가 통과하고 bootJar와 git diff --check가 성공한다.
- 검증 명령:

    .\gradlew.bat test --tests "com.usersy628.coffeeorder.global.error.GlobalExceptionHandlerTest"
    .\gradlew.bat test --tests "com.usersy628.coffeeorder.global.trace.TraceIdFilterTest"
    .\gradlew.bat test --tests "com.usersy628.coffeeorder.point.api.PointChargeApiIntegrationTest"
    .\gradlew.bat test --tests "com.usersy628.coffeeorder.order.api.OrderApiIntegrationTest"
    .\gradlew.bat test --no-daemon --rerun-tasks
    .\gradlew.bat bootJar --no-daemon
    git diff --check

#### 실제 구현 결과

- `SERVICE_UNAVAILABLE` 오류 코드와 `DataAccessResourceFailureException` 전용 503 handler를 추가했다.
- handler는 원문 예외 메시지를 응답이나 경고 로그에 남기지 않고, 오류 코드·원인 타입·서버 생성 traceId만 기록한다.
- `DataIntegrityViolationException`은 광범위하게 409로 변환하지 않으며, 안전한 `INTERNAL_SERVER_ERROR` 응답으로 남는 계약을 테스트했다.
- 새 503·500 응답의 빈 details, body/header traceId 일치, 민감 문자열 비노출을 회귀 테스트로 고정했다.

#### 계획 대비 변경

- 없음

#### 실제 검증 결과

| 검증 명령 또는 확인 | 결과 |
| --- | --- |
| GlobalExceptionHandlerTest (구현 전) | 의도한 red: 13개 중 1개 실패. 기존 handler가 503 기대를 500으로 처리함을 확인 |
| GlobalExceptionHandlerTest (구현 후) | 13개 테스트 성공 |
| TraceIdFilterTest | 성공 |
| PointChargeApiIntegrationTest와 OrderApiIntegrationTest 대상 실행 | 성공 |
| test --no-daemon --rerun-tasks | JUnit XML 최종 집계 105개, 실패 0, 오류 0 |
| bootJar --no-daemon | 성공 |
| git diff --check | 성공 |
| gh pr view 33 --json title,body,url | 한글 제목·본문과 예상하지 않은 리터럴 ? 손실이 없음을 확인 |

### [DOC-02](https://github.com/usersy628/coffee-order-system/issues/31) PR lifecycle와 문서 상태의 기준 분리

- 기록 유형: PR_SUBMISSION
- 기록일: 2026-07-15
- 제출 브랜치: feature/issue-31-pr-lifecycle-docs
- 연결:
  - 이슈: [#31](https://github.com/usersy628/coffee-order-system/issues/31)
  - PR: [#32](https://github.com/usersy628/coffee-order-system/pull/32)
  - 구현 커밋: ecdf3c1
- 목적: 리뷰어와 후속 작업자가 Plan의 준비 상태, Records의 제출 스냅샷, GitHub의 라이브 상태를 서로 다른 것으로 읽도록 문서 구조와 PR 흐름을 분리한다.
- 요구사항 근거:
  - [AGENTS.md](../AGENTS.md)의 이슈 우선·PR 검토·명시적 병합 승인 규칙
  - [README.md](../README.md)의 구현 순서와 검증 기준
  - S11 PR 제출 뒤 저장소 문서가 IN_PROGRESS로 남고 GitHub 이슈가 자동 종료되어 발생한 상태 불일치
- 선행 작업: S11 제출 결과와 origin/dev 기준 커밋 f8d7489를 확인하고, DOC-02 이슈를 먼저 생성한다.
- 대상 파일:
  - AGENTS.md
  - .github/pull_request_template.md
  - README.md
  - docs/IMPLEMENTATION_PLAN.md
  - docs/IMPLEMENTATION_RECORDS.md
  - docs/IMPLEMENTATION_HISTORY.md
  - docs/PROJECT_STATUS.md
- 먼저 수행한 검증:
  1. S11 Plan 행과 Project Status의 정적 상태가 PR #30 병합 뒤 최신 GitHub 상태와 다름을 확인했다.
  2. 첫 제출은 Refs로 시작하고, 명시적 병합 승인 직전에만 Closes로 바뀌는 순서를 문서에 고정했다.
  3. 문서 변경 뒤 링크·상태 용어·UTF-8 리터럴 ? 손실과 공백 오류를 확인했다.
- 구현 범위:
  - Plan에는 첫 PR 제출 전 작업의 전체 상세만 두고, PR URL 뒤에는 Records로 옮기는 RECORDED 상태를 도입했다.
  - Records에는 계획 전체, 실제 구현 결과와 검증 결과를 불변으로 기록하고, 현재 issue·PR·CI·병합 상태는 기록하지 않게 했다.
  - Project Status를 현재 작업과 다음 행동만 담는 짧은 인수인계로 바꾸고 GitHub 링크를 라이브 상태의 단일 확인 경로로 명시했다.
  - AGENTS와 PR 템플릿에 Refs에서 Closes로 바꾸는 시점, UTF-8 body-file 검증, 제출 기록 작성 순서를 명시했다.
  - S11을 첫 PR_SUBMISSION 기록으로 옮겨 새 읽기 경로를 실제 예시로 검증했다.
  - README에는 제품 정책을 바꾸지 않고 구현 문서의 읽는 순서와 남은 고수준 단계만 정리했다.
- 제외 범위:
  - Java·Gradle·DB 스키마·성능 결과·S11 구현 내용 변경
  - GitHub Actions, 자동 병합, 봇 또는 외부 동기화 도입
  - DOC-02 이전 History 항목을 현재 GitHub 상태에 맞춰 일괄 재작성
- 완료 조건:
  - 새 작업이 제출되면 Plan 상세가 Records로 이동하고 RECORDED만 남는 절차가 모든 관련 문서에 일관되게 설명된다.
  - 정적 기록에 현재 PR/issue 상태, CI 실행 시간 또는 merge commit을 넣지 않고 GitHub 링크에서 확인하도록 명시된다.
  - S11 제출 기록이 계획·실제 결과·검증 근거를 보존하며 라이브 상태 표기를 포함하지 않는다.
  - PR 템플릿과 AGENTS가 첫 제출 Refs, 승인 직전 Closes, UTF-8 body-file 검증을 같은 순서로 안내한다.
- 실제 검증 결과:

| 검증 명령 또는 확인 | 결과 |
| --- | --- |
| git diff --check | 성공 |
| git diff --cached --check | 성공 |
| RECORDED, PR_SUBMISSION, Refs, Closes, 라이브 상태 검색 | 관련 문서에 의도한 규칙이 존재함을 확인 |
| gh issue view 31 --json number,state,url,title | 이슈 연결 정보 조회 성공 |
| gh pr view 32 --json number,title,body,url,state | UTF-8 본문과 한글 제목이 보존됨을 확인 |

#### 계획 대비 변경

- 없음

### [S11-01](https://github.com/usersy628/coffee-order-system/issues/8) 기능 간 MySQL 회귀, 부하 기준선과 실행계획 검증

- 기록 유형: PR_SUBMISSION (DOC-02 이관)
- 기록일: 2026-07-15
- 제출 브랜치: feature/issue-8-mysql-regression-load-baseline
- 연결:
  - 이슈: [#8](https://github.com/usersy628/coffee-order-system/issues/8)
  - PR: [#30](https://github.com/usersy628/coffee-order-system/pull/30)
  - 구현 커밋: 8e69ec2
  - 결과 기록 커밋: 1badf16
  - 제출 인수인계 커밋: a3ca5ca

#### 작업 상세

- 목적: 이미 구현된 포인트 충전·주문·멱등성·Outbox·Mock 소비자·인기 메뉴를 실제 MySQL에서 하나의 흐름으로 회귀 검증하고, 인기 메뉴 조회의 단일 인스턴스 부하 기준선과 실행계획을 재현 가능하게 기록한다.
- 요구사항 근거:
  - [README.md의 동시성 및 트랜잭션 상세 전략](../README.md#동시성-및-트랜잭션-상세-전략)
  - [README.md의 부하 대응과 확장 기준](../README.md#부하-대응과-확장-기준)
  - [README.md의 테스트 전략](../README.md#테스트-전략)
- 선행 작업: S6-01부터 S10-01까지의 구현과 테스트 기반을 사용한다.
- 측정 정책:
  - 단일 애플리케이션 인스턴스 기준으로만 측정하고, 사용자의 일반 local MySQL 3307과 서버 18080은 참고 정보로만 취급한다.
  - 별도 Docker Compose MySQL은 기본 host port 3308, local,perf 애플리케이션은 기본 18081을 사용하여 일반 local 실행과 분리한다.
  - Docker k6로 GET /api/menus/popular만 constant-arrival-rate 30 RPS, 5분 측정한다. 목표는 p95 500 ms 이하, HTTP 오류율 1% 미만, 모든 응답 계약 check 성공이다.
  - 주문·포인트·Outbox 쓰기 경로는 k6에 섞지 않고 실제 MySQL 교차 회귀 테스트로 검증한다. GET-only 측정의 Outbox PENDING queue age는 관측값일 뿐 통과 증거로 해석하지 않는다.
  - Testcontainers 성능 태그에서 최근 30일 orders 100,000건과 order_item 300,000건으로 인기 메뉴 SQL의 EXPLAIN ANALYZE를 수행한다. 실행 시간과 출력 문구는 환경에 따라 달라지므로 고정 assertion으로 쓰지 않는다.
  - 측정 결과에는 commit·시각·OS/CPU/RAM·Docker/Java/MySQL 버전·애플리케이션/DB/부하 발생기 배치·데이터셋·시나리오·지연/오류/RPS·DB/락/Outbox 관측값을 함께 기록하고 비밀값은 적지 않는다.
- 대상 파일:
  - src/test/java/com/usersy628/coffeeorder/regression/CrossFeatureMySqlRegressionIntegrationTest.java
  - src/test/java/com/usersy628/coffeeorder/performance/PopularMenuExplainAnalyzeIntegrationTest.java
  - build.gradle
  - src/main/resources/application-perf.yml
  - src/main/java/com/usersy628/coffeeorder/global/config/PerfBaselineInfoContributor.java
  - src/main/java/com/usersy628/coffeeorder/global/config/PerfDataSourceConfigurationGuard.java
  - src/test/java/com/usersy628/coffeeorder/global/config/PerfBaselineInfoContributorTest.java
  - docker-compose.performance.yml
  - scripts/performance/popular-menu.js, mysql-snapshot.sql, seed-popularity.sql, run-local-baseline.ps1
  - docs/performance/S11_BASELINE.md
  - README.md, docs/IMPLEMENTATION_PLAN.md, docs/PROJECT_STATUS.md
- 먼저 수행할 테스트 또는 검증:
  1. 기존 실제 MySQL 주문·포인트·Outbox·Mock·인기 메뉴 통합 테스트가 독립적으로 통과하는지 확인한다.
  2. 교차 회귀가 구현 전에는 존재하지 않았으므로, 포인트 충전부터 이벤트 수신·집계까지 하나의 흐름으로 보호망을 만든다.
  3. native k6가 없는 환경에서는 설치하지 않고 Docker k6를 사용하며, 별도 coffee_order_perf DB를 자동 초기화하거나 seed하지 않는지 확인한다.
- 구현 범위:
  - 동일 주문 키의 동시 재시도에서 포인트 차감·주문·Outbox·Mock 수신·인기 메뉴 집계가 각각 한 번만 반영되고, 잔액 부족 주문은 ghost data를 남기지 않는 실제 MySQL 회귀 테스트를 추가한다.
  - performance JUnit 태그와 명시 실행용 performanceTest Gradle task를 추가해 일반 test에서 대용량 fixture와 EXPLAIN ANALYZE를 분리한다.
  - 100,000/300,000 fixture의 인기 메뉴 실행계획 결과를 build report로 남기되 가변 실행 시간은 assertion하지 않는다.
  - local,perf 전용 Actuator metrics, Flyway·JDBC URL guard, 실제 JDBC identity 검증, 별도 MySQL Compose, 명시 base URL의 GET k6 script, 관측 SQL과 결과 수집 script를 둔다.
- 제외 범위:
  - migration, 인덱스 추가, Redis, read replica, 다중 인스턴스 증설과 production metrics endpoint
  - 주문·포인트 쓰기 부하와 일반 사용자 DB의 초기화 또는 seed
  - 측정 전에 근거 없이 cache·replica·pool tuning을 추가하는 일
- 완료 조건:
  - 교차 회귀가 포인트 충전, 동시 멱등 주문, PENDING Outbox, 중복 없는 Mock 수신, 인기 메뉴 수량 집계와 거절 주문의 ghost data 부재를 실제 MySQL에서 확인한다.
  - 일반 test는 성능 fixture를 실행하지 않고, 명시한 performanceTest가 100,000/300,000 fixture와 EXPLAIN ANALYZE report를 만든다.
  - k6·DB 관측·환경 수집 명령은 비밀값이나 기존 사용자 DB 변경 없이 재현 가능하며, datasource identity와 fixture count를 검증하고 GET-only Outbox 관측의 한계를 명시한다.
  - 측정 전후 비교 근거가 생기기 전에는 index·Redis·replica를 추가하지 않는다.
- 검증 명령:

    .\gradlew.bat test --tests "com.usersy628.coffeeorder.regression.CrossFeatureMySqlRegressionIntegrationTest"
    .\gradlew.bat performanceTest --no-daemon
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\performance\run-local-baseline.ps1 -Seed -RunK6
    .\gradlew.bat test --no-daemon --rerun-tasks
    .\gradlew.bat bootJar --no-daemon
    git diff --check

#### 실제 구현 결과

- 실제 MySQL 교차 회귀 테스트를 추가해 동시 멱등 주문과 포인트·주문·Outbox·Mock 수신·인기 메뉴 집계의 연결, 그리고 잔액 부족 주문의 ghost data 부재를 확인했다.
- performanceTest가 30일 주문 100,000건·항목 300,000건 fixture에서 인기 메뉴 쿼리의 EXPLAIN ANALYZE를 수행하도록 분리했다. 관찰된 계획은 order_item scan을 포함하며, 이 관찰만으로 추가 인덱스를 넣지 않았다.
- local,perf 분리 실행, 별도 MySQL Compose, datasource identity guard, Docker k6 runner와 환경·DB·Hikari 관측 수집 경로를 추가했다.
- live k6 기준선은 p95 3,074.07 ms, HTTP 오류율 51.87%, 실제 처리량 19.88 RPS, dropped iterations 2,569로 목표를 충족하지 못했다. Hikari 최대 10개가 포화되고 최대 50개가 대기한 관측값을 [S11_BASELINE.md](performance/S11_BASELINE.md)에 보존했다.

#### 계획 대비 변경

- 없음. live 기준선이 목표를 충족하지 못한 것은 측정 결과이며, 이 제출 범위에서 원인을 숨기거나 index·Redis·replica·pool tuning으로 결과를 바꾸지 않았다.

#### 실제 검증 결과

| 검증 명령 또는 확인 | 결과 |
| --- | --- |
| CrossFeatureMySqlRegressionIntegrationTest | 2개 테스트 성공 |
| performanceTest --no-daemon | 1개 성능 테스트 성공 |
| test --no-daemon --rerun-tasks | 103개 테스트 성공 |
| bootJar --no-daemon | 성공 |
| git diff --check | 성공 |
| GitHub Actions Build and test | 성공 |
