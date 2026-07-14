package com.usersy628.coffeeorder.global.error;

import java.util.Map;

import com.usersy628.coffeeorder.global.trace.TraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

	private String currentTraceId() {
		String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
		return traceId == null ? "unavailable" : traceId;
	}
}
