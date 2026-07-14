package com.usersy628.coffeeorder.point.application;

public record PointChargeCommand(long userId, long amount, String idempotencyKey) {
}
