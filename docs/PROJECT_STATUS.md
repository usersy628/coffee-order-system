# Project Status

마지막 갱신일: 2026-07-15

## 현재 상태

- 기준 브랜치 `dev`의 확인 HEAD: `d799341` (PR #27 merge commit)
- `S7-01` 포인트 충전 API: `DONE`, issue #4 `CLOSED`, PR #24 `MERGED`
- PR #24의 손상된 한글 본문을 UTF-8 `--body-file`로 복구했고 GitHub 원본에서 한글 제목과 예상하지 않은 리터럴 `?`가 없음을 확인함
- `DOC-01` 구현 계획과 완료 이력 분리: `DONE`, issue #25 `CLOSED`, PR #26 `MERGED`, merge commit `14d8637`
- `S8-01` 주문·포인트 결제·멱등성: `DONE`, issue #5 `CLOSED`, PR #27 `MERGED`, merge commit `d799341`, 필수 `Build and test` 성공
- `S9-01` Outbox 게시자와 Mock 데이터 수집 플랫폼: issue #6 `OPEN`, Mock 수신 HTTP 경계 승인 완료, `READY`

## 문서 기준

- 제품 요구사항, ERD, API 계약과 기술적 결정: `README.md`
- 전체 상태 인덱스와 완료되지 않은 작업 상세: `docs/IMPLEMENTATION_PLAN.md`
- 완료 작업 상세, issue·PR·merge commit·완료일과 실제 검증 결과: `docs/IMPLEMENTATION_HISTORY.md`
- 작업·Git·보안·인수인계 규칙: `AGENTS.md`

## 현재 결정

- S8 네 정책은 2026-07-15 승인되어 README와 Implementation Plan에 반영했다.
- S8은 주문·차감·이력과 `PENDING` Outbox 저장까지만 완료했고 Outbox 전송과 소비는 S9에 남겼다.
- PR #27 리뷰의 주문 흐름 문서 불일치와 금액 오버플로 500 응답을 수정했다.
- 리뷰 반영 후 전체 테스트 66개와 `bootJar`가 성공했으며 주문 동시성은 동일 키 100개와 서로 다른 주문 100개로 검증했다.
- PR 생성과 필수 CI 성공 후에도 자동 병합하지 않고 별도 검토와 사용자의 명시적 승인을 기다린다.
- S9의 선점·lease·fencing·재시도·중복 소비 정책은 README에 확정돼 있다. 2026-07-15 사용자 승인으로 Mock은 같은 앱의 `local`·`test` 전용 내부 HTTP 수신기, URI는 `POST /internal/mock-data-platform/events`, 전송 헤더는 `Idempotency-Key: eventId`, 수신 저장은 V3 `event_id` 유니크·payload·`received_at`으로 확정했다.

## 다음 행동

S9 Mock 수신 endpoint의 `404` RED 테스트를 실행한 뒤 작업을 `IN_PROGRESS`로 전환한다.
