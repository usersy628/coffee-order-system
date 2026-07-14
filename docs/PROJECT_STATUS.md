# Project Status

마지막 갱신: 2026-07-15

## 현재 상태

- 기준 개발 브랜치: `dev`, S9 시작 기준 HEAD `d799341` (PR #27 merge commit)
- 현재 작업 브랜치: `feature/issue-6-outbox-publisher`
- `S9-01` Outbox 게시자와 Mock 데이터 수집 플랫폼: 구현 `DONE`, 기능 커밋 `3fc13a4`, issue #6 `OPEN`, [PR #28](https://github.com/usersy628/coffee-order-system/pull/28) `OPEN`, 필수 `Build and test` 성공 (1분 52초)
- 완료 상세와 실제 검증 결과: [`IMPLEMENTATION_HISTORY.md`](IMPLEMENTATION_HISTORY.md)

## 현재 결정

- Mock은 같은 애플리케이션의 `local`·`test` 전용 내부 HTTP 수신기이며 URI는 `POST /internal/mock-data-platform/events`다.
- 게시자는 원본 payload와 `Idempotency-Key: eventId`를 전송하고, V3 수신 테이블은 `event_id` 유니크·payload·`received_at`으로 중복 소비를 제거한다.
- scheduler는 기본·test에서 비활성화하고 local은 현재 `server.port`를 참조한다. 사용자 로컬 MySQL `3307`과 서버 `18080` 설정은 변경하지 않았다.
- PR 생성과 필수 CI 성공 후에도 자동 병합하지 않는다. 별도 검토와 사용자의 해당 PR 명시적 승인 후에만 병합한다.

## 다음 행동

PR #28을 별도 검토자가 검토한다. 지적 사항이 없거나 반영 뒤 CI가 다시 성공한 경우에도, 사용자의 해당 PR 명시적 승인 전에는 병합하지 않는다.
