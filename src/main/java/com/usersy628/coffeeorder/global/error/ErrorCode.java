package com.usersy628.coffeeorder.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	MALFORMED_JSON(HttpStatus.BAD_REQUEST, "요청 본문 형식이 올바르지 않습니다."),
	INVALID_USER_ID(HttpStatus.BAD_REQUEST, "사용자 ID는 1 이상의 정수여야 합니다."),
	IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "Idempotency-Key 헤더는 필수입니다."),
	INVALID_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "Idempotency-Key 헤더는 공백이 아닌 1자 이상 255자 이하여야 합니다."),
	INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "충전 금액은 1P 이상 300,000P 이하여야 합니다."),
	INVALID_ORDER_REQUEST(HttpStatus.BAD_REQUEST, "주문 항목 형식이 올바르지 않습니다."),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type입니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다."),
	ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),
	POINT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "충전 후 총잔액은 300,000P를 초과할 수 없습니다."),
	MENU_NOT_ON_SALE(HttpStatus.CONFLICT, "판매 중지된 메뉴는 주문할 수 없습니다."),
	INSUFFICIENT_POINTS(HttpStatus.CONFLICT, "포인트 잔액이 부족합니다."),
	IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT, "같은 Idempotency-Key를 다른 요청에 사용할 수 없습니다."),
	CONCURRENT_REQUEST_TIMEOUT(HttpStatus.SERVICE_UNAVAILABLE, "동시 요청 처리에 실패했습니다. 같은 Idempotency-Key로 다시 시도해 주세요."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final String message;

	ErrorCode(HttpStatus httpStatus, String message) {
		this.httpStatus = httpStatus;
		this.message = message;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public String getMessage() {
		return message;
	}
}
