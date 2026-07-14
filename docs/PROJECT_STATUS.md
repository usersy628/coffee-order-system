# Project Status

마지막 갱신일: 2026-07-14

## 현재 단계

- 2단계 도메인 및 ERD 설계 완료
- 3단계 API 명세 작성 완료
- 4단계 동시성·트랜잭션·Outbox 전략 상세 검토 완료
- `S5-01` 기술 스택과 프로젝트 구조 승인 및 문서화 완료 (`DONE`, issue #1)
- `S5-02` Spring Boot 기본 구조와 MySQL 통합 테스트 환경 완료 (`DONE`, issue #2)
- `S5-03` 리뷰 후속 공통 MVC 오류·Flyway 재실행 검증·PR CI 기반 보완 완료 (`DONE`, issue #17)
- `S6-01` 메뉴 목록 조회 API 완료 (`DONE`, issue #3)
- `S5-04` PR 검토와 명시적 병합 승인 규칙 문서화 완료 (`DONE`, issue #20)
- `S6-02` 메뉴 UTC 시간 매핑과 README 구현 상태 정합성 보완 완료 (`DONE`, issue #22)
- `S7-01` 포인트 충전 API 구현 완료 (`DONE`, issue #4, PR 생성·검토 대기)

## 문서 기준

- 제품 요구사항, ERD, API 계약과 기술적 결정: `README.md`
- 구현 작업 ID, 선행 관계, 대상 파일과 검증 기준: `docs/IMPLEMENTATION_PLAN.md`
- 작업·Git·보안·인수인계 규칙: `AGENTS.md`
- 이 문서에는 현재 단계, 확정된 구현 기준과 다음 행동만 기록하며 위 문서의 상세 내용을 복제하지 않음

## 완료된 주요 이정표

- 프로젝트 저장소 초기화 (`24de84b`)
- ERD와 설계 근거 작성 (`eb20647`)
- `dev` 브랜치와 인수인계 흐름 구성 (`9adaec9`)
- API 명세와 기술 선택 근거 작성 (`5254e6c`)
- 포인트 멱등성·동시성 정책 보강 (`997cf9e`)
- Outbox 운영 정책 보강 (`26ed865`)
- 시간·인기 메뉴·MySQL 테스트 전략 보강 (`10c6a8d`)
- 전체 설계 신뢰성 검토 완료 (`051854b`)
- 경량 구현 작업 관리 흐름 추가 (`999f04e`)
- 튜터 피드백 기반 인덱스·Outbox 간소화, 예외 처리와 부하 확장 기준 보강 (`dd2a27c`)
- 구현 계획의 `S5-01`~`S15-01`을 GitHub issue #1~#12로 등록
- Java 17·Spring Boot 3.5.16·MySQL 8.4.10과 기능 중심 계층 구조 승인 (`fe6d65b`, issue #1), 공식 Initializr 산출물에 맞춰 Gradle patch를 8.14.5로 보정 (issue #2)
- Gradle Wrapper·공통 오류·traceId·Flyway V1/V2·MySQL 8.4.10 Testcontainers 기반 구성과 12개 테스트 통과 (issue #2)
- 공통 MVC 4xx 매핑과 계약 테스트 보강, 전체 16개 테스트 통과 (`45612f5`, issue #17)
- Flyway migration 재실행 시 추가 적용 0건과 `validate()` 성공 검증 (`0319638`, issue #17)
- Java 17·Docker·전체 테스트·`bootJar`·diff를 검증하는 PR CI 추가 (`e018bf8`, issue #17)
- 판매 중지 포함 전체 메뉴의 ID 오름차순 조회 계층 구현 (`eacde62`, issue #3)
- `GET /api/menus`의 네 필드·빈 배열·traceId 계약과 MySQL 통합 테스트 구현, 전체 20개 테스트 통과 (`c7f599b`, issue #3)
- PR 생성·CI 성공 후 별도 검토와 사용자 명시적 승인 전 자동 병합을 금지하는 workflow 확정 (`24fb9ef`, issue #20)
- 메뉴 생성·수정 시각을 UTC `Instant`로 통일하고 MySQL `DATETIME(6)` 마이크로초 조회 검증 추가 (`525d985`, issue #22)
- README의 완료된 메뉴 목록과 다음 포인트 충전 준비 상태 정합성 갱신 (`fd26552`, issue #22)
- 포인트 충전 API, 지갑·이력 원자성, 멱등 결과 재현과 충전 오류 계약 구현 (`3227271`, issue #4)
- 동일 사용자·동일 멱등 키 100개 동시 요청, 사용자별 병렬 처리, 실제 락 타임아웃·데드락·롤백 검증 (`648f225`, `6a1a688`, issue #4)
- PR #24 리뷰에 따라 503 재시도 실패의 횟수·원인 타입을 보존하고 민감정보 없이 WARN으로 기록하도록 보완 (`4c1ebbf`, issue #4)

## 확정된 구현 기준

- 기술 스택, 패키지 경계, 설정과 테스트 도구의 단일 기준은 `README.md`의 `기술 스택과 프로젝트 구조`에 기록함
- 기능별 작업은 GitHub issue를 먼저 만들고 `feature/issue-<번호>-<slug>` 브랜치와 `dev` 대상 PR로 수행함
- Flyway `V1` schema와 과제용 `V2` 초기 사용자·메뉴·0P 지갑을 실제 MySQL 8.4.10 Testcontainers에서 검증함
- 공통 오류·traceId 최소 기반과 MySQL 테스트 기반은 `S5-02`, 공통 MVC 전송 오류와 Flyway 재실행 검증은 `S5-03`에서 보강함
- 충전·주문 DTO 검증 오류는 각각 `S7-01`·`S8-01`에서 기능별 코드로 구현하고, 전체 오류 계약의 최종 회귀는 `S12-01`에서 수행함
- `dev` 대상 PR은 필수 `Build and test` check에서 Java 17·Docker 기반 전체 테스트, `bootJar`와 `git diff --check`를 통과해야 함
- PR 생성과 필수 CI 성공 후에도 자동 병합하지 않으며, 별도 검토 결과를 반영하고 사용자가 해당 PR의 병합을 명시적으로 승인한 경우에만 `dev`에 병합함
- 메뉴 목록은 MySQL primary에서 판매 상태와 관계없이 전체 메뉴를 ID 오름차순으로 조회하고, `menuId`, `name`, `price`, `status`만 반환함
- 메뉴의 `createdAt`, `updatedAt`은 UTC `Instant`로 매핑하고 MySQL `DATETIME(6)`의 마이크로초 정밀도로 조회함
- 포인트 충전은 지갑을 먼저 비관적 락으로 잠그고 이력을 재확인하며, 지갑 증가와 `CHARGE` 이력을 5초 제한의 한 트랜잭션으로 커밋함
- 충전의 같은 멱등 키·같은 금액은 최초 잔액과 시각을 재현하고, 다른 금액은 409로 거절하며, 락·데드락은 전체 명령을 최대 3회 시도한 뒤 503으로 변환함
- 충전 재시도 실패는 전용 예외에 실제 시도 횟수와 원인을 보존하고 Advice에서 오류 코드·횟수·원인 타입·traceId만 WARN으로 기록함
- RestClient·Apache HttpClient 5의 숨은 재시도 부재와 5초 전체 call deadline은 `S9-01` WireMock 실제 소켓 테스트의 합격 조건으로 검증함

현재 진행을 막는 외부 차단 사항은 없다. Flyway는 MySQL 8.4가 공식 최신 검증 범위보다 새 버전이라는 경고를 출력하지만, 실제 MySQL 8.4.10 smoke test와 migration 검증을 통과했으며 이 호환성 위험은 계속 통합 테스트로 감시한다.

## 다음 행동

issue #4 브랜치를 원격에 push하고 `dev` 대상 PR을 만든다. 필수 CI 성공 후 자동 병합하지 않고 별도 검토와 사용자의 명시적 병합 승인을 기다린다.

## 작업 재개 기준

- 기준 브랜치: `feature/issue-4-point-charge-api`
- 원격 저장소: `https://github.com/usersy628/coffee-order-system.git`
- GitHub 작업 이슈: #1~#12, #15, #17, #20, #22, 현재 issue #4, 다음 준비 issue #5
- 경량 구현 계획 기준 커밋: `999f04e docs: add lightweight implementation workflow`
- 최신 설계 기준 커밋: `dd2a27c docs: simplify design after tutor feedback`
- 승인된 기술 스택과 S5-02 준비 기준 커밋: `fe6d65b docs: approve technology stack and project structure (#1)`
- 예상 작업 트리: 문서 완료 커밋 후 clean, PR 생성과 검토 대기
- 재개 시 `AGENTS.md`의 저장소 확인 명령으로 실제 상태를 다시 검증
