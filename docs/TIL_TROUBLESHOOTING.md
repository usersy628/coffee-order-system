# TIL 트러블슈팅: 검증 기록으로 배우는 동시성·Outbox·성능 해석

> 이 문서는 구현 과정에서 실제로 확인한 문제와 판단을 되짚는 학습 기록이다. 현재 요구사항·ERD·API·운영 정책의 단일 기준은 계속 [README.md](../README.md)다. 여기의 성능 수치는 특정 환경의 기준선이며, 설정 변경의 지시나 일반화된 성능 보장은 아니다.

## 읽는 방법

각 사례는 같은 순서로 읽는다.

1. **문제와 재현 조건**: 어떤 입력·동시성·데이터셋에서 위험이 나타나는가
2. **관찰**: 테스트나 기준선에서 실제로 확인한 사실은 무엇인가
3. **원인과 대안**: 왜 그런 위험이 생기며, 무엇을 검토했는가
4. **선택과 검증**: 이 프로젝트가 택한 경계와 그것을 확인한 근거는 무엇인가
5. **한계와 다음 질문**: 아직 증명되지 않았거나, 별도 이슈·측정이 필요한 것은 무엇인가

`관찰`은 테스트·기록에 있는 사실이고, `해석`은 그 사실을 바탕으로 한 설명이다. 둘을 섞지 않는 것이 트러블슈팅 문서에서 가장 중요하다.

## 1. 같은 주문을 여러 번 받으면 왜 한 번만 결제되어야 할까

### 문제와 재현 조건

사용자가 주문 버튼을 여러 번 누르거나, 응답을 못 받은 클라이언트가 같은 요청을 재전송할 수 있다. 특히 메뉴 배열의 순서만 다른 요청은 사용자가 의도한 주문은 같지만 JSON 문자열은 달라질 수 있다.

- 주문 요청은 메뉴 ID 순으로 정규화한 뒤 해시를 만들고, 사용자별 `Idempotency-Key`와 함께 저장한다.
- 같은 사용자의 동시 주문은 먼저 지갑 행을 `FOR UPDATE`로 잠근다. 잠금 뒤 기존 주문을 current read로 확인하고, 그 다음 메뉴와 금액을 조회한다.
- 실제 MySQL 교차 회귀 테스트는 같은 주문을 동시에 10번 보냈을 때 최초 1건과 replay 9건, 주문·`USE` 이력·Outbox 각 1건을 확인한다. 자세한 검증은 [CrossFeatureMySqlRegressionIntegrationTest](../src/test/java/com/usersy628/coffeeorder/regression/CrossFeatureMySqlRegressionIntegrationTest.java)에서 볼 수 있다.

### 관찰과 원인

과거 100개 동시 replay 검증에서, 지갑 잠금 전에 일반 메뉴 조회를 먼저 하면 MySQL `REPEATABLE READ`의 snapshot 때문에 선행 주문이 만든 이력을 보지 못하는 결함이 발견됐다. 단순히 “키가 같으니 먼저 조회하면 된다”는 순서는 동시 트랜잭션의 읽기 시점을 설명하지 못했다.

동시에 입력 배열의 순서가 다르면 원문 JSON을 그대로 해시하는 방식은 같은 주문을 다른 요청으로 취급할 위험이 있다. 따라서 이 프로젝트는 메뉴 ID 정렬 후 `menuId:quantity` 형태를 SHA-256으로 해시한다. 구현 위치는 [OrderRequestHasher](../src/main/java/com/usersy628/coffeeorder/order/application/OrderRequestHasher.java), 실제 실행 순서는 [OrderTransactionExecutor](../src/main/java/com/usersy628/coffeeorder/order/application/OrderTransactionExecutor.java)에 있다.

### 검토한 대안과 선택

