# Implementation Plan

이 문서는 Coffee Order System 작업의 상태 인덱스와 첫 PR 제출 전 준비·실행 상세를 관리한다. PR 제출 단위의 상세와 실제 검증 결과는 [IMPLEMENTATION_RECORDS.md](IMPLEMENTATION_RECORDS.md)에 불변 기록으로 보존한다.

## 문서 역할

- README.md: 요구사항, ERD, API 계약, 동시성·Outbox 전략과 선택 근거의 단일 기준
- docs/IMPLEMENTATION_PLAN.md: 전체 작업 상태 인덱스와 아직 첫 PR을 제출하지 않은 작업의 준비·실행 상세
- docs/IMPLEMENTATION_RECORDS.md: PR 제출 당시의 전체 작업 상세, 실제 구현 결과, 계획 대비 변경과 검증 결과를 보존하는 불변 기록
- docs/IMPLEMENTATION_HISTORY.md: DOC-02 이전 완료 이력을 보존하는 legacy archive
- docs/PROJECT_STATUS.md: 현재 작업과 다음 한 가지 행동만 전달하는 짧은 인수인계
- AGENTS.md: 저장소 작업, Git, 보안과 문서 갱신 규칙

요구사항·설계 정책이 달라지면 README.md를 먼저 수정하고 승인받는다. 이 문서와 제출 기록에는 정책 내용을 복사하지 않고 관련 섹션을 참조한다. issue·PR·CI·병합의 현재 상태는 반드시 링크한 GitHub 화면에서 확인한다.

## 작업 상태

| 상태 | 의미 |
| --- | --- |
| BACKLOG | 순서는 정했지만 시작 조건이나 상세 경로가 아직 확정되지 않음 |
| READY | 선행 작업, 정확한 파일 경로, 사전 테스트 또는 검증, 완료 명령이 모두 확정됨 |
| IN_PROGRESS | 현재 수행 중이며 동시에 하나만 허용 |
| BLOCKED | 외부 결정이나 선행 작업이 없어 진행할 수 없음 |
| RECORDED | PR 제출 기록이 존재함. 검토·병합·CI의 현재 상태를 뜻하지 않으므로 후속 작업 전에는 연결한 GitHub PR을 확인 |
| DONE | DOC-02 이전 legacy 이력의 완료 표기. 새 작업에는 사용하지 않음 |

READY가 아닌 작업은 구현하지 않는다. RECORDED는 완료 판정이 아니라 상세가 Plan에서 Records로 이동했음을 뜻한다. 첫 PR을 만들기 전에는 Plan에서 전체 상세를 관리하고, PR URL이 생긴 같은 브랜치의 최종 문서 커밋에서 Records로 옮긴다.

## 작업 준비 기준

각 작업은 시작 전에 다음 내용을 모두 가져야 한다.

1. 연결하는 README.md 요구사항 또는 설계 섹션
2. 먼저 끝나야 하는 선행 작업 ID
3. 생성하거나 수정할 정확한 저장소 상대 경로
4. 구현 전에 작성하거나 수행할 테스트 또는 검증
5. 독립적으로 확인 가능한 완료 조건
6. 실행할 검증 명령
7. 구현 작업에 필요한 정책 또는 기술 선택의 사용자 승인

선택의 결론과 사용자 승인 자체가 목적인 의사결정 작업은 승인 전에도 READY가 될 수 있으며, 승인을 완료 조건으로 둔다. 그 결과를 사용하는 구현 작업은 승인이 끝난 뒤에만 READY로 바꾼다.

기능 작업은 가능한 경우 테스트를 먼저 작성하고 실패를 확인한 뒤 구현한다. 문서 작업은 링크, 상태 정의, 형식, 모순 여부와 git diff --check를 사전 검증으로 사용한다.

## 전체 작업 목록

