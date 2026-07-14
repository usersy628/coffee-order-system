# Coffee Order System

다중 서버 환경에서 동시성, 데이터 일관성, 장애 복구를 고려하는 커피 주문 시스템 과제입니다.

현재는 요구사항 분석, ERD 설계와 API 명세를 완료한 단계이며, Spring Boot 코드는 이후 단계에서 구현합니다.

## 설계 목표와 의도

이 시스템의 핵심 목표는 다중 서버 환경에서 같은 사용자의 요청이 동시에 처리되더라도 포인트가 중복 사용되거나 주문, 포인트 이력과 외부 전송 대상 데이터가 서로 어긋나지 않도록 하는 것입니다.

포인트와 주문처럼 즉시 일관성이 필요한 데이터는 하나의 DB 트랜잭션과 DB 락으로 보호합니다. 반면 외부 데이터 플랫폼은 네트워크 장애와 응답 지연이 내부 주문 성공에 영향을 주지 않도록 Transactional Outbox로 분리합니다. 이 경계 안에서 다음 원칙을 지킵니다.

- 금액 계산과 상태 변경의 최종 판단은 클라이언트가 아니라 서버와 DB가 담당합니다.
- 주문, 포인트 차감, 이력과 Outbox 이벤트는 함께 성공하거나 함께 실패합니다.
- 재요청과 중복 전송은 정상적으로 발생할 수 있다고 보고 멱등성과 소비자 중복 제거를 설계합니다.
- 동시성 제어는 특정 JVM에 의존하지 않고 모든 서버 인스턴스가 공유하는 DB를 기준으로 수행합니다.
- 데이터 저장 시각은 UTC로 통일하고 한국 사용자가 보는 API 시각은 `Asia/Seoul`로 표현합니다.
- 테스트에서 경계 시각, 동시 요청과 외부 장애를 재현할 수 있도록 설계합니다.

## 과제 범위

- 커피 메뉴 목록 조회
- 포인트 충전
- 여러 메뉴 주문 및 포인트 결제
- 주문 데이터를 외부 데이터 플랫폼으로 실시간 전송
- 현재 시각 기준 최근 168시간 인기 메뉴 TOP 3 조회
- 다중 서버 동시성, 데이터 일관성 및 테스트 고려

회원가입, 사용자 관리, 메뉴 관리, 주문 취소는 이번 과제 범위에 포함하지 않습니다. 사용자와 메뉴는 사전에 존재하며, 사용자를 생성할 때 잔액이 0P인 포인트 지갑도 함께 생성합니다.

## 핵심 정책

### 포인트

- 1원은 1P이며 모든 금액은 정수로 관리합니다.
- 한 번에 1P 이상 300,000P 이하를 충전할 수 있습니다.
- 충전 후 총잔액은 300,000P를 초과할 수 없고, 잔액은 음수가 될 수 없습니다.
- 잔액 변경과 포인트 이력 저장은 하나의 트랜잭션으로 처리합니다.
- 다중 서버 동시성은 `SELECT ... FOR UPDATE`를 이용한 DB 비관적 락으로 제어합니다.
- 외부 API는 포인트 지갑 락을 보유한 트랜잭션 안에서 호출하지 않습니다.

### 주문

- 한 주문에 여러 메뉴와 메뉴별 수량을 입력할 수 있습니다.
- 수량은 1 이상이며, 한 요청에 같은 메뉴 ID가 중복되면 `400 Bad Request`로 실패합니다.
- 메뉴가 없거나 판매 중지된 항목이 하나라도 있으면 전체 주문을 실패시킵니다.
- 결제 금액은 클라이언트 입력이 아닌 DB의 메뉴 가격으로 계산합니다.
- 결제 후 잔액이 0P가 되는 것은 허용합니다.
- 주문, 주문 항목, 포인트 차감, 포인트 사용 이력, Outbox 이벤트가 하나의 DB 트랜잭션으로 커밋되어야 주문이 성공합니다.
- 주문 항목에는 주문 당시 메뉴명, 단가, 수량 및 항목 금액을 스냅샷으로 저장합니다.

### 주문 멱등성

