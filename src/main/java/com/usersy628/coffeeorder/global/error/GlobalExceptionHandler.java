package com.usersy628.coffeeorder.global.error;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.usersy628.coffeeorder.global.trace.TraceIdFilter;
import com.usersy628.coffeeorder.order.api.OrderCreateRequest;
import com.usersy628.coffeeorder.order.application.OrderRetryFailureException;
import com.usersy628.coffeeorder.point.api.PointChargeRequest;
import com.usersy628.coffeeorder.point.application.PointChargeRetryFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

	@ExceptionHandler(OrderRetryFailureException.class)
	public ResponseEntity<ApiErrorResponse> handleOrderRetryFailure(OrderRetryFailureException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		String traceId = currentTraceId();
		String causeType = exception.getCause().getClass().getSimpleName();
		log.warn(
			"Order retry failed code={} attempts={} causeType={} traceId={}",
			errorCode.name(),
			exception.getAttemptCount(),
			causeType,
			traceId
		);

		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(ApiErrorResponse.from(errorCode, exception.getDetails(), traceId));
	}

	@ExceptionHandler(PointChargeRetryFailureException.class)
	public ResponseEntity<ApiErrorResponse> handlePointChargeRetryFailure(
		PointChargeRetryFailureException exception
	) {
		ErrorCode errorCode = exception.getErrorCode();
		String traceId = currentTraceId();
		String causeType = exception.getCause().getClass().getSimpleName();
		log.warn(
			"Point charge retry failed code={} attempts={} causeType={} traceId={}",
			errorCode.name(),
			exception.getAttemptCount(),
			causeType,
			traceId
		);

		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(ApiErrorResponse.from(errorCode, exception.getDetails(), traceId));
	}

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
		if (invalidUserId) {
			return handleExpectedClientError(ErrorCode.INVALID_USER_ID);
		}

		boolean invalidIdempotencyKey = exception.getParameterValidationResults().stream()
			.anyMatch(result -> "idempotencyKey".equals(result.getMethodParameter().getParameterName()));
		if (invalidIdempotencyKey) {
			return handleExpectedClientError(ErrorCode.INVALID_IDEMPOTENCY_KEY);
		}

		boolean invalidOrderRequest = exception.getParameterValidationResults().stream()
			.anyMatch(result -> OrderCreateRequest.class.equals(result.getMethodParameter().getParameterType()));
		if (invalidOrderRequest) {
			return handleExpectedClientError(ErrorCode.INVALID_ORDER_REQUEST);
		}

		PointChargeRequest invalidChargeRequest = exception.getParameterValidationResults().stream()
			.filter(result -> PointChargeRequest.class.equals(result.getMethodParameter().getParameterType()))
			.map(result -> (PointChargeRequest) result.getArgument())
			.findFirst()
			.orElse(null);
		if (invalidChargeRequest != null) {
			Map<String, Object> fieldError = new LinkedHashMap<>();
			fieldError.put("field", "amount");
			fieldError.put("reason", ErrorCode.INVALID_CHARGE_AMOUNT.getMessage());
			if (invalidChargeRequest.amount() != null) {
				fieldError.put("rejectedValue", invalidChargeRequest.amount());
			}
			return handleExpectedClientError(
				ErrorCode.INVALID_CHARGE_AMOUNT,
				Map.of("fieldErrors", List.of(Map.copyOf(fieldError)))
			);
		}

		return handleUnexpectedException(exception);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleRequestValidation(MethodArgumentNotValidException exception) {
		Object target = exception.getBindingResult().getTarget();
		if (!(target instanceof PointChargeRequest) && !(target instanceof OrderCreateRequest)) {
			return handleUnexpectedException(exception);
		}
		List<Map<String, Object>> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
			.map(fieldError -> {
				Map<String, Object> detail = new LinkedHashMap<>();
				detail.put("field", fieldError.getField());
				detail.put("reason", fieldError.getDefaultMessage());
				if (fieldError.getRejectedValue() != null) {
					detail.put("rejectedValue", fieldError.getRejectedValue());
				}
				return Map.copyOf(detail);
			})
			.toList();
		String traceId = currentTraceId();
		ErrorCode errorCode = target instanceof OrderCreateRequest
			? ErrorCode.INVALID_ORDER_REQUEST
			: ErrorCode.INVALID_CHARGE_AMOUNT;
		log.info("Handled request validation error code={} traceId={}", errorCode.name(), traceId);

		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(ApiErrorResponse.from(errorCode, Map.of("fieldErrors", fieldErrors), traceId));
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

	@ExceptionHandler(DataAccessResourceFailureException.class)
	public ResponseEntity<ApiErrorResponse> handleInfrastructureUnavailable(
		DataAccessResourceFailureException exception
	) {
		ErrorCode errorCode = ErrorCode.SERVICE_UNAVAILABLE;
		String traceId = currentTraceId();
		log.warn(
			"Transient infrastructure error code={} causeType={} traceId={}",
			errorCode.name(),
			exception.getClass().getSimpleName(),
			traceId
		);

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
		return handleExpectedClientError(errorCode, Map.of());
	}

	private ResponseEntity<ApiErrorResponse> handleExpectedClientError(
		ErrorCode errorCode,
		Map<String, Object> details
	) {
		String traceId = currentTraceId();
		log.info("Handled client error code={} traceId={}", errorCode.name(), traceId);

		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(ApiErrorResponse.from(errorCode, details, traceId));
	}

	private String currentTraceId() {
		String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
		return traceId == null ? "unavailable" : traceId;
	}
}
