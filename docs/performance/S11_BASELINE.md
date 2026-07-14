# S11 인기 메뉴 부하 기준선

상태: `LIVE_K6_FAILED_BASELINE` — 2026-07-14T23:04:26Z부터 23:10:08Z까지 분리한 `local,perf` 환경에서 live k6 기준선을 실행했다. fixture·DB 관찰 수집은 완료됐지만 p95·오류율·dropped iterations 기준을 모두 넘었으므로, 이 결과는 성능 통과가 아니라 후속 개선 전의 재현 가능한 실패 기준선이다.

## 범위와 판정 기준

- 대상: 단일 애플리케이션 인스턴스의 `GET /api/menus/popular`.
- k6 시나리오: constant-arrival-rate `30 RPS`, `5분`.
- 기준: HTTP p95 `< 500 ms`, `http_req_failed < 1%`, `dropped_iterations == 0`, 모든 응답 계약 check 성공.
- 실행계획: MySQL Testcontainers에서 최근 30일 `orders` 100,000건·`order_item` 300,000건·메뉴 100개를 넣고 `EXPLAIN ANALYZE`를 기록한다. 절대 실행 시간은 CI assertion이 아니다.
- 제외: 주문·포인트·Outbox 쓰기 부하, 캐시·Redis·read replica·새 인덱스·다중 인스턴스 성능 주장.

인기 메뉴 GET은 Outbox 이벤트를 만들지 않는다. 따라서 이 run에서 `PENDING`이 0건이면 queue age는 `N/A (write path not exercised)`로 기록하며, `5초 이하를 검증했다`고 쓰지 않는다. Outbox의 원자성·중복 제거·전달은 `CrossFeatureMySqlRegressionIntegrationTest`가 담당한다.

## 안전한 로컬 구성

- 기존 local DB `3307`과 그 데이터는 사용·seed·초기화하지 않는다.
- perf profile은 `127.0.0.1:${PERF_MYSQL_PORT:3308}/coffee_order_perf`만 사용한다. Flyway·datasource 초기화 전에 최종 JDBC URL을 확인하고, 앱 시작 시 실제 JDBC 연결의 host·port·database를 다시 확인한다. runner도 `/actuator/info`에서 같은 실제 연결 식별값을 대조한다. DB 이름은 코드와 seed script에 고정되어 있어 일반 local DB로 조용히 fallback하지 않는다.
- [`docker-compose.performance.yml`](../../docker-compose.performance.yml)은 독립 project `coffee-order-system-perf`, service `mysql-perf`, loopback 전용 기본 port `3308`, 독립 volume을 사용한다. 기본 Compose 파일과 함께 실행하지 않는다.
- MySQL 계정·비밀번호는 저장소에 기록하지 않는다. `PERF_MYSQL_USER`, `PERF_MYSQL_PASSWORD`, `PERF_MYSQL_ROOT_PASSWORD`를 현재 PowerShell 프로세스와 IntelliJ 실행 구성에 각각 설정한다.
- 앱은 반드시 `SPRING_PROFILES_ACTIVE=local,perf` 순서로 시작한다. 마지막 `perf` profile이 Outbox scheduler를 끄고 `health,info,metrics`만 local 측정용으로 연다. perf 앱은 기본 `18081`을 사용해 기존 `18080` 일반 local 앱을 건드리지 않는다. runner는 `/actuator/info`의 `s11.baseline-profile=perf`, 실제 `host=127.0.0.1`, `port=3308`, `database=coffee_order_perf`를 확인하지 못하면 seed 전에 중단한다.
- perf DB를 지우는 `docker compose ... down -v`는 runner가 실행하지 않는다. 새로 시작할 필요가 있을 때만 사용자가 명시적으로 수행한다.

## 실행 순서

1. PowerShell에는 perf DB 생성용 root 값까지, IntelliJ Run Configuration에는 앱 접속용 user·password·port를 설정한다. 값 자체는 화면 공유·문서·커밋에 남기지 않는다.

   ```powershell
   $env:PERF_MYSQL_USER = '<perf-db-user>'
   $env:PERF_MYSQL_PASSWORD = '<perf-db-password>'
   $env:PERF_MYSQL_ROOT_PASSWORD = '<perf-db-root-password>'
   $env:PERF_MYSQL_PORT = '3308' # 선택 사항; 기본값
   ```