- 주문 API의 `Idempotency-Key` 헤더는 필수입니다.
- 별도의 멱등성 테이블을 두지 않고 `orders`에 멱등 키와 요청 해시를 저장합니다.
- `(user_id, idempotency_key)` 유니크 제약을 최종 방어선으로 사용합니다.
- 메뉴 ID로 정렬한 `menuId`와 `quantity` 목록을 canonical payload로 만들고 SHA-256 해시를 저장합니다.
- 같은 키와 같은 요청이면 새 결제 없이 기존 주문 결과를 반환합니다.
- 같은 키와 다른 요청이면 `409 Conflict`와 `IDEMPOTENCY_KEY_REUSED` 오류를 반환합니다.
- 같은 키의 동시 요청은 선행 트랜잭션이 끝날 때까지 DB 유니크 인덱스에서 대기합니다. 충돌이 확인되면 현재 트랜잭션을 롤백하고 새 트랜잭션에서 기존 주문과 요청 해시를 조회합니다.
- 선행 요청이 롤백되면 멱등성 행도 남지 않으므로 후행 요청이 정상적으로 주문을 처리할 수 있습니다.

### 외부 데이터 플랫폼

- Transactional Outbox 패턴을 사용합니다.
- 주문 트랜잭션에서는 외부 API를 호출하지 않고 Outbox 이벤트까지만 저장합니다.
- 별도 게시자가 커밋된 이벤트를 외부 플랫폼으로 전송합니다.
- 전달 의미는 at-least-once이며, 소비자는 `eventId`로 중복 이벤트를 제거해야 합니다.
- 2xx는 이벤트 전송 성공으로 처리하지만 주문 성공 조건에는 영향을 주지 않습니다.
- 4xx는 재시도하지 않는 데이터 오류로 보고 `FAILED`로 전환합니다.
- 네트워크 오류와 5xx는 지수 백오프로 최초 실패 후 최대 5회 재시도합니다. 최초 전송을 포함한 최대 시도 횟수는 6회이며, 모두 실패하면 `FAILED`로 전환합니다.

### 인기 메뉴

- 조회 시각 `T`를 한 번만 고정합니다.
- `[T - 168시간, T)` 범위의 `PAID` 주문만 집계합니다.
- `SUM(order_item.quantity)`로 메뉴별 판매 수량을 계산합니다.
- 판매 수량 내림차순, 동률이면 메뉴 ID 오름차순으로 정렬하여 최대 3개를 반환합니다.
- 데이터가 없으면 빈 목록을 반환합니다.
- 현재 시각은 직접 호출하지 않고 테스트에서 교체할 수 있는 `Clock`을 주입합니다.

## API 명세

### 공통 규칙

- 기본 경로는 `/api`이며 현재 과제에서는 URL 버전을 포함하지 않습니다. 경로가 단순한 대신 향후 호환되지 않는 변경이 생기면 새 경로나 헤더 기반 버전 전략을 별도로 도입해야 합니다.
- 사용자 인증은 과제 범위 밖이므로 사용자 식별자는 경로의 `{userId}`로 전달합니다.
- `{userId}`는 1 이상의 정수여야 하며 형식이 잘못되면 `400 Bad Request`와 `INVALID_USER_ID`를 반환합니다.
- 요청과 응답은 `application/json`이며 JSON 필드명은 `camelCase`를 사용합니다.
- ID, 수량과 금액은 JSON 정수로 표현합니다. 금액 단위는 원이자 포인트입니다.
- 성공 응답은 공통 래퍼 없이 각 API의 결과를 직접 반환합니다.
- API 시간은 ISO 8601 형식과 한국시간 오프셋을 포함하여 `2026-07-14T15:30:00+09:00`처럼 반환합니다.
- 서버 내부와 DB에는 동일한 시각을 UTC로 저장하고 API 경계에서 `Asia/Seoul`로 변환합니다.

공통 오류 응답은 다음 형식을 사용합니다. `details`는 필드 오류처럼 추가 정보가 있을 때 사용하고, 없으면 빈 객체를 반환합니다.

```json
{
  "code": "INVALID_CHARGE_AMOUNT",
  "message": "충전 금액은 1P 이상 300,000P 이하여야 합니다.",
  "details": {
    "field": "amount",
    "rejectedValue": 0
  }
}
```

