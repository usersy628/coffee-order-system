package com.usersy628.coffeeorder.global.error;

import java.util.Map;

import com.usersy628.coffeeorder.global.trace.TraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

	@ExceptionHandler(DomainException.class)
	public ResponseEntity<ApiErrorResponse> handleDomainException(DomainException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		String traceId = currentTraceId();
		log.info("Handled domain error code={} traceId={}", errorCode.name(), traceId);

		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(ApiErrorResponse.from(errorCode, exception.getDetails(), traceId));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpMessageNotReadableException exception) {
		ErrorCode errorCode = ErrorCode.MALFORMED_JSON;
		String traceId = currentTraceId();
		log.info("Handled malformed JSON traceId={}", traceId);

		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(ApiErrorResponse.from(errorCode, Map.of(), traceId));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleArgumentTypeMismatch(
		MethodArgumentTypeMismatchException exception
	) {
		if (!"userId".equals(exception.getName())) {
			return handleUnexpectedException(exception);
		}

		return handleExpectedClientError(ErrorCode.INVALID_USER_ID);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodValidation(
		HandlerMethodValidationException exception
	) {
		boolean invalidUserId = exception.getParameterValidationResults().stream()
			.anyMatch(result -> "userId".equals(result.getMethodParameter().getParameterName()));
		if (!invalidUserId) {
			return handleUnexpectedException(exception);
		}

		return handleExpectedClientError(ErrorCode.INVALID_USER_ID);
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<ApiErrorResponse> handleMissingRequestHeader(
		MissingRequestHeaderException exception
	) {
		if (!IDEMPOTENCY_KEY_HEADER.equalsIgnoreCase(exception.getHeaderName())) {
			return handleUnexpectedException(exception);
		}

		return handleExpectedClientError(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
		HttpMediaTypeNotSupportedException exception
	) {
		return handleExpectedClientError(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
	}

	@ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
	public ResponseEntity<ApiErrorResponse> handleEndpointNotFound(Exception exception) {
		ErrorCode errorCode = ErrorCode.ENDPOINT_NOT_FOUND;
		String traceId = currentTraceId();
		log.info("Handled missing endpoint traceId={}", traceId);

		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(ApiErrorResponse.from(errorCode, Map.of(), traceId));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
		String traceId = currentTraceId();
		log.error("Unhandled server error traceId={}", traceId, exception);

		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(ApiErrorResponse.from(errorCode, Map.of(), traceId));
	}

	private ResponseEntity<ApiErrorResponse> handleExpectedClientError(ErrorCode errorCode) {
		String traceId = currentTraceId();
		log.info("Handled client error code={} traceId={}", errorCode.name(), traceId);

		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(ApiErrorResponse.from(errorCode, Map.of(), traceId));
	}

	private String currentTraceId() {
		String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
		return traceId == null ? "unavailable" : traceId;
	}
}
