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
- `S5-04` PR 검토와 명시적 병합 승인 규칙 문서화 (`IN_PROGRESS`, issue #20)
- 후속 준비 작업: `S7-01` 포인트 충전 API 상세 구체화 (`BACKLOG`, issue #4)

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

## 확정된 구현 기준

- 기술 스택, 패키지 경계, 설정과 테스트 도구의 단일 기준은 `README.md`의 `기술 스택과 프로젝트 구조`에 기록함
- 기능별 작업은 GitHub issue를 먼저 만들고 `feature/issue-<번호>-<slug>` 브랜치와 `dev` 대상 PR로 수행함
- Flyway `V1` schema와 과제용 `V2` 초기 사용자·메뉴·0P 지갑을 실제 MySQL 8.4.10 Testcontainers에서 검증함
- 공통 오류·traceId 최소 기반과 MySQL 테스트 기반은 `S5-02`, 공통 MVC 전송 오류와 Flyway 재실행 검증은 `S5-03`에서 보강함
- 충전·주문 DTO 검증 오류는 각각 `S7-01`·`S8-01`에서 기능별 코드로 구현하고, 전체 오류 계약의 최종 회귀는 `S12-01`에서 수행함
- `dev` 대상 PR은 필수 `Build and test` check에서 Java 17·Docker 기반 전체 테스트, `bootJar`와 `git diff --check`를 통과해야 함
- PR 생성과 필수 CI 성공 후에도 자동 병합하지 않으며, 별도 검토 결과를 반영하고 사용자가 해당 PR의 병합을 명시적으로 승인한 경우에만 `dev`에 병합함
- 메뉴 목록은 MySQL primary에서 판매 상태와 관계없이 전체 메뉴를 ID 오름차순으로 조회하고, `menuId`, `name`, `price`, `status`만 반환함
- RestClient·Apache HttpClient 5의 숨은 재시도 부재와 5초 전체 call deadline은 `S9-01` WireMock 실제 소켓 테스트의 합격 조건으로 검증함

현재 진행을 막는 외부 차단 사항은 없다. Flyway는 MySQL 8.4가 공식 최신 검증 범위보다 새 버전이라는 경고를 출력하지만, 실제 MySQL 8.4.10 smoke test와 migration 검증을 통과했으며 이 호환성 위험은 계속 통합 테스트로 감시한다.

## 다음 행동

issue #20의 열린 PR에서 `AGENTS.md` 병합 승인 규칙을 별도 검토하고, 지적 사항이 있으면 같은 브랜치에 반영해 필수 CI를 다시 통과시킨다. 사용자가 해당 PR의 병합을 명시적으로 승인하기 전에는 병합하지 않는다. 병합 후 issue #4의 `S7-01` 작업 상세을 구체화한다.

## 작업 재개 기준

- 기준 브랜치: issue #3 병합 후 최신 `dev`
- 원격 저장소: `https://github.com/usersy628/coffee-order-system.git`
- GitHub 작업 이슈: #1~#12, #15, #17, 다음 준비 issue #4
- 경량 구현 계획 기준 커밋: `999f04e docs: add lightweight implementation workflow`
- 최신 설계 기준 커밋: `dd2a27c docs: simplify design after tutor feedback`
- 승인된 기술 스택과 S5-02 준비 기준 커밋: `fe6d65b docs: approve technology stack and project structure (#1)`
- 예상 작업 트리: issue #3 병합 직후에는 clean, `S7-01` 승인 전에는 issue #4 브랜치를 만들지 않음
- 재개 시 `AGENTS.md`의 저장소 확인 명령으로 실제 상태를 다시 검증