### API 목록

| 기능 | Method | URI |
| --- | --- | --- |
| 메뉴 목록 조회 | `GET` | `/api/menus` |
| 포인트 충전 | `POST` | `/api/users/{userId}/points/charges` |
| 여러 메뉴 주문 및 포인트 결제 | `POST` | `/api/users/{userId}/orders` |
| 최근 168시간 인기 메뉴 TOP 3 | `GET` | `/api/menus/popular` |

Outbox 게시와 외부 데이터 플랫폼 수신은 클라이언트 공개 API가 아니라 내부 비동기 처리 및 테스트용 Mock 영역으로 구분합니다.

### 메뉴 목록 조회

판매 중지 메뉴를 포함한 전체 메뉴를 ID 오름차순으로 반환합니다. 과제에서 메뉴 수가 제한적이고 페이지네이션 요구가 없으므로 현재는 페이지네이션을 적용하지 않습니다.

```http
GET /api/menus
```

성공 응답: `200 OK`

```json
[
  {
    "menuId": 1,
    "name": "아메리카노",
    "price": 4500,
    "status": "ON_SALE"
  },
  {
    "menuId": 2,
    "name": "카페라테",
    "price": 5000,
    "status": "STOPPED"
  }
]
```

메뉴가 없으면 `200 OK`와 빈 배열을 반환합니다.

### 포인트 충전

`userId`는 1 이상의 정수이고 `amount`는 1P 이상 300,000P 이하의 정수여야 합니다. 충전 후 잔액이 300,000P를 초과하면 전체 요청을 실패시킵니다.

```http
POST /api/users/1/points/charges
Content-Type: application/json
```

```json
{
  "amount": 10000
}
```

성공 응답: `200 OK`

```json
{
  "userId": 1,
  "chargedAmount": 10000,
  "balance": 25000,
  "chargedAt": "2026-07-14T15:30:00+09:00"
}
```

| 조건 | HTTP 상태 | 오류 코드 |
| --- | --- | --- |
| 사용자 ID 형식 오류 | `400` | `INVALID_USER_ID` |
| 충전 금액 형식·범위 오류 | `400` | `INVALID_CHARGE_AMOUNT` |
| 사용자 없음 | `404` | `USER_NOT_FOUND` |
| 충전 후 총잔액 한도 초과 | `409` | `POINT_LIMIT_EXCEEDED` |

### 여러 메뉴 주문 및 포인트 결제

`Idempotency-Key`는 공백이 아닌 1자 이상 255자 이하의 필수 헤더이며 사용자별로 유일합니다. 요청에는 메뉴 ID와 수량만 포함하고 가격은 서버가 DB에서 조회하여 계산합니다.

```http
POST /api/users/1/orders
Content-Type: application/json
Idempotency-Key: 3f83e46e-29ae-4bab-93da-c06c4396e012
```

```json
{
  "items": [
    {
      "menuId": 1,
      "quantity": 2
    },
    {
      "menuId": 2,
      "quantity": 1
    }
  ]
}
```

최초 성공 응답: `201 Created`, `Idempotency-Replayed: false`

```json
{
  "orderId": 101,
  "userId": 1,
  "status": "PAID",
  "totalAmount": 14000,
  "balanceAfter": 11000,
  "items": [
    {
      "menuId": 1,
      "menuName": "아메리카노",
      "unitPrice": 4500,
      "quantity": 2,
      "lineAmount": 9000
    },
    {
      "menuId": 2,
      "menuName": "카페라테",
      "unitPrice": 5000,
      "quantity": 1,
      "lineAmount": 5000
    }
  ],
  "paidAt": "2026-07-14T15:35:00+09:00"
}
```

같은 키와 같은 정규화 요청을 다시 보내면 새 주문이나 포인트 차감 없이 `200 OK`, `Idempotency-Replayed: true`와 기존 주문 응답을 반환합니다. 동시에 도착한 같은 요청도 선행 트랜잭션이 완료된 뒤 같은 규칙을 적용합니다. 별도의 `PROCESSING` 응답은 사용하지 않습니다.

