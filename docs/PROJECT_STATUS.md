# Project Status

마지막 갱신: 2026-07-15

## 현재 상태

- 기준 개발 브랜치: `dev`, 현재 HEAD `609cdc8` (PR #28 merge commit)
- 현재 작업 브랜치: `feature/issue-7-popular-menu-top3`
- `S9-01` Outbox 게시자와 Mock 데이터 수집 플랫폼: `DONE`, issue #6 `CLOSED`, PR #28 `MERGED`, merge commit `609cdc8`
- `S10-01` 최근 168시간 인기 메뉴 TOP 3 조회: 구현 `DONE`, 기능 커밋 `5724012`, issue #7 `OPEN`, PR [#29](https://github.com/usersy628/coffee-order-system/pull/29) `OPEN`, GitHub Actions CI 성공, 별도 검토 완료, 사용자 병합 승인 대기
- 완료 상세와 실제 검증 결과: [`IMPLEMENTATION_HISTORY.md`](IMPLEMENTATION_HISTORY.md)

## 현재 결정

- Mock은 같은 애플리케이션의 `local`·`test` 전용 내부 HTTP 수신기이며 URI는 `POST /internal/mock-data-platform/events`다.
- 게시자는 원본 payload와 `Idempotency-Key: eventId`를 전송하고, V3 수신 테이블은 `event_id` 유니크·payload·`received_at`으로 중복 소비를 제거한다.
- scheduler는 기본·test에서 비활성화하고 local은 현재 `server.port`를 참조한다. 사용자 로컬 MySQL `3307`과 서버 `18080` 설정은 변경하지 않았다.
- 인기 메뉴는 한 번 고정한 마이크로초 UTC `T`의 `[T - 168시간, T)`를 `SUM(order_item.quantity)`로 직접 집계하며, API에서만 `Asia/Seoul`으로 변환한다.
- PR 생성과 필수 CI 성공 후에도 자동 병합하지 않는다. 별도 검토와 사용자의 해당 PR 명시적 승인 후에만 병합한다.

## 다음 행동

PR #29의 별도 검토 결과를 확인하고, 사용자의 명시적 병합 승인을 기다린다. 승인 전에는 병합하지 않는다.
