package com.usersy628.coffeeorder.order.api;

import java.net.URI;

import com.usersy628.coffeeorder.order.application.OrderCommand;
import com.usersy628.coffeeorder.order.application.OrderResult;
import com.usersy628.coffeeorder.order.application.OrderService;
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
@RequestMapping("/api/users/{userId}/orders")
public class OrderController {

	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final String IDEMPOTENCY_REPLAYED_HEADER = "Idempotency-Replayed";

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderCreateResponse> create(
		@PathVariable @Min(1) long userId,
		@RequestHeader(IDEMPOTENCY_KEY_HEADER) @NotBlank @Size(max = 255) String idempotencyKey,
		@Valid @RequestBody OrderCreateRequest request
	) {
		OrderCommand command = new OrderCommand(
			userId,
			request.items().stream()
				.map(item -> new OrderCommand.Item(item.menuId(), Math.toIntExact(item.quantity())))
				.toList(),
			idempotencyKey
		);
		OrderResult result = orderService.create(command);
		OrderCreateResponse response = OrderCreateResponse.from(result);
		if (result.replayed()) {
			return ResponseEntity.ok()
				.header(IDEMPOTENCY_REPLAYED_HEADER, "true")
				.body(response);
		}
		return ResponseEntity.created(URI.create("/api/users/" + userId + "/orders/" + result.orderId()))
			.header(IDEMPOTENCY_REPLAYED_HEADER, "false")
			.body(response);
	}
}
