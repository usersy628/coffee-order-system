package com.usersy628.coffeeorder.global.error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ApiErrorResponse(
	String code,
	String message,
	Map<String, Object> details,
	String traceId
) {

	public ApiErrorResponse {
		Objects.requireNonNull(code, "code must not be null");
		Objects.requireNonNull(message, "message must not be null");
		Objects.requireNonNull(details, "details must not be null");
		Objects.requireNonNull(traceId, "traceId must not be null");
		details = Map.copyOf(new LinkedHashMap<>(details));
	}

	public static ApiErrorResponse from(ErrorCode errorCode, Map<String, Object> details, String traceId) {
		return new ApiErrorResponse(errorCode.name(), errorCode.getMessage(), details, traceId);
	}
}