| 조건 | HTTP 상태 | 오류 코드 |
| --- | --- | --- |
| 사용자 ID 형식 오류 | `400` | `INVALID_USER_ID` |
| 멱등 키 누락 | `400` | `IDEMPOTENCY_KEY_REQUIRED` |
| 멱등 키 길이 또는 형식 오류 | `400` | `INVALID_IDEMPOTENCY_KEY` |
| 빈 주문, 중복 메뉴 ID, 수량 오류 | `400` | `INVALID_ORDER_REQUEST` |
| 사용자 없음 | `404` | `USER_NOT_FOUND` |
| 메뉴 없음 | `404` | `MENU_NOT_FOUND` |
| 판매 중지 메뉴 포함 | `409` | `MENU_NOT_ON_SALE` |
| 포인트 부족 | `409` | `INSUFFICIENT_POINTS` |
| 같은 멱등 키와 다른 요청 | `409` | `IDEMPOTENCY_KEY_REUSED` |

### 최근 168시간 인기 메뉴 TOP 3 조회

```http
GET /api/menus/popular
```

성공 응답: `200 OK`

```json
{
  "from": "2026-07-07T15:40:00+09:00",
  "to": "2026-07-14T15:40:00+09:00",
  "items": [
    {
      "rank": 1,
      "menuId": 1,
      "menuName": "아메리카노",
      "totalQuantity": 42
    },
    {
      "rank": 2,
      "menuId": 2,
      "menuName": "카페라테",
      "totalQuantity": 31
    }
  ]
}
```

조회 시각 `T`를 한 번 얻어 `[T - 168시간, T)`를 집계하고 `from`과 `to`에도 같은 경계를 사용합니다. 메뉴명은 현재 메뉴명을 반환하며, 메뉴별 판매 수량 내림차순과 메뉴 ID 오름차순으로 순위를 결정합니다. 집계 결과가 없으면 `items`를 빈 배열로 반환합니다.

## ERD

```mermaid
erDiagram
    USERS ||--|| POINT_WALLET : owns
    POINT_WALLET ||--o{ POINT_HISTORY : records
    USERS ||--o{ ORDERS : places
    ORDERS ||--|{ ORDER_ITEM : contains
    MENU ||--o{ ORDER_ITEM : referenced_by
    ORDERS o|--|| POINT_HISTORY : creates_use_history
    ORDERS ||--|| ORDER_EVENT_OUTBOX : emits

    USERS {
        BIGINT id PK
        DATETIME created_at
    }

    POINT_WALLET {
        BIGINT user_id PK,FK
        BIGINT balance
        DATETIME updated_at
    }

    POINT_HISTORY {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR type
        BIGINT amount
        BIGINT balance_after
        BIGINT order_id FK
        DATETIME created_at
    }

    MENU {
        BIGINT id PK
        VARCHAR name
        BIGINT price
        VARCHAR status
        DATETIME created_at
        DATETIME updated_at
    }

    ORDERS {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR idempotency_key UK
        CHAR request_hash
        BIGINT total_amount
        VARCHAR status
        DATETIME created_at
        DATETIME paid_at
    }

    ORDER_ITEM {
        BIGINT id PK
        BIGINT order_id FK
        BIGINT menu_id FK
        VARCHAR menu_name
        BIGINT unit_price
        INT quantity
        BIGINT line_amount
    }

    ORDER_EVENT_OUTBOX {
        BIGINT id PK
        CHAR event_id UK
        BIGINT order_id FK
        VARCHAR event_type
        JSON payload
        VARCHAR status
        INT attempt_count
        DATETIME next_attempt_at
        VARCHAR locked_by
        DATETIME locked_at
        DATETIME published_at
        TEXT last_error
        DATETIME created_at
    }
```

## 테이블 설계

금액은 Java `long`, MySQL `BIGINT`를 사용합니다. 현재 한도는 `int` 범위 안이지만 주문 합계와 향후 정책 확장에서 발생할 수 있는 오버플로를 줄이고 도메인 전체의 금액 타입을 통일하기 위한 선택입니다.

### `users`

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK | 사용자 식별자 |
| `created_at` | `DATETIME(6)` | NOT NULL | 생성 시각 |

