package com.usersy628.coffeeorder.point.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "point_history")
public class PointHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PointHistoryType type;

	@Column(nullable = false)
	private long amount;

	@Column(name = "balance_after", nullable = false)
	private long balanceAfter;

	@Column(name = "order_id")
	private Long orderId;

	@Column(name = "idempotency_key", length = 255)
	private String idempotencyKey;

	@Column(name = "request_hash", columnDefinition = "CHAR(64)")
	private String requestHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected PointHistory() {
	}

	private PointHistory(
		long userId,
		long amount,
		long balanceAfter,
		String idempotencyKey,
		String requestHash,
		Instant createdAt
	) {
		this.userId = userId;
		this.type = PointHistoryType.CHARGE;
		this.amount = amount;
		this.balanceAfter = balanceAfter;
		this.idempotencyKey = idempotencyKey;
		this.requestHash = requestHash;
		this.createdAt = createdAt;
	}

	public static PointHistory charge(
		long userId,
		long amount,
		long balanceAfter,
		String idempotencyKey,
		String requestHash,
		Instant createdAt
	) {
		return new PointHistory(userId, amount, balanceAfter, idempotencyKey, requestHash, createdAt);
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public PointHistoryType getType() {
		return type;
	}

	public long getAmount() {
		return amount;
	}

	public long getBalanceAfter() {
		return balanceAfter;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public String getRequestHash() {
		return requestHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