2. 먼저 독립 perf MySQL만 healthcheck가 통과할 때까지 시작한다. 이 명령은 기본 Compose 파일을 합치지 않으며, runner도 일반 local 앱을 발견하면 seed 전에 중단한다.

   ```powershell
   docker compose -p coffee-order-system-perf -f docker-compose.performance.yml up -d --wait mysql-perf
   ```

3. IntelliJ에서 기존 설정을 바꾸지 않고 새 `local,perf` Run Configuration을 만든다. perf 앱은 `18081`에서 실행하므로 기존 `18080` 일반 local 앱을 종료하거나 변경하지 않는다.

   ```text
   SPRING_PROFILES_ACTIVE=local,perf
   PERF_MYSQL_USER=<same user>
   PERF_MYSQL_PASSWORD=<same password>
   PERF_MYSQL_PORT=3308
   PERF_SERVER_PORT=18081
   ```

4. IntelliJ에서 `local,perf` 앱이 `/actuator/health`의 `UP`과 `/actuator/info`의 실제 `s11` datasource identity를 반환할 때까지 기다린 뒤 runner를 실행한다. runner는 marker가 없거나 연결 대상이 다른 앱에는 perf DB를 만들거나 seed하지 않으며, `ApplicationBaseUrl`과 `K6BaseUrl`의 scheme·port가 다르면 부하가 다른 앱을 향할 수 있으므로 중단한다. `-RunK6`는 직전에 fixture를 재생성·검증하도록 `-Seed`를 함께 요구한다.

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\performance\run-local-baseline.ps1 `
     -ApplicationBaseUrl http://localhost:18081 `
     -K6BaseUrl http://host.docker.internal:18081 `
     -Seed -RunK6
   ```

   Windows Docker의 k6 컨테이너에서는 `localhost`가 컨테이너 자신을 뜻하므로 `host.docker.internal`을 사용한다. `-ExecutionPolicy Bypass`는 이 한 프로세스에만 적용되며 시스템 실행 정책을 바꾸지 않는다. runner는 k6 image `grafana/k6:0.54.0`의 실제 digest를 결과에 기록한다.

5. 별도로 실행계획 profile을 돌린다.

   ```powershell
   .\gradlew.bat performanceTest --no-daemon --rerun-tasks
   ```

   결과는 `build/reports/performance/popular-menu-explain-analyze.txt`에 생성된다. 일반 `test`는 `performance` tag를 제외하므로 대량 fixture를 실행하지 않는다.

## 산출물과 관찰값

runner 출력은 `build/performance/s11/<UTC timestamp>/`에만 쓴다.

- `environment.json`: commit, 작업 트리 상태, Windows·CPU·논리 코어·RAM, Docker·Java, 단일 인스턴스·profile·port·network path, seed 직후 실제 검증한 데이터셋, 시작 직전 Hikari snapshot.
- `seed-output.txt`: perf DB 100,000 orders·300,000 order items 생성 확인.
- `mysql-before.txt`, `mysql-after.txt`: 검증한 S11 fixture 행 수, MySQL connection/lock/deadlock counters, Outbox 상태와 가장 오래된 PENDING age.
- `hikari-samples.json`, `hikari-after.json`: k6 동안 5초 간격으로 수집한 Hikari active·pending·max와 종료 직후 snapshot. `environment.json`에는 시작 직전 snapshot이 들어 있다.
- `k6-summary.json`: p50(`med`)/p95/p99, 오류율, 실제 RPS, dropped iterations.
- `k6-image-digest.txt`: 실행한 image digest.

`mysql-snapshot.sql`은 읽기 전용이다. k6는 조회만 수행하고 fixture의 direct insert는 `coffee_order_perf`에만 존재한다.

## Testcontainers 실행계획 사전 검증

2026-07-15에 아래 명령을 통과했다. 이것은 live k6 SLO 결과가 아니라, 같은 쿼리와 30일 fixture가 실제 MySQL에서 실행되는지 확인하는 사전 검증이다.

```powershell
.\gradlew.bat performanceTest --no-daemon --rerun-tasks
```

- report: `build/reports/performance/popular-menu-explain-analyze.txt`
- fixture: 최근 168시간 안 70,000건, 그 전 23일 30,000건, 총 주문 100,000건·항목 300,000건·메뉴 100개. 테스트가 두 구간의 행 수를 각각 assertion한다.
- 현재 Testcontainers report는 `order_item` 전체 scan → `orders` primary-key lookup → `menu` primary-key lookup 접근을 보인다. `orders(paid_at)`과 `order_item(order_id, menu_id)` 인덱스의 존재는 별도로 assertion하지만, 옵티마이저가 언제나 그 순서로 실행한다고 고정하지 않는다.
- 이 한 번의 관찰만으로 인덱스를 추가하지 않는다. live k6 결과와 같은 환경의 비교 측정이 쌓인 뒤에만 원인을 분석한다.

## 실행 결과 기록

아래는 실제 run 결과다. 다른 CPU·Docker 자원 제한·데이터 크기·동시 실행 프로세스의 결과는 숫자만으로 직접 비교하지 않는다.

| 항목 | 실제 값 |
| --- | --- |
| commit / 측정 UTC 시각 / 작업 트리 상태 | `8e69ec2c2e0f7531b3abb4b3786497edb79f3c35` / `2026-07-14T23:04:26Z`~`23:10:08Z` / clean |
| OS / CPU 모델 / 물리·논리 코어 / RAM | Windows 11 Home `10.0.26200` / AMD Ryzen 7 7800X3D 8-Core Processor / 8·16 / 31.71 GiB |
| Docker Engine / Docker CPU·메모리 할당 / k6 image digest | 29.4.3 / 16 CPU·15.48 GiB / `grafana/k6@sha256:1f40432b1cbe7234e977f96c362c9bc550a2d2b583d014dd8669fe40d3e9e755` |
| Java·JVM 옵션 / MySQL 버전 / 앱·DB·k6 배치 | Java 17.0.12 LTS (이 run에서는 JVM argument를 별도 수집하지 않음) / MySQL 8.4.10 / Windows 단일 앱, Docker Compose MySQL, Docker k6 |
| active profiles / 앱 instance 수 / app·DB port / network path | `local,perf` / 1 / `18081`·`3308` / `host.docker.internal:18081` |
| 데이터셋 (orders / items / menus / 기간) | 100,000 / 300,000 / 100 / 30일, `2026-07-14T23:04:37Z` 검증 |
| scenario (30 RPS / 5분) | constant-arrival-rate 30 RPS, 5분, 10~60 VU; 실제 최대 60 VU |
| p50(`med`) / p95 / p99 / `http_req_failed` / actual RPS / dropped iterations | 2,045.95 ms / 3,074.07 ms / 3,166.57 ms / 51.87% / 19.88 RPS / 2,569 |
| Hikari 시작·5초 sample 최대·종료 값 / MySQL connection·lock·deadlock | 시작 `active=0,pending=0,max=10`; 65개 sample 최대 `active=10,pending=50,max=10`; 종료 `active=0,pending=0,max=10`; MySQL 전·후 `Threads_connected=11`, `Threads_running=2`, max-connection error·row lock wait·deadlock 모두 0 |
| Outbox PENDING count / oldest age | 전·후 0 / `N/A (write path not exercised)` |
| `EXPLAIN ANALYZE` report path / 관찰한 접근 경로 | `build/reports/performance/popular-menu-explain-analyze.txt` / `order_item` 전체 scan → `orders` primary-key lookup → `menu` primary-key lookup |
| 결론 | **실패 기준선**. p95는 목표 500 ms의 약 6.15배이고 오류율·dropped iterations도 기준을 초과했다. 앱 로그는 Hikari의 `max=10` 포화와 최대 50개 대기, 약 2초 connection-acquire timeout을 기록했다. 이 PR에서는 인덱스·Redis·replica·pool tuning을 추가하지 않으며, 후속 성능 개선은 이 기준선과 비교하는 별도 범위로 결정한다. |
