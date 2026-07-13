# Project Status

마지막 갱신일: 2026-07-14

## 현재 단계

- 2단계 도메인 및 ERD 설계 완료
- 다음 단계: 3단계 API 명세 작성

## 완료된 작업

- 프로젝트 저장소 초기화 (`24de84b`)
- 요구사항 분석 및 핵심 정책 결정
- 도메인 모델, 테이블, 관계, 제약조건, 인덱스와 Outbox 선점 전략 설계
- ERD와 설계 근거를 `README.md`에 반영 (`eb20647`)
- 개발 브랜치와 GitHub 기본 브랜치를 `dev`로 설정

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

세부 정책과 전체 ERD는 `README.md`를 단일 설계 문서로 참조한다.

## 미결 사항

- API별 URI, 요청·응답 필드와 형식
- 공통 성공 응답 래퍼 사용 여부
- 오류 응답의 공통 스키마와 세부 오류 코드
- 동시 멱등 요청이 진행 중일 때의 구체적인 API 응답 정책
- 메뉴 목록의 정렬 및 페이지네이션 정책
- 인기 메뉴 응답에 포함할 메뉴 정보와 판매 수량 필드

## 다음 작업

3단계 API 명세 초안을 작성한다. 메뉴 목록 조회, 포인트 충전, 여러 메뉴 주문·결제, 최근 168시간 인기 메뉴 TOP 3의 URI, 헤더, 요청·응답, 상태 코드와 오류 코드를 표로 제시하고 미결 정책은 추천안과 함께 확인받는다. 승인 전에는 Spring Boot 코드를 구현하지 않는다.

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
- 문서화된 마지막 기능 커밋: `eb20647 docs: define domain model and ERD`
- GitHub CLI 인증 실패는 프로젝트 상태 문제가 아니며 운영체제의 CLI 인증을 별도로 복구한다.
