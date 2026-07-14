package com.usersy628.coffeeorder.order.application;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usersy628.coffeeorder.global.error.DomainException;
import com.usersy628.coffeeorder.global.error.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderTransactionExecutor {

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public OrderTransactionExecutor(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Transactional(timeout = 5, propagation = Propagation.REQUIRES_NEW)
	public OrderResult execute(OrderCommand command, String requestHash) {
		List<OrderCommand.Item> requestedItems = command.items().stream()
			.sorted(Comparator.comparingLong(OrderCommand.Item::menuId))
			.toList();
		long balance = lockWallet(command.userId());

		Optional<OrderResult> existing = findExisting(command.userId(), command.idempotencyKey(), requestHash);
		if (existing.isPresent()) {
			return existing.get();
		}
		List<MenuRow> menus = findMenus(requestedItems);
		if (menus.size() != requestedItems.size()) {
			throw new DomainException(ErrorCode.MENU_NOT_FOUND);
		}

		List<OrderResult.Item> items = new ArrayList<>();
		long totalAmount = 0;
		for (int index = 0; index < requestedItems.size(); index++) {
			OrderCommand.Item requested = requestedItems.get(index);
			MenuRow menu = menus.get(index);
			if (menu.id() != requested.menuId()) {
				throw new DomainException(ErrorCode.MENU_NOT_FOUND);
			}
			if (!"ON_SALE".equals(menu.status())) {
				throw new DomainException(ErrorCode.MENU_NOT_ON_SALE);
			}
			long lineAmount = Math.multiplyExact(menu.price(), requested.quantity());
			totalAmount = Math.addExact(totalAmount, lineAmount);
			items.add(new OrderResult.Item(menu.id(), menu.name(), menu.price(), requested.quantity(), lineAmount));
		}
		if (balance < totalAmount) {
			throw new DomainException(ErrorCode.INSUFFICIENT_POINTS);
		}

		Instant occurredAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
		long balanceAfter = balance - totalAmount;
		long orderId = insertOrder(command, requestHash, totalAmount, occurredAt);
		insertItems(orderId, items);
		jdbcTemplate.update(
			"UPDATE point_wallet SET balance = ?, updated_at = ? WHERE user_id = ?",
			balanceAfter, Timestamp.from(occurredAt), command.userId()
		);
		jdbcTemplate.update("""
			INSERT INTO point_history (user_id, type, amount, balance_after, order_id, created_at)
			VALUES (?, 'USE', ?, ?, ?, ?)
			""", command.userId(), -totalAmount, balanceAfter, orderId, Timestamp.from(occurredAt));
		insertOutbox(orderId, command.userId(), totalAmount, items, occurredAt);
		return new OrderResult(orderId, command.userId(), "PAID", totalAmount, balanceAfter, items, occurredAt, false);
	}

	private List<MenuRow> findMenus(List<OrderCommand.Item> items) {
		String placeholders = String.join(",", items.stream().map(item -> "?").toList());
		Object[] ids = items.stream().map(OrderCommand.Item::menuId).toArray();
		return jdbcTemplate.query(
			"SELECT id, name, price, status FROM menu WHERE id IN (" + placeholders + ") ORDER BY id",
			(rs, rowNum) -> new MenuRow(rs.getLong("id"), rs.getString("name"), rs.getLong("price"), rs.getString("status")),
			ids
		);
	}

	private long lockWallet(long userId) {
		List<Long> balances = jdbcTemplate.query(
			"SELECT balance FROM point_wallet WHERE user_id = ? FOR UPDATE",
			(rs, rowNum) -> rs.getLong("balance"), userId
		);
		if (balances.isEmpty()) {
			throw new DomainException(ErrorCode.USER_NOT_FOUND);
		}
		return balances.get(0);
	}

	private Optional<OrderResult> findExisting(long userId, String key, String requestHash) {
		List<OrderRow> orders = jdbcTemplate.query("""
			SELECT id, request_hash, total_amount, status, paid_at
			FROM orders WHERE user_id = ? AND idempotency_key = ? FOR UPDATE
			""", (rs, rowNum) -> new OrderRow(
			rs.getLong("id"), rs.getString("request_hash"), rs.getLong("total_amount"),
			rs.getString("status"), rs.getTimestamp("paid_at").toInstant()
		), userId, key);
		if (orders.isEmpty()) {
			return Optional.empty();
		}
		OrderRow order = orders.get(0);
		if (!order.requestHash().equals(requestHash)) {
			throw new DomainException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
		}
		List<OrderResult.Item> items = jdbcTemplate.query("""
			SELECT menu_id, menu_name, unit_price, quantity, line_amount
			FROM order_item WHERE order_id = ? ORDER BY menu_id
			""", (rs, rowNum) -> new OrderResult.Item(
			rs.getLong("menu_id"), rs.getString("menu_name"), rs.getLong("unit_price"),
			rs.getInt("quantity"), rs.getLong("line_amount")
		), order.id());
		Long balanceAfter = jdbcTemplate.queryForObject(
			"SELECT balance_after FROM point_history WHERE order_id = ?", Long.class, order.id());
		return Optional.of(new OrderResult(
			order.id(), userId, order.status(), order.totalAmount(), balanceAfter, items, order.paidAt(), true
		));
	}

	private long insertOrder(OrderCommand command, String requestHash, long totalAmount, Instant occurredAt) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO orders
				(user_id, idempotency_key, request_hash, total_amount, status, created_at, paid_at)
				VALUES (?, ?, ?, ?, 'PAID', ?, ?)
				""", Statement.RETURN_GENERATED_KEYS);
			statement.setLong(1, command.userId());
			statement.setString(2, command.idempotencyKey());
			statement.setString(3, requestHash);
			statement.setLong(4, totalAmount);
			statement.setTimestamp(5, Timestamp.from(occurredAt));
			statement.setTimestamp(6, Timestamp.from(occurredAt));
			return statement;
		}, keyHolder);
		if (keyHolder.getKey() == null) {
			throw new IllegalStateException("Order id was not generated");
		}
		return keyHolder.getKey().longValue();
	}

	private void insertItems(long orderId, List<OrderResult.Item> items) {
		for (OrderResult.Item item : items) {
			jdbcTemplate.update("""
				INSERT INTO order_item (order_id, menu_id, menu_name, unit_price, quantity, line_amount)
				VALUES (?, ?, ?, ?, ?, ?)
				""", orderId, item.menuId(), item.menuName(), item.unitPrice(), item.quantity(), item.lineAmount());
		}
	}

	private void insertOutbox(
		long orderId, long userId, long totalAmount, List<OrderResult.Item> items, Instant occurredAt
	) {
		String eventId = UUID.randomUUID().toString();
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("eventId", eventId);
		payload.put("eventType", "ORDER_COMPLETED");
		payload.put("schemaVersion", 1);
		payload.put("occurredAt", occurredAt.toString());
		payload.put("orderId", orderId);
		payload.put("data", Map.of("userId", userId, "totalAmount", totalAmount, "items", items));
		try {
			jdbcTemplate.update("""
				INSERT INTO order_event_outbox
				(event_id, order_id, event_type, schema_version, payload, status, attempt_count,
				 next_attempt_at, created_at)
				VALUES (?, ?, 'ORDER_COMPLETED', 1, ?, 'PENDING', 0, UTC_TIMESTAMP(6), ?)
				""", eventId, orderId, objectMapper.writeValueAsString(payload), Timestamp.from(occurredAt));
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize order event", exception);
		}
	}

	private record MenuRow(long id, String name, long price, String status) {
	}

	private record OrderRow(long id, String requestHash, long totalAmount, String status, Instant paidAt) {
	}
}
