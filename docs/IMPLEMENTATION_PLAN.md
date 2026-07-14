# Implementation Plan

이 문서는 Coffee Order System 구현 작업의 상태 인덱스와 아직 완료되지 않은 작업의 준비·실행 상세를 관리한다. 완료된 작업의 상세와 실제 검증 결과는 [`IMPLEMENTATION_HISTORY.md`](IMPLEMENTATION_HISTORY.md)에서 보존한다.

## 문서 역할

- `README.md`: 요구사항, ERD, API 계약, 동시성·Outbox 전략과 선택 근거의 단일 기준
- `docs/IMPLEMENTATION_PLAN.md`: 전체 작업 상태 인덱스와 완료되지 않은 작업의 준비·실행 상세
- `docs/IMPLEMENTATION_HISTORY.md`: `DONE` 작업의 상세, issue·PR·merge commit·완료일과 실제 검증 결과
- `docs/PROJECT_STATUS.md`: 현재 작업과 다음 한 가지 행동을 전달하는 짧은 인수인계 문서
- `AGENTS.md`: 저장소 작업, Git, 보안과 문서 갱신 규칙

정책이 달라지면 `README.md`를 먼저 수정하고 승인받는다. 이 문서에는 정책 내용을 복사하지 않고 관련 섹션을 참조한다.

## 작업 상태

| 상태 | 의미 |
| --- | --- |
| `BACKLOG` | 순서는 정했지만 시작 조건이나 상세 경로가 아직 확정되지 않음 |
| `READY` | 선행 작업, 정확한 파일 경로, 사전 테스트 또는 검증, 완료 명령이 모두 확정됨 |
| `IN_PROGRESS` | 현재 수행 중이며 동시에 하나만 허용 |
| `BLOCKED` | 외부 결정이나 선행 작업이 없어 진행할 수 없음 |
| `DONE` | 완료 조건을 충족하고 정의된 검증을 통과했으며 상세를 History로 이동함 |

`READY`가 아닌 작업은 구현하지 않는다. 아직 존재하지 않는 소스 경로는 프로젝트 구조가 승인된 뒤 정확한 경로로 확정하고 `READY`로 변경한다.

## 작업 준비 기준

각 작업은 시작 전에 다음 내용을 모두 가져야 한다.

1. 연결되는 `README.md` 요구사항 또는 설계 섹션
2. 먼저 끝나야 하는 작업 ID
3. 생성하거나 수정할 정확한 저장소 상대 경로
4. 구현 전에 작성하거나 실행할 테스트·검증
5. 독립적으로 확인할 수 있는 완료 조건
6. 실행할 검증 명령
7. 구현 작업에 필요한 정책 또는 기술 선택의 사용자 승인

선택안 수립과 승인 획득 자체가 목적인 의사결정 작업은 승인 전에도 `READY`가 될 수 있으며, 승인을 완료 조건으로 둔다. 그 결과를 사용하는 구현 작업은 승인이 끝난 뒤에만 `READY`로 변경한다.

기능 작업은 가능한 경우 테스트를 먼저 작성하고 실패를 확인한 뒤 구현한다. 문서 작업은 링크, 형식, 모순 여부와 `git diff --check`를 사전 검증으로 사용한다.

## 전체 작업 목록

