package com.usersy628.coffeeorder.point.api;

import com.usersy628.coffeeorder.point.application.PointChargeCommand;
import com.usersy628.coffeeorder.point.application.PointChargeResult;
import com.usersy628.coffeeorder.point.application.PointChargeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/points")
public class PointChargeController {

	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final String IDEMPOTENCY_REPLAYED_HEADER = "Idempotency-Replayed";

	private final PointChargeService pointChargeService;

	public PointChargeController(PointChargeService pointChargeService) {
		this.pointChargeService = pointChargeService;
	}

	@PostMapping(value = "/charges", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PointChargeResponse> charge(
		@PathVariable @Min(1) long userId,
		@RequestHeader(IDEMPOTENCY_KEY_HEADER) @NotBlank @Size(max = 255) String idempotencyKey,
		@Valid @RequestBody PointChargeRequest request
	) {
		PointChargeResult result = pointChargeService.charge(
			new PointChargeCommand(userId, request.amount(), idempotencyKey)
		);

		return ResponseEntity.ok()
			.header(IDEMPOTENCY_REPLAYED_HEADER, Boolean.toString(result.replayed()))
			.body(PointChargeResponse.from(result));
	}
}
