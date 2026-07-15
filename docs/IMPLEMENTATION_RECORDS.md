# Implementation Records

이 문서는 DOC-02부터 PR에 제출한 작업의 불변 제출 기록을 보존한다. 각 기록은 제출 당시의 계획, 실제 구현 결과, 계획 대비 변경과 검증 근거를 담는다. issue·PR·CI·병합의 현재 상태는 기록의 링크를 따라 GitHub에서 확인한다.

## 읽는 방법

- PR_SUBMISSION은 PR에 제출한 변경의 정적 스냅샷이며, 검토·병합·CI의 현재 상태를 뜻하지 않는다.
- 기록에는 issue와 PR 링크, 구현 커밋, 실행한 검증 명령처럼 바뀌지 않는 근거만 적는다.
- 기록을 만든 뒤 GitHub의 상태가 바뀌어도 이 문서를 맞추기 위해 수정하지 않는다. 라이브 상태는 GitHub 링크가 단일 기준이다.
- 새 PR은 Plan의 전체 상세를 본문에 넣어 제출하고, PR URL이 생긴 뒤 같은 브랜치의 최종 문서 커밋에서 그 상세를 이 문서로 옮긴다.

## PR 제출 기록

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