사용자 생성 트랜잭션에서 잔액 0P의 `point_wallet`도 함께 생성합니다. FK만으로 모든 사용자에게 지갑이 반드시 존재한다는 역방향 조건을 강제할 수 없으므로 애플리케이션 생성 흐름과 초기 데이터 검증으로 이 불변식을 보장합니다.

### `point_wallet`

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `user_id` | `BIGINT` | PK, FK | 사용자 및 지갑 식별자 |
| `balance` | `BIGINT` | NOT NULL, DEFAULT 0 | 현재 잔액 |
| `updated_at` | `DATETIME(6)` | NOT NULL | 마지막 변경 시각 |

- `CHECK (balance BETWEEN 0 AND 300000)`
- `FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT`
- 비관적 락을 사용하므로 현재 설계에는 낙관적 락용 `version` 컬럼을 두지 않습니다.

### `point_history`

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK | 이력 식별자 |
| `user_id` | `BIGINT` | FK, NOT NULL | 지갑 소유 사용자 |
| `type` | `VARCHAR(20)` | NOT NULL | `CHARGE`, `USE` |
| `amount` | `BIGINT` | NOT NULL | 충전은 양수, 사용은 음수 |
| `balance_after` | `BIGINT` | NOT NULL | 변경 직후 잔액 |
| `order_id` | `BIGINT` | FK, NULL 허용, UNIQUE | 사용 이력의 주문 |
| `created_at` | `DATETIME(6)` | NOT NULL | 잔액 변경 시각 |

- `CHARGE`이면 `amount > 0`, `order_id IS NULL`이어야 합니다.
- `USE`이면 `amount < 0`, `order_id IS NOT NULL`이어야 합니다.
- `CHECK (balance_after BETWEEN 0 AND 300000)`을 적용합니다.
- `UNIQUE (order_id)`로 한 주문에 포인트 사용 이력이 중복 생성되는 것을 방지합니다. MySQL 유니크 인덱스는 여러 `NULL`을 허용하므로 충전 이력에는 영향을 주지 않습니다.
- `FOREIGN KEY (user_id) REFERENCES point_wallet(user_id) ON DELETE RESTRICT`
- `FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT`

### `menu`

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK | 메뉴 식별자 |
| `name` | `VARCHAR(100)` | NOT NULL | 현재 메뉴명 |
| `price` | `BIGINT` | NOT NULL | 현재 판매 가격 |
| `status` | `VARCHAR(20)` | NOT NULL | `ON_SALE`, `STOPPED` |
| `created_at` | `DATETIME(6)` | NOT NULL | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NOT NULL | 수정 시각 |

- `CHECK (price >= 1)`
- `CHECK (status IN ('ON_SALE', 'STOPPED'))`

### `orders`

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK | 주문 식별자 |
| `user_id` | `BIGINT` | FK, NOT NULL | 주문 사용자 |
| `idempotency_key` | `VARCHAR(255)` | NOT NULL | 주문 멱등 키 |
| `request_hash` | `CHAR(64)` | NOT NULL | canonical request의 SHA-256 |
| `total_amount` | `BIGINT` | NOT NULL | 서버가 계산한 주문 총액 |
| `status` | `VARCHAR(20)` | NOT NULL | 현재 범위에서는 `PAID` |
| `created_at` | `DATETIME(6)` | NOT NULL | 주문 생성 시각 |
| `paid_at` | `DATETIME(6)` | NOT NULL | 결제 완료 시각 |

- `UNIQUE (user_id, idempotency_key)`
- `CHECK (total_amount >= 1)`
- `CHECK (status = 'PAID')`
- `FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT`
- `idempotency_key`에는 대소문자를 구분하는 `utf8mb4_bin` 계열 collation을 적용합니다.
- 현재 주문 트랜잭션은 완료 상태만 커밋하므로 `PENDING`을 저장하지 않습니다. 취소 기능이 추가되면 상태 모델을 확장합니다.