| ID | 상태 | 선행 작업 | 결과물 | 독립 완료 신호 |
| --- | --- | --- | --- | --- |
| [DOC-01](https://github.com/usersy628/coffee-order-system/issues/25) | DONE | PR #24 병합 | 활성 계획과 완료 이력 분리, 인수인계와 PR 본문 규칙 정합성 보완 | 문서 역할·링크·상태가 일치하고 PR 본문 UTF-8 검증 성공 |
| [DOC-02](https://github.com/usersy628/coffee-order-system/issues/31) | RECORDED | S11 제출 결과 확인 | PR lifecycle과 문서 상태 기준 분리, 불변 제출 기록 도입 | [IMPLEMENTATION_RECORDS.md](IMPLEMENTATION_RECORDS.md)의 제출 기록과 연결 PR에서 검증 |
| [S5-01](https://github.com/usersy628/coffee-order-system/issues/1) | DONE | 4단계 설계 완료 | 기술 스택·패키지 구조·설정 및 테스트 구성 추천안과 승인 | 선택 사항이 문서화되고 사용자가 승인함 |
| [S5-02](https://github.com/usersy628/coffee-order-system/issues/2) | DONE | S5-01 | Spring Boot·빌드 도구 기본 구조, traceId·공통 오류 기반과 MySQL Testcontainers 환경 | 기본 컨텍스트·공통 예외 smoke test·MySQL smoke 테스트와 빌드 성공 |
| [S5-03](https://github.com/usersy628/coffee-order-system/issues/17) | DONE | S5-02 | 리뷰 후속 공통 MVC 오류·Flyway 재실행 검증·PR CI 기반 보완 | 4xx 계약·migration 재실행·GitHub Actions 검증 성공 |
| [S5-04](https://github.com/usersy628/coffee-order-system/issues/20) | DONE | S5-03 | PR 검토와 명시적 병합 승인 workflow 문서화 | PR·CI 후 검토 대기와 사용자 승인 전 병합 금지가 명시됨 |
| [S6-01](https://github.com/usersy628/coffee-order-system/issues/3) | DONE | S5-03 | 메뉴 목록 조회 API와 테스트 | 메뉴 목록 계약·통합 테스트 성공 |
| [S6-02](https://github.com/usersy628/coffee-order-system/issues/22) | DONE | S6-01, S5-04 | 메뉴 UTC 시간 매핑과 README 구현 상태 정합성 보완 | MySQL DATETIME(6)·Instant 정밀도 테스트와 문서 정합성 검증 성공 |
| [S7-01](https://github.com/usersy628/coffee-order-system/issues/4) | DONE | S5-03 | 포인트 충전·이력·멱등성·동시성과 충전 요청 검증 오류 처리 | 실제 MySQL 단일·중복·경합 충전과 INVALID_CHARGE_AMOUNT 계약 테스트 성공 |
| [S8-01](https://github.com/usersy628/coffee-order-system/issues/5) | DONE | S6-01, S7-01 | 여러 메뉴 주문·결제·멱등성, 트랜잭션 내 Outbox 저장과 주문 요청 검증 오류 처리 | 실제 MySQL 원자성·중복 요청·동시 주문과 INVALID_ORDER_REQUEST 계약 테스트 성공 |
| [S9-01](https://github.com/usersy628/coffee-order-system/issues/6) | DONE | S8-01 | Outbox 게시자와 Mock 데이터 수집 플랫폼 | V3 수신 저장, claim·lease·fencing·고정 6회 재시도, 실제 HTTP 취소·종료 안전성과 중복 제거 검증 성공 |
| [S10-01](https://github.com/usersy628/coffee-order-system/issues/7) | DONE | S8-01 | 최근 168시간 인기 메뉴 TOP 3 조회 | 실제 MySQL 기간 경계·수량 합계·동률 정렬·빈 결과·UTC/KST 시간 경계 테스트 성공 |
| [S11-01](https://github.com/usersy628/coffee-order-system/issues/8) | RECORDED | S6-01, S7-01, S8-01, S9-01, S10-01 | 기능 간 동시성·회귀, k6 부하 기준선과 인기 메뉴 EXPLAIN ANALYZE 검증 | [IMPLEMENTATION_RECORDS.md](IMPLEMENTATION_RECORDS.md)의 제출 기록과 연결 PR에서 검증 |
| [S12-01](https://github.com/usersy628/coffee-order-system/issues/9) | RECORDED | S6-01, S7-01, S8-01, S9-01, S10-01, S11-01 | 전역 예외 매핑·traceId·로그와 API 계약 정합성 최종 보강 | [IMPLEMENTATION_RECORDS.md](IMPLEMENTATION_RECORDS.md)의 제출 기록과 연결 PR에서 검증 |
| [S13-01](https://github.com/usersy628/coffee-order-system/issues/10) | RECORDED | S12-01 | README 실행 방법과 구현 근거 보강 | [IMPLEMENTATION_RECORDS.md](IMPLEMENTATION_RECORDS.md)의 제출 기록과 연결 PR에서 검증 |
| [S14-01](https://github.com/usersy628/coffee-order-system/issues/11) | READY | S6-01, S7-01, S8-01, S9-01, S10-01, S11-01, S12-01 | 구현 중 수시 기록한 내용을 정리한 TIL 트러블슈팅 문서 | 문제·원인·해결·검증 근거가 기록됨 |
| [S15-01](https://github.com/usersy628/coffee-order-system/issues/12) | BACKLOG | S13-01, S14-01 | 전체 테스트·보안정보·공개 저장소 제출 검증 | 깨끗한 clone 기준 빌드와 전체 테스트 성공 |

S9-01과 S10-01은 모두 S8-01만 직접 선행하므로 서로 독립적으로 진행할 수 있다. MySQL Testcontainers 기반은 S5-02에서 만들고 S5-03에서 migration 재실행 검증을 보강한 뒤 각 기능 단계에서 사용한다. S11-01의 상세는 제출 기록으로 이동했으므로, S12-01을 시작하기 전에는 정적 기록과 연결한 GitHub PR을 함께 확인한다.

## 준비·진행 중인 작업 상세

현재 IN_PROGRESS 작업은 없다. S14-01은 READY이며, DOC-02, S11-01, S12-01과 S13-01의 계획·구현·검증 상세는 [IMPLEMENTATION_RECORDS.md](IMPLEMENTATION_RECORDS.md)에 보존한다. 다음 기능 작업을 시작하기 전에는 연결한 GitHub PR의 라이브 상태를 확인한다.

### [S14-01](https://github.com/usersy628/coffee-order-system/issues/11) 구현 과정의 TIL 트러블슈팅을 정리한다

- 상태: READY
- 목적: 이미 검증된 동시성·멱등성·트랜잭션·Outbox·실행계획·부하 기준선 사례를 초보자도 재현 경로와 한계까지 이해할 수 있는 학습 기록으로 정리한다.
- 요구사항 근거:
  - [README.md의 동시성 및 트랜잭션 상세 전략](../README.md#동시성-및-트랜잭션-상세-전략)
  - [README.md의 Outbox 상태 전이와 fencing](../README.md#outbox-상태-전이와-fencing)
  - [README.md의 부하 대응과 확장 기준](../README.md#부하-대응과-확장-기준)
  - [README.md의 테스트 전략](../README.md#테스트-전략)
  - [S11 기준선](performance/S11_BASELINE.md)
  - [issue #11](https://github.com/usersy628/coffee-order-system/issues/11)
- 선행 작업: S6-01부터 S12-01까지의 구현·검증 기록이 있고, S13-01 PR #34가 `dev`에 병합된 최신 `origin/dev`를 기준으로 한다.
- 작업 브랜치: feature/issue-11-til-troubleshooting
- 대상 파일:
  - docs/TIL_TROUBLESHOOTING.md
  - README.md
  - docs/IMPLEMENTATION_PLAN.md
  - docs/PROJECT_STATUS.md
- 먼저 수행할 테스트 또는 검증:
  1. 제출 기록, legacy history, S11 기준선, 실제 통합 테스트 경로를 대조해 각 사례에 재현 조건·관찰 결과·검증 명령이 모두 있는지 확인한다.
  2. S11의 실패 기준선 수치와 Hikari 관찰값을 확인하되, 단일 측정만으로 인과관계나 최적화 효과를 단정하지 않는지 검토한다.
  3. 새 문서의 상대 링크, 용어와 코드·테스트 경로가 최신 `dev`에서 실제로 존재하고 비밀값·개인 환경 값이 없는지 확인한다.
- 구현 범위:
  - `docs/TIL_TROUBLESHOOTING.md`에 다음 다섯 사례를 같은 형식(문제·재현 조건·관찰·원인·검토 대안·선택·검증 근거·한계)으로 작성한다.
    1. 지갑 락 뒤 current read와 정규화된 멱등 요청으로 동시 replay를 한 번만 반영한 사례
    2. 주문·포인트·이력·Outbox를 하나의 DB 트랜잭션에 넣어 ghost data를 막은 사례
    3. Outbox lease·claim token fencing과 소비자 중복 제거로 at-least-once 전달을 안전하게 만든 사례
    4. 인기 메뉴 SQL의 `EXPLAIN ANALYZE` 관찰만으로 인덱스·캐시를 성급히 추가하지 않은 사례
    5. k6 실패 기준선에서 p95·오류율·dropped iterations·Hikari 대기를 함께 읽고, 개선 전 비교 기준으로 남긴 사례
  - 각 사례에서 README의 설계 기준, 실제 테스트 또는 기준선, 제출 기록을 링크로 연결하고, 사실·추론·미확정 후속 개선을 구분한다.
  - README의 다음 단계에서 TIL 문서로 이동할 수 있게 한 줄 링크를 추가한다.
- 제외 범위:
  - Java·Spring·DB schema·migration·API·Outbox 정책·성능 설정의 동작 변경
  - k6 재실행, 새 벤치마크 수치 작성, 인덱스·Redis·replica·pool tuning 추가
  - 개인 MySQL·IntelliJ·Docker 자격 증명, 비밀값, 단순 일지나 커밋 목록의 전사
- 완료 조건:
  - 다섯 사례 모두 재현 조건, 실제 관찰·실패 또는 위험, 검토 대안, 채택 이유, 테스트·기준선 근거, 한계를 포함한다.
  - S11의 실패 결과를 성능 통과나 확정 원인으로 오해하지 않게 기록하고, 향후 개선은 별도 측정·이슈가 필요함을 명시한다.
  - README와 TIL 문서의 링크·용어·파일 경로가 실제 저장소와 일치하고 문서 검증 및 전체 테스트·패키징 검사가 성공한다.
- 검증 명령:

    rg -n "동시성|멱등|트랜잭션|Outbox|EXPLAIN|k6|Hikari" README.md docs/TIL_TROUBLESHOOTING.md docs/IMPLEMENTATION_RECORDS.md docs/IMPLEMENTATION_HISTORY.md docs/performance/S11_BASELINE.md
    .\gradlew.bat test --no-daemon --rerun-tasks
    .\gradlew.bat bootJar --no-daemon
    git diff --check

## 작업 상세 템플릿

다음 작업을 READY로 바꿀 때 아래 형식을 복사해 구체화한다.

    ### [작업 ID] 작업명

    - 상태: READY
    - 목적:
    - 요구사항 근거:
    - 선행 작업:
    - 대상 파일:
      - 정확한 저장소/상대/경로
    - 먼저 수행할 테스트 또는 검증:
    - 구현 범위:
    - 제외 범위:
    - 완료 조건:
    - 검증 명령:

## 경량 적용 범위

- .specify/, specs/은 Spec Kit 명령 파일을 추가하지 않는다.
- 번호 기반 기능 브랜치를 자동 생성하지 않고 현재 dev 작업 흐름을 유지한다.
- README의 요구사항과 설계를 작업 문서에 중복 전사하지 않는다.
- 작업 목록 자동 생성 대신 단계가 READY가 될 때 필요한 정보만 구체화한다.
- 프로젝트 규모와 작업 인원을 고려해 커스텀 자동화의 이점이 유지 비용보다 클 때만 도입을 다시 검토한다.
