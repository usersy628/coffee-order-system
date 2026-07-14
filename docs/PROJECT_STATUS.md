# Project Status

마지막 갱신일: 2026-07-14

## 현재 단계

- 2단계 도메인 및 ERD 설계 완료
- 3단계 API 명세 작성 완료
- 4단계 동시성·트랜잭션·Outbox 전략 상세 검토 완료
- 다음 단계: 5단계 Spring Boot 프로젝트 기본 구조 구성

## 완료된 작업

- 프로젝트 저장소 초기화 (`24de84b`)
- 요구사항 분석 및 핵심 정책 결정
- 도메인 모델, 테이블, 관계, 제약조건, 인덱스와 Outbox 선점 전략 설계
- ERD와 설계 근거를 `README.md`에 반영 (`eb20647`)
- 개발 브랜치와 GitHub 기본 브랜치를 `dev`로 설정
- 프로젝트 작업 지침과 상태 인수인계 문서 추가 (`9adaec9`)
- API 공통 규칙, 요청·응답, 오류, 설계 의도와 기술 선택 근거를 `README.md`에 반영 (`5254e6c`)
- 포인트 충전 멱등성, 지갑 락 순서와 DB 재시도 정책 보강 (`997cf9e`)
- Outbox claim fencing, 상태별 재시도, redrive·보관·관측 정책 보강 (`26ed865`)
- 시간 정밀도, 인기 메뉴 SQL·인덱스와 MySQL 테스트 전략 보강 (`10c6a8d`)

## 확정된 주요 정책

- 사용자 생성 시 잔액 0P의 포인트 지갑을 같은 트랜잭션에서 사전 생성한다.
- 금액은 Java `long`, MySQL `BIGINT`로 관리한다.
- 주문 멱등성 정보는 별도 테이블 없이 `orders`에 저장한다.
- `(user_id, idempotency_key)` 유니크 제약과 정규화 요청의 SHA-256 해시를 사용한다.
- 충전 멱등성 정보는 `point_history`의 `CHARGE` 이력에 저장하고 사용자·작업 종류 단위로 키를 구분한다.
- 같은 사용자의 충전과 주문은 지갑 행을 공통 직렬화 지점으로 사용하고, 락 획득 후 멱등 결과를 current read로 재확인한다.
- 데드락과 락 timeout은 트랜잭션 밖에서 전체 명령을 최대 2회 재시도하고 소진 시 503을 반환한다.
- 주문 가격은 서버 DB에서 계산하고 주문 항목에 메뉴명, 단가, 수량과 항목 금액을 스냅샷으로 저장한다.
- 인기 메뉴는 조회 시각 `T`를 고정하고 `[T - 168시간, T)`의 결제 완료 주문을 집계한다.
- 시간은 애플리케이션에서 `Instant`, DB에서 UTC 기준 `DATETIME(6)`로 저장하며 마이크로초 정밀도로 맞춘다.
- Outbox는 `FOR UPDATE SKIP LOCKED`와 `claim_token` fencing으로 즉시 실행 가능한 worker 수만큼 선점한다.
- 외부 전송은 중복 가능한 at-least-once 시도 모델이며, 최대 시도 후 `FAILED`는 운영자 redrive로 재개한다.
- Outbox 네트워크 오류와 5xx는 최초 전송 실패 후 최대 5회 재시도한다.
- Outbox 4xx는 외부 계약에 따라 중복 성공·재시도·영구 실패로 분류하고 429는 `Retry-After`를 적용한다.
- 포인트 이력의 `balance_after`는 필수이며 `order_id`는 충전 시 `NULL`, 사용 시 필수다.
- API 기본 경로는 URL 버전이 없는 `/api`를 사용한다.
- 사용자 ID는 경로 변수로 전달하고 성공 응답은 공통 래퍼 없이 반환한다.
- API 시간은 ISO 8601 `+09:00` 형식으로 표현한다.
- 최초 주문 성공은 `201`, 동일 멱등 요청 재전송은 `200`과 `Idempotency-Replayed: true`로 응답한다.
- API 오류는 `code`, `message`, `details`, `traceId`를 반환한다.
- 인기 메뉴는 현재 `PAID` 단일 상태를 전제로 `(paid_at, id)` 인덱스와 직접 SQL 집계를 사용하고 실행계획으로 검증한다.

세부 정책과 전체 ERD는 `README.md`를 단일 설계 문서로 참조한다.

## 미결 사항

- Java와 Spring Boot 세부 버전, Gradle 설정과 의존성
- 패키지 구조와 도메인·애플리케이션·인프라 계층 경계
- Flyway 적용 여부와 초기 사용자·메뉴 데이터 구성 방식
- 트랜잭션 재시도와 새 트랜잭션 조회를 Spring proxy 경계에서 분리하는 구체적인 클래스 구조
- 외부 Mock 플랫폼 호출에 사용할 HTTP 클라이언트와 테스트 대역 방식
- README API 명세의 필드와 구현 DTO가 일치하는지에 대한 구현 단계 검증

## 다음 작업

5단계 Spring Boot 프로젝트 기본 구조를 설계한다. Java·Spring Boot·Gradle·MySQL·Flyway·Testcontainers와 HTTP 클라이언트의 추천 버전 및 선택 이유, 패키지 구조, 설정 파일과 테스트 소스 구성을 먼저 제시하고 확인받은 뒤 프로젝트 파일을 생성한다.

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
- 문서화된 마지막 기능 커밋: `10c6a8d docs: finalize API time and test strategy`
- GitHub CLI 인증 실패는 프로젝트 상태 문제가 아니며 운영체제의 CLI 인증을 별도로 복구한다.