### `order_item`

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK | 주문 항목 식별자 |
| `order_id` | `BIGINT` | FK, NOT NULL | 주문 식별자 |
| `menu_id` | `BIGINT` | FK, NOT NULL | 원본 메뉴 식별자 |
| `menu_name` | `VARCHAR(100)` | NOT NULL | 주문 당시 메뉴명 |
| `unit_price` | `BIGINT` | NOT NULL | 주문 당시 단가 |
| `quantity` | `INT` | NOT NULL | 주문 수량 |
| `line_amount` | `BIGINT` | NOT NULL | 단가와 수량을 곱한 금액 |

- `UNIQUE (order_id, menu_id)`
- `CHECK (unit_price >= 1)`
- `CHECK (quantity >= 1)`
- `CHECK (line_amount = unit_price * quantity)`
- `FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT`
- `FOREIGN KEY (menu_id) REFERENCES menu(id) ON DELETE RESTRICT`

### `order_event_outbox`

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK | 내부 식별자 |
| `event_id` | `CHAR(36)` | UNIQUE, NOT NULL | 소비자 중복 제거용 UUID |
| `order_id` | `BIGINT` | FK, NOT NULL | 주문 식별자 |
| `event_type` | `VARCHAR(50)` | NOT NULL | `ORDER_COMPLETED` |
| `payload` | `JSON` | NOT NULL | 외부 전송 데이터 |
| `status` | `VARCHAR(20)` | NOT NULL | `PENDING`, `PROCESSING`, `PUBLISHED`, `FAILED` |
| `attempt_count` | `INT` | NOT NULL, DEFAULT 0 | 실제 전송 시도 횟수 |
| `next_attempt_at` | `DATETIME(6)` | NOT NULL | 다음 재시도 가능 시각 |
| `locked_by` | `VARCHAR(100)` | NULL 허용 | 선점한 게시자 식별자 |
| `locked_at` | `DATETIME(6)` | NULL 허용 | 선점 시각 |
| `published_at` | `DATETIME(6)` | NULL 허용 | 전송 완료 시각 |
| `last_error` | `TEXT` | NULL 허용 | 마지막 실패 원인 |
| `created_at` | `DATETIME(6)` | NOT NULL | 이벤트 생성 시각 |

- `UNIQUE (event_id)`
- `UNIQUE (order_id, event_type)`
- `CHECK (attempt_count BETWEEN 0 AND 6)`
- `FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT`

게시자는 짧은 DB 트랜잭션에서 다음 조건의 이벤트를 `FOR UPDATE SKIP LOCKED`로 조회하여 다중 인스턴스 간 중복 선점을 방지합니다.

```sql
SELECT id
FROM order_event_outbox
WHERE status = 'PENDING'
  AND next_attempt_at <= UTC_TIMESTAMP(6)
ORDER BY created_at, id
LIMIT :batch_size
FOR UPDATE SKIP LOCKED;
```

선점한 행을 `PROCESSING`으로 변경하고 `locked_by`, `locked_at`을 기록한 뒤 트랜잭션을 커밋합니다. 외부 API는 커밋 후 호출합니다. 게시자가 중단되어 `PROCESSING` 상태가 오래 유지되면 lease timeout을 기준으로 다시 `PENDING`으로 회수합니다.

외부 플랫폼이 이벤트를 수신한 직후 게시자가 종료되면 같은 이벤트가 다시 전송될 수 있습니다. 따라서 게시자는 매번 같은 `event_id`를 사용하고, Mock API 테스트에서도 소비자의 중복 제거를 검증합니다.

## 인덱스

| 테이블 | 인덱스 | 목적 |
| --- | --- | --- |
| `orders` | `UNIQUE (user_id, idempotency_key)` | 사용자별 멱등성 보장 |
| `orders` | `INDEX (status, paid_at, id)` | 최근 168시간 결제 주문 범위 탐색 |
| `order_item` | `UNIQUE (order_id, menu_id)` | 주문 내 메뉴 중복 방지 |
| `order_item` | `INDEX (order_id, menu_id, quantity)` | 인기 메뉴 조인 및 수량 집계 |
| `point_history` | `UNIQUE (order_id)` | 주문별 중복 차감 방지 |
| `point_history` | `INDEX (user_id, created_at, id)` | 사용자 포인트 이력 조회 |
| `order_event_outbox` | `UNIQUE (event_id)` | 이벤트 식별자 중복 방지 |
| `order_event_outbox` | `UNIQUE (order_id, event_type)` | 주문 이벤트 중복 생성 방지 |
| `order_event_outbox` | `INDEX (status, next_attempt_at, created_at, id)` | 전송 대상 배치 조회 |
| `order_event_outbox` | `INDEX (status, locked_at)` | 만료된 `PROCESSING` 이벤트 회수 |