| ID | 상태 | 선행 작업 | 결과물 | 독립 완료 신호 |
| --- | --- | --- | --- | --- |
| [`DOC-01`](https://github.com/usersy628/coffee-order-system/issues/25) | `DONE` | PR #24 병합 | 활성 계획과 완료 이력 분리, 인수인계와 PR 본문 규칙 정합성 보완 | 문서 역할·링크·상태가 일치하고 PR 본문 UTF-8 검증 성공 |
| [`S5-01`](https://github.com/usersy628/coffee-order-system/issues/1) | `DONE` | 4단계 설계 완료 | 기술 스택·패키지 구조·설정 및 테스트 구성 추천안과 승인 | 선택 사항이 문서화되고 사용자가 승인함 |
| [`S5-02`](https://github.com/usersy628/coffee-order-system/issues/2) | `DONE` | `S5-01` | Spring Boot·빌드 도구 기본 구조, traceId·공통 오류 기반과 MySQL Testcontainers 환경 | 기본 컨텍스트·공통 예외 smoke test·MySQL smoke 테스트와 빌드 성공 |
| [`S5-03`](https://github.com/usersy628/coffee-order-system/issues/17) | `DONE` | `S5-02` | 리뷰 후속 공통 MVC 오류·Flyway 재실행 검증·PR CI 기반 보완 | 4xx 계약·migration 재실행·GitHub Actions 검증 성공 |
| [`S5-04`](https://github.com/usersy628/coffee-order-system/issues/20) | `DONE` | `S5-03` | PR 검토와 명시적 병합 승인 workflow 문서화 | PR·CI 후 검토 대기와 사용자 승인 전 병합 금지가 명시됨 |
| [`S6-01`](https://github.com/usersy628/coffee-order-system/issues/3) | `DONE` | `S5-03` | 메뉴 목록 조회 API와 테스트 | 메뉴 목록 계약·통합 테스트 성공 |
| [`S6-02`](https://github.com/usersy628/coffee-order-system/issues/22) | `DONE` | `S6-01`, `S5-04` | 메뉴 UTC 시간 매핑과 README 구현 상태 정합성 보완 | MySQL `DATETIME(6)`·`Instant` 정밀도 테스트와 문서 정합성 검증 성공 |
| [`S7-01`](https://github.com/usersy628/coffee-order-system/issues/4) | `DONE` | `S5-03` | 포인트 충전·이력·멱등성·동시성과 충전 요청 검증 오류 처리 | 실제 MySQL 단일·중복·경합 충전과 `INVALID_CHARGE_AMOUNT` 계약 테스트 성공 |
| [`S8-01`](https://github.com/usersy628/coffee-order-system/issues/5) | `DONE` | `S6-01`, `S7-01` | 여러 메뉴 주문·결제·멱등성, 트랜잭션 내 Outbox 저장과 주문 요청 검증 오류 처리 | 실제 MySQL 원자성·중복 요청·동시 주문과 `INVALID_ORDER_REQUEST` 계약 테스트 성공 |
| [`S9-01`](https://github.com/usersy628/coffee-order-system/issues/6) | `DONE` | `S8-01` | Outbox 게시자와 Mock 데이터 수집 플랫폼 | V3 수신 저장, claim·lease·fencing·고정 6회 재시도, 실제 HTTP 취소·종료 안전성과 중복 제거 검증 성공; PR 재검토 대기 |
| [`S10-01`](https://github.com/usersy628/coffee-order-system/issues/7) | `BACKLOG` | `S8-01` | 최근 168시간 인기 메뉴 TOP 3 조회 | 실제 MySQL 기간 경계·수량·동률 정렬 테스트 성공 |
| [`S11-01`](https://github.com/usersy628/coffee-order-system/issues/8) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01` | 기능 간 동시성·회귀, k6 부하 기준선과 인기 메뉴 `EXPLAIN ANALYZE` 검증 | p95·오류율·DB·락·Outbox 지표와 인덱스·확장 판단 근거 기록 |
| [`S12-01`](https://github.com/usersy628/coffee-order-system/issues/9) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01`, `S11-01` | 전역 예외 매핑·traceId·로그와 API 계약 정합성 최종 보강 | 검증·도메인·동시성·예상외 500 응답과 헤더 계약 전체 테스트 성공 |
| [`S13-01`](https://github.com/usersy628/coffee-order-system/issues/10) | `BACKLOG` | `S12-01` | README 실행 방법과 구현 근거 보강 | 새 환경에서 문서만으로 실행·테스트 가능 |
| [`S14-01`](https://github.com/usersy628/coffee-order-system/issues/11) | `BACKLOG` | `S6-01`, `S7-01`, `S8-01`, `S9-01`, `S10-01`, `S11-01`, `S12-01` | 구현 중 수시 기록한 내용을 정리한 TIL 트러블슈팅 문서 | 문제·원인·해결·검증 근거가 기록됨 |
| [`S15-01`](https://github.com/usersy628/coffee-order-system/issues/12) | `BACKLOG` | `S13-01`, `S14-01` | 전체 테스트·보안정보·공개 저장소 제출 검증 | 깨끗한 clone 기준 빌드와 전체 테스트 성공 |

`S9-01`과 `S10-01`은 모두 `S8-01`만 직접 선행하므로 서로 독립적으로 진행할 수 있다. MySQL Testcontainers 기반은 `S5-02`에서 만들고 `S5-03`에서 migration 재실행 검증을 보강한 뒤 각 기능 단계에서 사용하며, `S11-01`에서는 도입이 아니라 기능 간 최종 회귀와 부하·실행계획을 검증한다. 공통 MVC 전송 오류는 `S5-03`, 충전과 주문의 `MethodArgumentNotValidException`은 각각 `S7-01`과 `S8-01`에서 기능별 오류 코드로 구현하고, `S12-01`에서는 전체 API 오류·traceId·로그 계약의 최종 회귀와 누락을 점검한다.

## 진행 중인 작업 상세

현재 `IN_PROGRESS` 작업은 없다. `S9-01`의 완료 상세와 실제 검증 결과는 [`IMPLEMENTATION_HISTORY.md`](IMPLEMENTATION_HISTORY.md)에 보존한다.

## 작업 상세 템플릿

다음 작업을 `READY`로 변경할 때 아래 형식을 복사해 구체화한다.

```markdown
### `[작업 ID]` 작업명

- 상태: `READY`
- 목적:
- 요구사항 근거:
- 선행 작업:
- 대상 파일:
  - `정확한/저장소/상대/경로`
- 먼저 수행할 테스트 또는 검증:
- 구현 범위:
- 제외 범위:
- 완료 조건:
- 검증 명령:
```

## 경량 적용 범위

- `.specify/`, `specs/`와 Spec Kit 명령 파일은 추가하지 않는다.
- 번호 기반 기능 브랜치를 자동 생성하지 않고 현재 `dev` 작업 흐름을 유지한다.
- README의 요구사항과 설계를 작업 문서에 중복 저장하지 않는다.
- 작업 목록 자동 생성 대신, 단계가 `READY`가 될 때 필요한 정보만 구체화한다.
- 프로젝트 규모나 협업 인원이 커져 추적성 이점이 유지 비용보다 커질 때 전면 도입을 다시 검토한다.
