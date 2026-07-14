# Project Status

마지막 갱신일: 2026-07-14

## 현재 단계

- 2단계 도메인 및 ERD 설계 완료
- 3단계 API 명세 작성 완료
- 다음 단계: 4단계 동시성·트랜잭션·Outbox 전략 상세 검토

## 완료된 작업

- 프로젝트 저장소 초기화 (`24de84b`)
- 요구사항 분석 및 핵심 정책 결정
- 도메인 모델, 테이블, 관계, 제약조건, 인덱스와 Outbox 선점 전략 설계
- ERD와 설계 근거를 `README.md`에 반영 (`eb20647`)
- 개발 브랜치와 GitHub 기본 브랜치를 `dev`로 설정
- 프로젝트 작업 지침과 상태 인수인계 문서 추가 (`9adaec9`)
- API 공통 규칙, 요청·응답, 오류, 설계 의도와 기술 선택 근거를 `README.md`에 반영 (`5254e6c`)

## 확정된 주요 정책

- 사용자 생성 시 잔액 0P의 포인트 지갑을 같은 트랜잭션에서 사전 생성한다.
- 금액은 Java `long`, MySQL `BIGINT`로 관리한다.
- 주문 멱등성 정보는 별도 테이블 없이 `orders`에 저장한다.
- `(user_id, idempotency_key)` 유니크 제약과 정규화 요청의 SHA-256 해시를 사용한다.
- 주문 가격은 서버 DB에서 계산하고 주문 항목에 메뉴명, 단가, 수량과 항목 금액을 스냅샷으로 저장한다.
- 인기 메뉴는 조회 시각 `T`를 고정하고 `[T - 168시간, T)`의 결제 완료 주문을 집계한다.
- 시간은 애플리케이션에서 `Instant`, DB에서 UTC 기준 `DATETIME(6)`로 저장한다.
- Outbox는 `FOR UPDATE SKIP LOCKED`로 선점하고 at-least-once 전달을 보장한다.
- Outbox 네트워크 오류와 5xx는 최초 전송 실패 후 최대 5회 재시도한다.
- 포인트 이력의 `balance_after`는 필수이며 `order_id`는 충전 시 `NULL`, 사용 시 필수다.
- API 기본 경로는 URL 버전이 없는 `/api`를 사용한다.
- 사용자 ID는 경로 변수로 전달하고 성공 응답은 공통 래퍼 없이 반환한다.
- API 시간은 ISO 8601 `+09:00` 형식으로 표현한다.
- 최초 주문 성공은 `201`, 동일 멱등 요청 재전송은 `200`과 `Idempotency-Replayed: true`로 응답한다.

세부 정책과 전체 ERD는 `README.md`를 단일 설계 문서로 참조한다.

## 미결 사항

- 비관적 락 획득 순서와 트랜잭션 경계의 구체적인 서비스 흐름
- 동시 멱등 요청에서 유니크 제약 충돌 후 새 트랜잭션으로 조회하는 구현 방식
- Outbox 배치 크기, 지수 백오프 간격과 lease timeout의 구체적인 값
- Outbox 4xx 실패 이벤트의 운영상 재처리 방식
- API 명세의 세부 필드명이 구현 DTO와 일치하는지에 대한 구현 단계 검증

## 다음 작업

4단계 동시성·트랜잭션·Outbox 전략을 상세 검토한다. 포인트 충전과 주문의 락 획득 순서, 트랜잭션 경계, 동시 멱등 요청 충돌 처리, Outbox 선점·전송·재시도·lease 회수 흐름을 시퀀스로 작성하고 테스트 가능한 조건을 정의한다. 승인 전에는 Spring Boot 코드를 구현하지 않는다.

## 작업 재개 확인

```powershell
git status --short --branch
git branch -vv
git remote -v
git log --oneline -5
gh auth status
```

예상 기준:

- 현재 브랜치: `dev`
- 원격 저장소: `https://github.com/usersy628/coffee-order-system.git`
- 문서화된 마지막 기능 커밋: `5254e6c docs: define API contract and design rationale`
- GitHub CLI 인증 실패는 프로젝트 상태 문제가 아니며 운영체제의 CLI 인증을 별도로 복구한다.
