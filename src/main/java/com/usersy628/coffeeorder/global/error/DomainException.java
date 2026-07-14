package com.usersy628.coffeeorder.global.error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class DomainException extends RuntimeException {

	private final ErrorCode errorCode;
	private final Map<String, Object> details;

	public DomainException(ErrorCode errorCode) {
		this(errorCode, Map.of());
	}

	public DomainException(ErrorCode errorCode, Map<String, Object> details) {
		super(Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage());
		this.errorCode = errorCode;
		this.details = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(details, "details must not be null")));
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public Map<String, Object> getDetails() {
		return details;
	}
}
