package com.usersy628.coffeeorder.point.domain;

import java.time.Instant;

import com.usersy628.coffeeorder.global.error.DomainException;
import com.usersy628.coffeeorder.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "point_wallet")
public class PointWallet {

	private static final long MAX_BALANCE = 300_000L;

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Column(nullable = false)
	private long balance;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PointWallet() {
	}

	public long charge(long amount, Instant chargedAt) {
		if (amount < 1 || amount > MAX_BALANCE) {
			throw new DomainException(ErrorCode.INVALID_CHARGE_AMOUNT);
		}
		if (amount > MAX_BALANCE - balance) {
			throw new DomainException(ErrorCode.POINT_LIMIT_EXCEEDED);
		}
		balance += amount;
		updatedAt = chargedAt;
		return balance;
	}

	public Long getUserId() {
		return userId;
	}

	public long getBalance() {
		return balance;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
