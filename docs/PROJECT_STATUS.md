# Project Status

마지막 갱신: 2026-07-15

## 현재 상태

- 기준 개발 브랜치: `dev`, 현재 HEAD `c2243a6` (PR #29 merge commit)
- 현재 작업 브랜치: `feature/issue-8-mysql-regression-load-baseline`
- `S9-01` Outbox 게시자와 Mock 데이터 수집 플랫폼: `DONE`, issue #6 `CLOSED`, PR #28 `MERGED`, merge commit `609cdc8`
- `S10-01` 최근 168시간 인기 메뉴 TOP 3 조회: `DONE`, issue #7 `CLOSED`, PR [#29](https://github.com/usersy628/coffee-order-system/pull/29) `MERGED`, merge commit `c2243a6`
- `S11-01` 기능 간 MySQL 회귀, 부하 기준선과 실행계획 검증: `IN_PROGRESS`, issue #8 `OPEN`, PR [#30](https://github.com/usersy628/coffee-order-system/pull/30) `OPEN`; 문서 준비 커밋 `9dc903f`, 구현 커밋 `8e69ec2`, 결과 기록 커밋 `1badf16`. 실제 MySQL 교차 회귀와 전체 30일 분포의 100,000/300,000 `EXPLAIN ANALYZE` profile을 통과했다. Testcontainers plan은 `order_item` scan을 선택했으며, 근거 없이 인덱스를 추가하지 않는다. `local,perf`·`18081`·전용 MySQL `3308` live k6 기준선도 실행했지만 p95 3,074.07 ms, 시스템 오류율 51.87%, dropped iterations 2,569로 SLO를 통과하지 못했다. Hikari 최대 10개가 포화되고 최대 50개가 대기한 실측 근거는 [`S11_BASELINE.md`](performance/S11_BASELINE.md)에 보존했다.
- 완료 상세와 실제 검증 결과: [`IMPLEMENTATION_HISTORY.md`](IMPLEMENTATION_HISTORY.md)

## 현재 결정

- Mock은 같은 애플리케이션의 `local`·`test` 전용 내부 HTTP 수신기이며 URI는 `POST /internal/mock-data-platform/events`다.
- 게시자는 원본 payload와 `Idempotency-Key: eventId`를 전송하고, V3 수신 테이블은 `event_id` 유니크·payload·`received_at`으로 중복 소비를 제거한다.
- scheduler는 기본·test에서 비활성화하고 local은 현재 `server.port`를 참조한다. 사용자 로컬 MySQL `3307`과 서버 `18080` 설정은 변경하지 않았다.
- 인기 메뉴는 한 번 고정한 마이크로초 UTC `T`의 `[T - 168시간, T)`를 `SUM(order_item.quantity)`로 직접 집계하며, API에서만 `Asia/Seoul`으로 변환한다.
- S11은 단일 인스턴스에서 Docker k6로 인기 메뉴 GET만 30 RPS·5분 측정하고 p95 500 ms 이하·시스템 오류율 1% 미만·모든 응답 계약 check 성공을 기준으로 삼는다. 결과에는 OS·CPU·RAM·Docker/Java/MySQL 버전·배치·실제 검증한 데이터셋·DB/락/Outbox·Hikari 관찰값을 함께 기록하며, 실제 사용자 DB의 초기화·seed는 자동화하지 않는다.
- S11 perf profile은 `local,perf` 순서로만 실행한다. `perf`는 `127.0.0.1:3308/coffee_order_perf`와 `PERF_MYSQL_*`만 사용하고 Outbox scheduler를 끈다. 별도 perf 앱은 기본 `18081`을 사용해 일반 local `18080`을 유지하며, runner는 실제 JDBC host·port·database identity를 확인한 뒤 healthcheck가 통과한 Compose service에만 seed한다.
- S11 live run은 기준선 **실패**로 기록한다. Hikari pool·쿼리·요청 동시성의 개선 방향은 이 결과와 비교할 별도 후속 작업에서 결정하며, 이 기준선 PR에 추정 기반의 인덱스·Redis·replica·pool tuning을 섞지 않는다.
- PR 생성과 필수 CI 성공 후에도 자동 병합하지 않는다. 별도 검토와 사용자의 해당 PR 명시적 승인 후에만 병합한다.

## 다음 행동

PR [#30](https://github.com/usersy628/coffee-order-system/pull/30)의 코드·문서·live 기준선 실패 근거를 별도 검토한다. 성능 개선 구현이나 병합은 검토와 사용자의 해당 PR 명시적 승인 전에는 진행하지 않는다.