| 대안 | 장점 | 이 사례에서 부족한 점 |
| --- | --- | --- |
| 애플리케이션 메모리 `synchronized` | 코드가 짧다 | 서버가 여러 대면 서로의 메모리를 공유하지 않는다. |
| 잠금 없이 먼저 기존 주문만 조회 | 평상시 SQL이 적어 보인다 | 동시 요청에서 같은 시점의 결과를 보장하지 못한다. |
| DB 유니크 제약만 사용 | 마지막 방어선이 된다 | 충돌 뒤 어떤 요청이 최초 결과인지 복원하는 흐름까지 설명하지 못한다. |
| 지갑 잠금 → current read → 정규화 해시·유니크 제약 | 다중 서버에서도 같은 DB 기준으로 직렬화하고 replay를 복원한다 | 같은 사용자 요청은 대기하므로 트랜잭션을 짧게 유지해야 한다. |

선택한 방식은 마지막 행이다. 유니크 제약은 최종 방어선으로 남기고, 지갑 잠금 안에서 기존 주문을 확인해 최초 결과를 재사용한다. 메뉴 조회를 잠금과 current read 뒤에 둔 이유도 이 순서를 지키기 위해서다.

### 검증 근거와 한계

- [OrderConcurrencyIntegrationTest](../src/test/java/com/usersy628/coffeeorder/order/infrastructure/OrderConcurrencyIntegrationTest.java)는 실제 MySQL 행 잠금과 timeout 재시도 소진 시 `503 CONCURRENT_REQUEST_TIMEOUT`을 확인한다.
- [S8-01 완료 이력](IMPLEMENTATION_HISTORY.md)에는 같은 키 100개 동시 요청에서 한 번만 차감·저장된 검증과 snapshot 문제의 수정 기록이 남아 있다.
- 이 방식은 같은 사용자의 경합을 없애지 않는다. 경합이 지속될 때 락 대기·timeout 비율을 관찰하고, 락 순서와 트랜잭션 길이를 먼저 다시 검토해야 한다.

## 2. 주문이 실패했는데 일부 데이터만 남는 ghost data는 어떻게 막았을까

### 문제와 재현 조건

주문 하나에는 주문 본문, 주문 항목, 지갑 차감, `USE` 포인트 이력, 외부 전송용 Outbox가 함께 필요하다. 이 중 하나라도 실패한 뒤 앞선 데이터만 남으면 잔액·주문·외부 전송 대상이 서로 달라진다. 이를 ghost data라고 부른다.

[OrderTransactionExecutor](../src/main/java/com/usersy628/coffeeorder/order/application/OrderTransactionExecutor.java)는 이 저장들을 하나의 `@Transactional(timeout = 5)` 경계에 둔다. 외부 HTTP 호출은 이 경계에 넣지 않고, 여기서는 `PENDING` Outbox 이벤트만 저장한다.

### 관찰과 원인

[CrossFeatureMySqlRegressionIntegrationTest](../src/test/java/com/usersy628/coffeeorder/regression/CrossFeatureMySqlRegressionIntegrationTest.java)는 잔액이 부족한 주문을 실제 MySQL에서 실행한 뒤 다음을 함께 확인한다.

- `orders`, `order_item`, `USE` 이력, `order_event_outbox`, Mock 수신 행이 모두 0건이다.
- 지갑 잔액은 요청 전 값 그대로다.
- 이후 인기 메뉴 조회도 빈 결과다.

즉 “주문을 거절했다”는 HTTP 응답만 보는 것이 아니라, 서로 다른 테이블에 부분 기록이 없는지를 확인한다. 이 검증이 없으면 단위 테스트만 통과한 상태에서 데이터 일관성 결함이 남을 수 있다.

### 검토한 대안과 선택

| 대안 | 장점 | 이 사례에서 부족한 점 |
| --- | --- | --- |
| 주문·포인트·Outbox를 각각 저장 | 구현을 단계별로 나눌 수 있다 | 중간 실패 때 보상 로직이 필요하고 누락될 위험이 크다. |
| 주문 트랜잭션 안에서 외부 HTTP 호출 | 겉보기에는 즉시 전송된다 | 느린 네트워크가 지갑 잠금을 오래 잡고, 외부 실패가 주문 성공을 막는다. |
| 내부 DB 저장을 하나의 트랜잭션으로 묶고 Outbox만 저장 | 내부 상태는 함께 커밋·롤백되고 외부 전송은 분리된다 | 이후 Outbox 게시·재시도·관찰이 필요하다. |