실제 인덱스 사용 여부는 MySQL 실행 계획과 통합 테스트 데이터로 확인하고, 중복되거나 사용되지 않는 인덱스는 구현 단계에서 조정합니다.

## 시간 저장 기준

- 애플리케이션에서는 `Instant`를 사용합니다.
- DB에는 UTC 기준 `DATETIME(6)`으로 저장합니다.
- JDBC와 Hibernate의 시간대도 UTC로 고정합니다.
- 인기 메뉴 조회는 주입된 `Clock`에서 `T`를 한 번 얻고 `T - 168시간`과 `T`를 UTC 값으로 변환하여 쿼리에 전달합니다.
- `Asia/Seoul`은 API 표현과 정책 설명의 기준으로 사용하되 DB 서버나 세션의 암묵적인 시간대 변환에는 의존하지 않습니다.
- UTC 저장은 서버 위치와 세션 설정이 달라져도 주문, 로그와 Outbox 시각을 동일한 순간으로 비교하기 위한 선택입니다. 사용자에게 UTC를 노출하는 것이 아니라 API 경계에서 `Asia/Seoul`로 변환하여 `+09:00` 오프셋과 함께 반환합니다.
- 향후 한국 날짜 단위 집계가 필요하면 `Asia/Seoul`에서 시작과 종료 경계를 계산한 뒤 UTC로 변환하여 조회합니다.

## 문제 해결 전략 및 기술적 선택 이유

