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
| [S14-01](https://github.com/usersy628/coffee-order-system/issues/11) | RECORDED | S6-01, S7-01, S8-01, S9-01, S10-01, S11-01, S12-01 | 구현 중 수시 기록한 내용을 정리한 TIL 트러블슈팅 문서 | [IMPLEMENTATION_RECORDS.md](IMPLEMENTATION_RECORDS.md)의 제출 기록과 연결 PR에서 검증 |
| [S15-01](https://github.com/usersy628/coffee-order-system/issues/12) | IN_PROGRESS | S13-01, S14-01 | 전체 테스트·보안정보·공개 저장소 제출 검증 | 공개 원격의 깨끗한 clone에서 전체 테스트·패키징·실행 smoke가 성공하고 보안정보·불필요한 산출물이 없음 |

S9-01과 S10-01은 모두 S8-01만 직접 선행하므로 서로 독립적으로 진행할 수 있다. MySQL Testcontainers 기반은 S5-02에서 만들고 S5-03에서 migration 재실행 검증을 보강한 뒤 각 기능 단계에서 사용한다. S11-01의 상세는 제출 기록으로 이동했으므로, S12-01을 시작하기 전에는 정적 기록과 연결한 GitHub PR을 함께 확인한다.

## 준비·진행 중인 작업 상세

현재 IN_PROGRESS 작업은 S15-01 하나다. DOC-02, S11-01, S12-01, S13-01과 S14-01의 계획·구현·검증 상세는 [IMPLEMENTATION_RECORDS.md](IMPLEMENTATION_RECORDS.md)에 보존한다.

### [S15-01](https://github.com/usersy628/coffee-order-system/issues/12) 전체 테스트·보안정보·공개 저장소 제출 상태를 검증한다

- 상태: IN_PROGRESS
- 목적: 공개 원격에서 새로 받은 평가 환경도 저장소 문서만으로 프로젝트를 빌드·테스트·실행할 수 있고, 현재 스냅샷과 Git 이력에 보안정보나 불필요한 산출물이 포함되지 않았음을 최종 확인한다.
- 요구사항 근거:
  - [README.md의 설정, 초기 데이터와 테스트 구성](../README.md#설정-초기-데이터와-테스트-구성)
  - [README.md의 실행 가이드](../README.md#실행-가이드)
  - [README.md의 테스트 전략](../README.md#테스트-전략)
  - [README.md의 다음 단계](../README.md#다음-단계)
  - [issue #12](https://github.com/usersy628/coffee-order-system/issues/12)
- 선행 작업: S13-01과 S14-01이 제출 기록으로 보존됐고, PR #34와 PR #35가 반영된 최신 `origin/dev`를 기준으로 한다.
- 작업 브랜치: feature/issue-12-final-submission-verification
- 대상 파일:
  - README.md
  - docs/IMPLEMENTATION_PLAN.md
  - docs/PROJECT_STATUS.md
  - docs/IMPLEMENTATION_RECORDS.md (PR URL 생성 뒤 PR_SUBMISSION 기록 이관 시)
- 먼저 수행할 테스트 또는 검증:
  1. `git status --short --branch`, `git log --oneline -5`, `git remote -v`와 GitHub 저장소 메타데이터를 확인해 작업 기준이 최신 공개 `dev`이고 기본 브랜치가 `dev`인지 확인한다.
  2. `git ls-files`와 전체 Git 이력을 대상으로 비밀 키 표식·고신뢰 토큰 형식·자격 증명 파일명·빌드 및 IDE 산출물을 검사한다. 매칭된 비밀 문자열 자체는 콘솔이나 문서에 출력하지 않고 커밋·파일 경로만 확인한다.
  3. `.gitignore`, `.env.example`, `application*.yml`, Compose 파일과 GitHub Actions를 대조해 운영 비밀번호가 없고 개발용 예시 값·loopback 바인딩·검증 명령이 README와 일치하는지 확인한다.
  4. 공개 원격의 S15 브랜치를 새로운 임시 디렉터리에 clone하고, 그 clone에서만 전체 테스트·패키징·애플리케이션 health smoke를 실행한다.
- 구현 범위:
  - 현재 추적 파일과 모든 reachable commit을 대상으로 비밀 키, GitHub·AWS·Google·Slack·OpenAI 계열의 고신뢰 토큰 형식과 자격 증명성 파일명을 검사한다.
  - Gradle·IDE·OS·로컬 환경 산출물이 추적되지 않았고 `gradle/wrapper/gradle-wrapper.jar`만 의도한 JAR 예외인지 확인한다.
  - 공개 저장소 여부, 기본 브랜치 `dev`, S13·S14 반영 여부, README 실행·테스트 안내와 CI 명령의 정합성을 확인한다.
  - 공개 원격의 깨끗한 clone에서 `clean test`, `bootJar`, JUnit XML 합계와 실행 JAR 생성을 확인한다.
  - 고유 Compose project와 기본 로컬 환경과 겹치지 않는 임시 포트를 사용해 MySQL과 패키징된 애플리케이션을 시작하고 `/actuator/health`가 `UP`인지 확인한 뒤 생성한 프로세스·컨테이너만 정리한다.
  - 검증 완료 뒤 README의 남은 마일스톤 표현, Plan 상태와 Project Status를 최종 제출 단계에 맞게 정리한다.
- 제외 범위:
  - Java·DB schema·migration·API·Outbox·성능 설정의 동작 변경
  - 검증 실패를 숨기기 위한 테스트 비활성화, 예외 처리 또는 결과 문구만의 우회
  - 사용자의 기존 local MySQL 3307·서버 18080·Docker volume·IntelliJ 설정 변경
  - 실제 토큰·비밀번호·키의 콘솔 출력 또는 저장소 문서 기록
  - 검증 중 발견한 새 코드·설정 결함의 무계획 수정. 대상 파일이나 완료 조건이 달라지면 먼저 이 상세를 갱신한다.
- 완료 조건:
  - 공개 원격의 깨끗한 clone에서 Gradle Wrapper 검증, 전체 테스트, `bootJar`와 패키징 JAR health smoke가 성공한다.
  - JUnit XML의 failures와 errors가 0이고 실행 JAR 및 필요한 Wrapper 파일은 존재하며, 빌드·IDE·로컬 환경 산출물은 추적되지 않는다.
  - 현재 스냅샷과 전체 Git 이력의 고신뢰 비밀 패턴 및 자격 증명성 파일명 검사에 미해결 항목이 없다. 예측 가능한 `.env.example` 개발값은 운영 자격 증명이 아님을 README와 함께 확인한다.
  - GitHub 저장소는 public이고 기본 브랜치는 `dev`이며, README·Plan·Project Status와 공개 원격의 제출 상태가 모순되지 않는다.
  - `git diff --check`가 성공하고 검증 결과에는 비밀값, 개인 환경 값, CI 실행 시간 또는 특정 CI head를 고정하지 않는다.
- 검증 명령:

    git status --short --branch
    git log --oneline -5
    git remote -v
    git ls-files
    $artifactHits = git ls-files | Where-Object { ($_ -match '(^|/)(build|\.gradle|\.idea|out|work|outputs)/|\.(class|log|iws|iml|ipr)$') -or (($_ -match '\.jar$') -and ($_ -ne 'gradle/wrapper/gradle-wrapper.jar')) }; if ($artifactHits) { $artifactHits; exit 1 }
    $credentialPaths = git rev-list --objects --all | Where-Object { $_ -match '(^|[ /])(\.env($|\.)|id_(rsa|dsa|ecdsa|ed25519)$|credentials($|\.)|[^/]+\.(pem|key|p12|pfx|jks|keystore)$)' -and $_ -notmatch '\.env\.example$' }; if ($credentialPaths) { $credentialPaths; exit 1 }
    $secretPattern = '-----BEGIN (RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----|github_pat_[A-Za-z0-9_]{20,}|gh[pousr]_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z_-]{35}|xox[baprs]-[0-9A-Za-z-]{10,}|sk-(proj-)?[A-Za-z0-9_-]{20,}'
    $secretHits = git rev-list --all | ForEach-Object { $commit = $_; $paths = git grep -I -l -E -e $secretPattern $commit 2>$null; if ($LASTEXITCODE -gt 1) { throw "Secret scan failed for $commit" }; $paths }; if ($secretHits) { $secretHits | Sort-Object -Unique; exit 1 }
    .\gradlew.bat clean test --no-daemon --rerun-tasks
    .\gradlew.bat bootJar --no-daemon
    git diff --check

  공개 원격의 깨끗한 clone 검증은 다음 순서로 수행한다. 고유 임시 경로·Compose project와 기존 local/perf 환경에 쓰지 않는 3309·18082 포트를 사용한다.

    $clonePath = Join-Path ([IO.Path]::GetTempPath()) ("coffee-order-system-s15-" + [guid]::NewGuid().ToString("N"))
    $composeProject = "coffee-order-system-s15-" + [guid]::NewGuid().ToString("N").Substring(0, 8)
    git clone --branch feature/issue-12-final-submission-verification --single-branch https://github.com/usersy628/coffee-order-system.git $clonePath
    Push-Location $clonePath
    .\gradlew.bat clean test --no-daemon --rerun-tasks
    .\gradlew.bat bootJar --no-daemon
    $files = Get-ChildItem build\test-results\test\TEST-*.xml; $tests = 0; $failures = 0; $errors = 0; foreach ($file in $files) { [xml]$xml = Get-Content -Raw $file.FullName; $tests += [int]$xml.testsuite.tests; $failures += [int]$xml.testsuite.failures; $errors += [int]$xml.testsuite.errors }; if ($failures -ne 0 -or $errors -ne 0) { throw "JUnit failures=$failures errors=$errors" }; "JUnit XML: files=$($files.Count) tests=$tests failures=$failures errors=$errors"
    $env:COMPOSE_PROJECT_NAME = $composeProject; $env:MYSQL_PORT = "3309"; $env:MYSQL_DATABASE = "coffee_order"; $env:MYSQL_USER = "coffee"; $env:MYSQL_PASSWORD = "coffee-local"; $env:MYSQL_ROOT_PASSWORD = "root-local"; $env:SERVER_PORT = "18082"; $env:SPRING_PROFILES_ACTIVE = "local"
    docker compose --env-file .env.example -f compose.yaml up -d --wait
    $app = Start-Process -FilePath java -ArgumentList "-jar", "build/libs/coffee-order-system-0.0.1-SNAPSHOT.jar" -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $clonePath "s15-app.out.log") -RedirectStandardError (Join-Path $clonePath "s15-app.err.log")
    $health = $null; for ($attempt = 1; $attempt -le 60 -and $null -eq $health; $attempt++) { try { $health = Invoke-RestMethod -Uri "http://127.0.0.1:18082/actuator/health" -TimeoutSec 2 } catch { Start-Sleep -Seconds 1 } }; if ($health.status -ne "UP") { throw "Application health smoke failed" }
    Stop-Process -Id $app.Id -ErrorAction SilentlyContinue
    docker compose --env-file .env.example -f compose.yaml down -v
    Pop-Location
    $resolvedClone = [IO.Path]::GetFullPath($clonePath); $resolvedTemp = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()); if (-not $resolvedClone.StartsWith($resolvedTemp, [StringComparison]::OrdinalIgnoreCase) -or -not (Split-Path $resolvedClone -Leaf).StartsWith("coffee-order-system-s15-")) { throw "Unsafe cleanup path: $resolvedClone" }; Remove-Item -LiteralPath $resolvedClone -Recurse -Force

  smoke 실패 시에도 같은 경로·project 검증을 거친 뒤 생성한 애플리케이션 프로세스와 `$composeProject`만 정리한다. 로그는 원문 비밀값을 문서에 복사하지 않고 실패 원인 확인에만 사용한다.

#### 실제 구현 결과

- 현재 추적 파일과 모든 reachable commit에서 고신뢰 비밀 패턴, 자격 증명성 파일명과 불필요한 빌드·IDE 산출물을 검사했고 미해결 항목이 없음을 확인했다.
- `.env`는 추적되지 않고 `.env.example`의 값은 README가 설명하는 loopback 전용 개발 예시와 일치했다. Gradle Wrapper JAR는 의도한 추적 예외이며 README의 Java 17·Testcontainers·패키징 명령은 GitHub Actions와 일치했다.
- GitHub 공개 메타데이터에서 저장소가 public이고 기본 브랜치가 `dev`이며 S13·S14가 반영된 것을 확인했다.
- 공개 원격의 S15 브랜치를 고유 임시 경로에 새로 clone해 전체 테스트와 `bootJar`를 반복하고, 전용 Compose project와 포트 3309·18082에서 패키징 JAR의 `/actuator/health`가 `UP`임을 확인했다.
- 검증용 Java 프로세스, 컨테이너, network, volume과 임시 clone을 정리했고 기존 local 3307·18080 및 perf 3308·18081 환경은 사용하지 않았다.
- README의 남은 마일스톤 표현을 제거하고 계획한 고수준 구현·문서화 단계와 최종 제출 검증 근거의 위치를 정리했다.

#### 계획 대비 변경

- 고신뢰 비밀 패턴이 하이픈으로 시작할 때 `git grep`이 옵션으로 오인하는 것을 최종 스냅샷 재검사에서 발견했다. 패턴을 `-e`로 명시하고 exit code 2 이상의 실행 오류를 실패 처리하도록 검증 명령을 보완한 뒤 현재 스냅샷과 전체 reachable history를 다시 검사했다.
- Windows에서 smoke 프로세스 종료 직후 JAR 핸들 해제가 지연되어, 해당 실행 시각의 Java 프로세스를 확인·종료한 뒤 검증된 임시 경로 정리를 재시도했다. 두 조정 모두 검증 신뢰성과 정리 절차를 보완했으며 대상 파일·완료 조건은 바뀌지 않았다.

#### 실제 검증 결과

| 검증 명령 또는 확인 | 결과 |
| --- | --- |
| 추적 산출물 검사 | 불필요한 빌드·IDE·로컬 환경 산출물 없음, Gradle Wrapper JAR만 의도한 예외 |
| 전체 Git 이력 자격 증명성 파일명·고신뢰 비밀 패턴 검사 | 미해결 항목 없음, 비밀 문자열 원문은 출력하지 않음 |
| `.gitignore`·`.env.example`·`application*.yml`·Compose·README·CI 대조 | 운영 자격 증명 없음, 개발 예시·loopback·Java 17·검증 명령 일치 |
| 현재 브랜치 `clean test --no-daemon --rerun-tasks` | JUnit XML 23개 파일, 105개 테스트, 실패 0, 오류 0 |
| 현재 브랜치 `bootJar --no-daemon` | 성공, 실행 JAR 생성 |
| 공개 원격의 깨끗한 clone `clean test --no-daemon --rerun-tasks` | JUnit XML 23개 파일, 105개 테스트, 실패 0, 오류 0 |
| 공개 원격의 깨끗한 clone `bootJar --no-daemon` | 성공, 실행 JAR 생성 |
| 패키징 JAR 실행 smoke | 전용 MySQL과 `local` profile 기동, `/actuator/health` 응답 `UP` |
| 검증 자원 정리 | 전용 Java 프로세스·container·network·volume·임시 clone 제거, 포트 3309·18082 해제 |
| GitHub 저장소 메타데이터 | public, 기본 브랜치 `dev`, S13·S14 반영 확인 |
| `git diff --check` | 성공 |

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