선택한 것은 마지막 방식이다. 주문이 커밋되면 전송해야 할 사실도 DB에 함께 남고, 주문이 롤백되면 전송할 이벤트도 남지 않는다. 외부 전송의 불확실성은 다음 사례의 Outbox 흐름이 담당한다.

### 검증 근거와 한계

- [S8-01 완료 이력](IMPLEMENTATION_HISTORY.md)에는 Outbox 저장 실패 시 주문·항목·차감·이력이 전체 롤백된 실제 검증이 남아 있다.
- 교차 회귀는 성공 주문에서 `PENDING` 1건과 이후 Mock 수신 1건까지 확인한다. 따라서 단순히 이벤트 행이 생겼는지만 보지 않는다.
- 이 트랜잭션은 외부 플랫폼에 실제로 전달됐다는 보장은 하지 않는다. 커밋 이후의 전달 보장은 Outbox와 소비자 중복 제거의 책임이다.

## 3. Outbox는 왜 한 번 이상 보내면서도 중복을 허용할까

### 문제와 재현 조건

게시자가 HTTP 요청을 보낸 뒤 응답을 받기 전에 네트워크가 끊기면, 외부 플랫폼이 받았는지 보내지 못했는지 알 수 없다. 이 상태에서 “절대 한 번만 보냈다”고 단정하면 이벤트 유실 위험이 있고, 재전송하면 중복 위험이 있다.

이 프로젝트는 **at-least-once 시도**를 택한다. 여러 게시자가 같은 `PENDING` 이벤트를 동시에 집지 않도록 짧은 claim 트랜잭션에서 `SKIP LOCKED`와 `claim_token`을 사용한다. lease가 끝난 이벤트는 다시 처리할 수 있지만, 이전 작업자가 늦게 끝나도 자신이 가진 예전 token으로 상태를 덮어쓸 수 없다.

### 관찰과 원인

