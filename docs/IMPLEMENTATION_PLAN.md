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
| [S13-01](https://github.com/usersy628/coffee-order-system/issues/10) | IN_PROGRESS | S12-01 | README 실행 방법과 구현 근거 보강 | 새 환경에서 문서만으로 실행·테스트 가능 |
| [S14-01](https://github.com/usersy628/coffee-order-system/issues/11) | BACKLOG | S6-01, S7-01, S8-01, S9-01, S10-01, S11-01, S12-01 | 구현 중 수시 기록한 내용을 정리한 TIL 트러블슈팅 문서 | 문제·원인·해결·검증 근거가 기록됨 |
| [S15-01](https://github.com/usersy628/coffee-order-system/issues/12) | BACKLOG | S13-01, S14-01 | 전체 테스트·보안정보·공개 저장소 제출 검증 | 깨끗한 clone 기준 빌드와 전체 테스트 성공 |

S9-01과 S10-01은 모두 S8-01만 직접 선행하므로 서로 독립적으로 진행할 수 있다. MySQL Testcontainers 기반은 S5-02에서 만들고 S5-03에서 migration 재실행 검증을 보강한 뒤 각 기능 단계에서 사용한다. S11-01의 상세는 제출 기록으로 이동했으므로, S12-01을 시작하기 전에는 정적 기록과 연결한 GitHub PR을 함께 확인한다.

## 준비·진행 중인 작업 상세

현재 IN_PROGRESS 작업은 S13-01 하나다. DOC-02, S11-01과 S12-01의 계획·구현·검증 상세는 [IMPLEMENTATION_RECORDS.md](IMPLEMENTATION_RECORDS.md)에 보존한다. 다음 기능 작업을 시작하기 전에는 연결한 GitHub PR의 라이브 상태를 확인한다.

### [S13-01](https://github.com/usersy628/coffee-order-system/issues/10) README 실행 방법과 구현 근거를 완성한다

- 상태: IN_PROGRESS
- 목적: 처음 받은 개발자가 README만으로 일반 로컬 MySQL, Spring Boot `local` 프로필, Testcontainers 테스트와 과제용 API 예시를 실제 구현과 같은 방식으로 실행·검증하게 한다.
- 요구사항 근거:
  - [README.md의 런타임과 빌드 도구](../README.md#런타임과-빌드-도구)
  - [README.md의 설정, 초기 데이터와 테스트 구성](../README.md#설정-초기-데이터와-테스트-구성)
  - [README.md의 API 명세](../README.md#api-명세)
  - [README.md의 테스트 전략](../README.md#테스트-전략)
  - [issue #10](https://github.com/usersy628/coffee-order-system/issues/10)
- 선행 작업: S12-01의 오류·traceId 계약과 PR #33의 제출 기록을 반영한 최신 `origin/dev`를 기준으로 한다.
- 작업 브랜치: feature/issue-10-readme-runbook
- 대상 파일:
  - README.md
  - .env.example
  - docs/IMPLEMENTATION_PLAN.md
  - docs/PROJECT_STATUS.md
- 먼저 수행할 테스트 또는 검증:
  1. 일반 로컬 `compose.yaml`, `.env.example`, `application-local.yml`의 MySQL image·환경 변수·loopback port가 같은 계약인지 확인한다.
  2. `application-local.yml`, `application-test.yml`, Flyway V2 초기 데이터와 Controller·DTO를 읽어 환경 변수, 초기 사용자·메뉴, API 예시가 실제 값과 같은지 확인한다.
  3. 성능 전용 `docker-compose.performance.yml`의 별도 DB·포트·비밀값 규칙을 확인해 일반 로컬 실행 절차와 섞이지 않게 한다.
- 구현 범위:
  - 기존 일반 개발용 MySQL 8.4.10 `compose.yaml`의 데이터베이스·사용자·비밀번호·host port 환경 변수, loopback 바인딩·UTC·healthcheck·named volume을 README에 정확히 설명한다.
  - README에 Java 17·Docker 준비 조건, `.env.example` 복사, Compose 기동, `SPRING_PROFILES_ACTIVE=local` 실행, IntelliJ 환경 변수 입력, `/actuator/health` 확인, Flyway V2 초기 데이터, Testcontainers 전체 테스트와 `bootJar` 절차를 작성한다.
  - `MYSQL_PORT=3307`, `SERVER_PORT=18080` 같은 개인 로컬 오버라이드는 지원하되 저장소 기본값이나 애플리케이션 설정을 개인 포트로 바꾸지 않는다.
  - 실제 Controller·DTO와 V2 초기 데이터에 맞춘 `curl.exe` 메뉴 조회·포인트 충전·주문·인기 메뉴 예시와 멱등 키 재사용 주의사항을 작성한다.
  - README의 구현 현황·다음 단계에서 이미 끝난 S11·S12를 최신 기록 경로와 남은 S13~S15 순서에 맞게 정리한다.
- 제외 범위:
  - Java·Spring·DB schema·migration·API 계약·CI·성능 전용 Compose의 동작 변경
  - 운영용 비밀번호·외부 데이터 플랫폼 URL·개인 IntelliJ 설정의 저장
  - 일반 로컬 실행에 성능 기준선의 `local,perf` DB·포트·환경 변수를 섞는 일
- 완료 조건:
  - 새 환경이 README의 순서만으로 일반 MySQL을 기동하고 `local` 앱의 health 및 초기 메뉴 조회까지 확인할 수 있다.
  - `MYSQL_PORT`와 `SERVER_PORT` 오버라이드가 Compose·애플리케이션 실행 설명에서 일관되고, Testcontainers 테스트가 로컬 개발 DB를 사용하지 않는다고 명시된다.
  - API 예시가 V2의 사용자 1~3, 판매 중인 메뉴 1~2, 실제 request/response·Idempotency 헤더 계약과 일치한다.
  - README의 현재 단계, 파일 참조, 일반·성능 실행 경계가 실제 저장소와 일치하고 문서 공백 검증·전체 테스트·bootJar·diff 검사가 성공한다.
- 검증 명령:

    docker compose --env-file .env.example -f compose.yaml config
    docker compose --env-file .env.example -p coffee-order-system-s13 -f compose.yaml up -d --wait
    $env:SPRING_PROFILES_ACTIVE='local'; $env:MYSQL_PORT='3309'; $env:SERVER_PORT='18082'; .\gradlew.bat bootRun
    curl.exe --fail http://127.0.0.1:18082/actuator/health
    curl.exe --fail http://127.0.0.1:18082/api/menus
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