| 문제 | 선택한 전략 | 검토한 대안 | 선택 이유 | 트레이드오프 및 보완 |
| --- | --- | --- | --- | --- |
| 다중 서버 포인트 동시성 | DB 비관적 락 `SELECT ... FOR UPDATE` | JVM `synchronized`, 낙관적 락 | 모든 서버가 공유하는 지갑 행을 잠가 검증과 갱신을 직렬화하고 잔액 음수를 방지합니다. | 동일 사용자 요청은 대기하므로 트랜잭션을 짧게 유지하고 외부 API를 락 밖에서 호출합니다. |
| 주문 원자성 | 주문, 항목, 포인트 차감, 이력, Outbox를 한 DB 트랜잭션으로 저장 | 단계별 별도 저장과 보상 처리 | 중간 실패 시 일부 데이터만 남는 상태를 DB 롤백으로 방지합니다. | 트랜잭션 범위가 넓어질 수 있어 네트워크 호출은 포함하지 않습니다. |
| 주문 멱등성 | `orders`에 키와 요청 해시 저장, DB 유니크 제약 | 별도 멱등성 테이블, 애플리케이션 선조회만 사용 | 주문과 멱등 결과가 같은 트랜잭션 생명주기를 가져 구조가 단순하며 유니크 제약이 동시 요청의 최종 방어선이 됩니다. | 처리 중 상태와 실패 응답을 별도로 저장하기 어렵습니다. 현재는 선행 트랜잭션 완료까지 대기하고 커밋 결과만 재사용합니다. |
| 요청 동일성 판단 | 메뉴 ID 정렬 후 canonical payload의 SHA-256 저장 | 원본 JSON 문자열 비교 | JSON 필드나 메뉴 순서가 달라도 의미가 같은 요청을 동일하게 판단합니다. | canonical 규칙이 바뀌면 호환성 문제가 생기므로 규칙을 테스트로 고정합니다. |
| 외부 데이터 전송 | Transactional Outbox | 주문 트랜잭션 안에서 직접 호출, 커밋 후 메모리 작업 큐 | 주문과 이벤트 저장의 원자성을 지키면서 외부 장애를 주문 성공과 분리합니다. | 게시자, 재시도와 정체 이벤트 모니터링이 추가로 필요합니다. |
| 이벤트 전달 보장 | at-least-once와 `eventId` 중복 제거 | exactly-once 전달 | 네트워크 단절 시 전송 성공 여부를 완전히 알 수 없으므로 재전송을 허용하는 방식이 현실적입니다. | 소비자가 같은 `eventId`를 멱등 처리해야 하며 Mock API 테스트로 검증합니다. |
| Outbox 다중 인스턴스 선점 | `FOR UPDATE SKIP LOCKED`와 짧은 선점 트랜잭션 | 분산 락, 인스턴스별 고정 파티션 | 별도 인프라 없이 여러 게시자가 서로 잠긴 행을 건너뛰며 병렬 처리할 수 있습니다. | DB 지원 여부에 의존하며 오래된 `PROCESSING` 이벤트를 lease timeout으로 회수해야 합니다. |
| 금액 타입 | Java `long`, MySQL `BIGINT` | `int`, 소수 타입 | 정수 포인트 정책에 맞고 합계 계산의 오버플로 여유와 도메인 타입 일관성을 확보합니다. | 현재 한도보다 넓은 타입이지만 DB `CHECK`와 애플리케이션 검증으로 정책 범위를 제한합니다. |
| 주문 가격 보존 | `order_item`에 메뉴명과 가격 스냅샷 저장 | 조회 시 현재 `menu`만 조인 | 메뉴 정보가 바뀌어도 주문 당시 금액과 표시 내용을 재현할 수 있습니다. | 데이터가 중복되지만 주문 이력의 불변성과 추적 가능성을 우선합니다. |
| 시간 저장과 표현 | 내부·DB UTC, API `Asia/Seoul` | DB에 한국시간 직접 저장 | 동일한 순간을 명확하게 비교하면서 한국 사용자에게 자연스러운 시간을 제공합니다. | API 경계 변환이 필요하므로 공통 직렬화 설정과 시간 경계 테스트를 둡니다. |
| 현재 시각 취득 | `Clock` 주입 | 서비스 내부에서 `Instant.now()` 직접 호출 | 최근 168시간 경계를 테스트에서 고정하여 시작 포함·종료 제외 조건을 재현할 수 있습니다. | 생성자 의존성이 하나 늘지만 테스트 결정성을 얻습니다. |
| API 경로 | `/api` 사용 | `/api/v1` URL 버전 | 현재 단일 과제 API를 간결하게 표현합니다. | 향후 호환되지 않는 변경이 생기면 별도 버전 전략이 필요합니다. |

## 주요 오류 정책

| 상황 | HTTP 상태 | 오류 코드 예시 |
| --- | --- | --- |
| 사용자 또는 메뉴 없음 | 404 | `USER_NOT_FOUND`, `MENU_NOT_FOUND` |
| 사용자 ID 형식 오류 | 400 | `INVALID_USER_ID` |
| 빈 주문, 중복 메뉴, 수량 오류 | 400 | `INVALID_ORDER_REQUEST` |
| 충전 금액 오류 | 400 | `INVALID_CHARGE_AMOUNT` |
| 멱등 키 누락 | 400 | `IDEMPOTENCY_KEY_REQUIRED` |
| 멱등 키 길이 또는 형식 오류 | 400 | `INVALID_IDEMPOTENCY_KEY` |
| 판매 중지 메뉴 | 409 | `MENU_NOT_ON_SALE` |
| 포인트 총잔액 한도 초과 | 409 | `POINT_LIMIT_EXCEEDED` |
| 포인트 부족 | 409 | `INSUFFICIENT_POINTS` |
| 같은 멱등 키의 다른 요청 | 409 | `IDEMPOTENCY_KEY_REUSED` |

외부 플랫폼 전송 실패는 완료된 주문의 HTTP 응답이나 DB 상태를 롤백하지 않습니다.

## 다음 단계

1. API 명세와 동시성·트랜잭션·Outbox 상세 흐름 최종 검토
2. Spring Boot 프로젝트 기본 구조 구성
3. 기능별 구현과 단위·통합·동시성 테스트
4. MySQL과 Testcontainers 기반 최종 검증
5. 실행 방법, 테스트 전략과 트러블슈팅을 포함한 제출용 README 완성