- Outbox의 상태 전이·lease·fencing 계약은 [README의 Outbox 상태 전이와 fencing](../README.md#outbox-상태-전이와-fencing)에 정의돼 있다.
- [OutboxClaimIntegrationTest](../src/test/java/com/usersy628/coffeeorder/outbox/infrastructure/OutboxClaimIntegrationTest.java)는 실제 MySQL에서 동시 claim, 잠긴 due 행 건너뛰기, lease 회수와 이전 claim의 상태 갱신 차단을 확인한다.
- [OutboxPublisherIntegrationTest](../src/test/java/com/usersy628/coffeeorder/outbox/application/OutboxPublisherIntegrationTest.java)는 성공·재시도·최종 실패 흐름을 확인한다. retryable 실패는 최대 6번째 시도까지 DB 상태로 관리한다.
- 소비자는 `eventId`를 중복 제거 키로 저장한다. 같은 이벤트가 다시 도착해도 추가 반영 대신 기존 결과로 `200 OK`를 반환한다.

### 검토한 대안과 선택

| 대안 | 장점 | 이 사례에서 부족한 점 |
| --- | --- | --- |
| 주문 트랜잭션 안에서 직접 HTTP 호출 | 코드 경로가 짧다 | DB 커밋과 외부 호출의 성공을 한 원자 작업으로 만들 수 없고, 네트워크 지연이 주문 경합을 키운다. |
| exactly-once 전달을 가정 | 중복을 신경 쓰지 않아도 되는 것처럼 보인다 | HTTP 응답 유실 상황에서는 송신자가 성공 여부를 확정할 수 없다. |
| 단순 재시도만 하고 소비자는 중복 허용 | 송신 구현이 단순하다 | 같은 주문이 외부 분석 데이터에 여러 번 더해질 수 있다. |
| claim token fencing + 재시도 + 소비자 `eventId` 중복 제거 | 늦은 작업자의 상태 갱신을 막고, 재전송에도 최종 반영은 한 번으로 만든다 | 게시자·lease·상태 관찰과 실패 이벤트 운영이 필요하다. |

선택한 방식은 마지막 행이다. “전송을 한 번만 시도한다”가 아니라 “같은 이벤트를 다시 보내도 소비자가 한 번만 반영한다”가 정확한 목표다.

### 검증 근거와 한계

- [S9-01 완료 이력](IMPLEMENTATION_HISTORY.md)에는 실제 HTTP timeout·5xx·connection reset, lease recovery replay, Mock 수신의 순차·동시 중복 제거 검증이 남아 있다.
- 최대 시도 뒤 `FAILED`가 된 이벤트를 다시 보내는 운영자 redrive와 장기 보관·정리는 과제 범위 밖이다. 그래서 `FAILED`는 자동으로 사라진 성공이 아니라 후속 운영 판단이 필요한 상태다.

## 4. 실행계획 한 번을 보고 왜 인덱스나 Redis를 바로 넣지 않았을까

### 문제와 재현 조건

인기 메뉴는 최근 168시간 주문을 집계한다. 소량 데이터에서는 빨라도 데이터가 커지면 어느 테이블을 먼저 읽는지, 실제로 얼마나 많은 행을 만나는지 확인해야 한다.

[PopularMenuExplainAnalyzeIntegrationTest](../src/test/java/com/usersy628/coffeeorder/performance/PopularMenuExplainAnalyzeIntegrationTest.java)는 MySQL Testcontainers에 30일 주문 100,000건, 주문 항목 300,000건, 메뉴 100개를 만들고 `EXPLAIN ANALYZE` 보고서를 생성한다. 최근 168시간 안 70,000건과 앞선 23일 30,000건의 분포도 함께 확인한다.

### 관찰과 원인

S11에서 관찰한 실행 경로는 `order_item` 전체 scan → `orders` primary-key lookup → `menu` primary-key lookup이었다. `orders(paid_at)`과 `order_item(order_id, menu_id)` 인덱스가 존재하는 것은 테스트로 확인했지만, 옵티마이저가 항상 그 인덱스 순서를 택해야 한다고 assertion하지는 않았다.

실행계획은 데이터 분포, 통계, MySQL 버전과 설정에 따라 달라질 수 있다. 그래서 “이 인덱스가 있으니 항상 빠르다”나 “scan이 보였으니 새 인덱스가 정답이다” 둘 다 근거가 부족하다.

### 검토한 대안과 선택

| 대안 | 장점 | 이 사례에서 보류한 이유 |
| --- | --- | --- |
| 실행계획이 보이자마자 새 복합 인덱스 추가 | 특정 읽기를 빠르게 할 수 있다 | 쓰기 비용과 실제 비교 측정 없이 효과를 단정할 수 없다. |
| Redis·사전 집계 도입 | 반복 조회를 줄일 수 있다 | rolling window의 즉시 일치 정책과 운영 복잡도를 먼저 검토해야 한다. |
| read replica 도입 | 읽기 부하를 분산할 수 있다 | 복제 지연이 있고, 현재 병목이 읽기인지도 아직 확정되지 않았다. |
| 실제 fixture·계획을 기록하고 변화 전후를 비교 | 다음 변경의 기준점이 생긴다 | 즉시 성능 개선은 되지 않는다. |

선택한 것은 마지막 행이다. S11은 인덱스·Redis·replica를 추가하지 않았다. 먼저 같은 데이터셋과 시나리오에서 한 번에 하나의 변경만 비교해야, 개선인지 우연한 환경 차이인지 판단할 수 있다.

### 검증 근거와 한계

- [S11 기준선](performance/S11_BASELINE.md)의 `Testcontainers 실행계획 사전 검증`은 보고서 경로와 fixture 구성을 남긴다.
- 절대 실행 시간은 CI 합격 기준이 아니다. 이 테스트는 계획을 기록하고 데이터·인덱스 존재를 확인하는 장치이지, 특정 milliseconds를 강제하는 테스트가 아니다.
- 이 fixture와 단일 인스턴스만으로 운영 데이터 전체를 대표할 수는 없다. 새 인덱스나 캐시는 별도 이슈에서 동일·확장 데이터셋으로 전후 측정해야 한다.

## 5. k6가 실패했을 때 “원인을 찾았다”고 말하면 왜 위험할까

### 문제와 재현 조건

S11은 단일 `local,perf` 애플리케이션에서 `GET /api/menus/popular`만 constant-arrival-rate 30 RPS로 5분 동안 측정했다. 일반 local DB와 분리한 MySQL, 애플리케이션 포트, Docker k6 runner를 사용했다.

목표는 p95 500 ms 미만, HTTP 오류율 1% 미만, dropped iterations 0이었다. 이 목표는 구현의 합격 선언이 아니라 병목을 비교하기 위한 기준선이다.

### 실제 관찰

| 관찰 항목 | 실제 값 |
| --- | --- |
| p95 / HTTP 오류율 / 실제 RPS / dropped iterations | 3,074.07 ms / 51.87% / 19.88 RPS / 2,569 |
| Hikari 최대 관찰 | `active=10`, `pending=50`, `max=10` |
| MySQL row lock wait·deadlock·max-connection error | 관찰값 0 |
| Outbox 가장 오래된 `PENDING` | `N/A (write path not exercised)` |

즉 이 run은 성능 통과가 아니라 실패 기준선이다. Hikari가 최대 연결 수까지 사용되고 대기열이 쌓인 사실은 확인됐지만, 이 한 번의 run만으로 SQL·CPU·네트워크·pool 설정 중 하나를 유일한 원인이라고 확정할 수는 없다. MySQL row lock wait와 deadlock 관찰값이 0이므로, 이 GET-only run을 쓰기 락 병목의 증거로 해석해서도 안 된다.

### 검토한 대안과 선택

| 다음 행동 | 왜 바로 하지 않았는가 |
| --- | --- |
| Hikari pool만 즉시 키우기 | DB가 감당할 동시 연결과 실제 병목을 모른 채 대기를 DB 부하로 옮길 수 있다. |
| 인덱스·Redis·replica를 한 PR에 함께 넣기 | 무엇이 수치를 바꿨는지 알 수 없고, 정합성·운영 복잡도까지 한 번에 바뀐다. |
| 실패 수치를 숨기고 통과한 테스트만 기록 | 다음 개선의 기준점과 문제 재현성을 잃는다. |
| 환경·데이터셋·관찰값을 함께 남기고 후속 측정으로 비교 | 숫자를 같은 조건에서 해석할 수 있다. |

S11은 마지막 방식을 택했다. 환경 사양, Docker 자원, Java·MySQL 버전, 데이터셋, k6 image digest와 Hikari·MySQL 관찰값을 함께 기록했다. 그 덕분에 다음 성능 개선은 “전보다 빨라 보인다”가 아니라 같은 조건의 전후 비교로 검증할 수 있다.

### 검증 근거와 다음 질문

- 전체 환경과 실패 결과는 [S11 기준선](performance/S11_BASELINE.md)의 `실행 결과 기록`에 보존돼 있다.
- 이 기록은 사용자의 일반 local MySQL 3307과 서버 18080을 바꾸지 않는 분리 환경에서 만들었다.
- 후속 개선은 별도 이슈에서 한 번에 한 병목 가설만 세우고, 같은 fixture·시나리오·관찰 항목으로 다시 측정해야 한다. 현재 문서는 그 개선을 구현하거나 승인하지 않는다.

## 근거 빠른 찾기

| 확인하려는 내용 | 우선 볼 근거 |
| --- | --- |
| 제품 정책과 선택 이유 | [README.md](../README.md) |
| 완료 단계의 실제 구현·검증 | [IMPLEMENTATION_HISTORY.md](IMPLEMENTATION_HISTORY.md), [IMPLEMENTATION_RECORDS.md](IMPLEMENTATION_RECORDS.md) |
| S11 성능 환경·수치·한계 | [S11_BASELINE.md](performance/S11_BASELINE.md) |
| 동시 주문부터 Mock 수신·인기 메뉴까지의 교차 회귀 | [CrossFeatureMySqlRegressionIntegrationTest](../src/test/java/com/usersy628/coffeeorder/regression/CrossFeatureMySqlRegressionIntegrationTest.java) |
| 대용량 fixture와 실행계획 보고서 생성 | [PopularMenuExplainAnalyzeIntegrationTest](../src/test/java/com/usersy628/coffeeorder/performance/PopularMenuExplainAnalyzeIntegrationTest.java) |

새 문제를 발견했을 때는 이 문서에 결론만 덧붙이지 않는다. 먼저 재현 조건과 검증을 별도 이슈·테스트로 만들고, 실제 결과가 확인된 뒤에 이 표의 근거와 연결한다.
