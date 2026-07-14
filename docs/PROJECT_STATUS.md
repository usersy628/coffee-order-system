# Project Status

마지막 갱신일: 2026-07-14

## 현재 단계

- 2단계 도메인 및 ERD 설계 완료
- 3단계 API 명세 작성 완료
- 4단계 동시성·트랜잭션·Outbox 전략 상세 검토 완료
- 현재 작업: `S5-01` 기술 스택과 프로젝트 구조 승인 (`READY`)
- Spring Boot 프로젝트와 애플리케이션 코드는 아직 생성하지 않음

## 문서 기준

- 제품 요구사항, ERD, API 계약과 기술적 결정: `README.md`
- 구현 작업 ID, 선행 관계, 대상 파일과 검증 기준: `docs/IMPLEMENTATION_PLAN.md`
- 작업·Git·보안·인수인계 규칙: `AGENTS.md`
- 이 문서에는 현재 단계, 미결 사항과 다음 행동만 기록하며 위 문서의 내용을 복제하지 않음

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

## 미결 사항

- Java와 Spring Boot 세부 버전
- Gradle과 Maven 비교를 포함한 빌드 도구 선택 및 버전
- 기본 의존성, 패키지 구조와 도메인·애플리케이션·인프라 계층 경계
- Flyway 적용 여부와 초기 사용자·메뉴 데이터 구성 방식
- 트랜잭션 재시도와 새 트랜잭션 조회를 Spring proxy 경계에서 분리하는 클래스 구조
- 외부 결제 API 없이 Mock 데이터 수집 플랫폼에만 사용할 HTTP 클라이언트와 테스트 대역 방식
- 로컬·테스트 설정 파일과 MySQL Testcontainers 기반 구성

현재 진행을 막는 외부 차단 사항은 없다. 위 선택은 `S5-01` 추천안을 검토한 뒤 사용자 승인으로 확정한다.

## 다음 행동

`S5-01`을 수행한다. Java·Spring Boot·빌드 도구·MySQL·Flyway·Testcontainers와 Mock 데이터 수집용 HTTP 클라이언트의 추천 버전과 선택 이유, 패키지 구조, 설정 및 테스트 소스 구성을 먼저 제시한다. 사용자가 승인하면 결정 내용을 문서화하고 `S5-02`의 정확한 대상 파일과 검증 명령을 채운 뒤에만 프로젝트 파일을 생성한다.

## 작업 재개 기준

- 현재 브랜치: `dev`
- 원격 저장소: `https://github.com/usersy628/coffee-order-system.git`
- 경량 구현 계획 기준 커밋: `999f04e docs: add lightweight implementation workflow`
- 최신 설계 기준 커밋: `dd2a27c docs: simplify design after tutor feedback`
- 예상 작업 트리: clean, `dev`와 `origin/dev` 동기화
- 재개 시 `AGENTS.md`의 저장소 확인 명령으로 실제 상태를 다시 검증
