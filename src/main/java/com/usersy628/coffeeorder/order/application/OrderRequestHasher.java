package com.usersy628.coffeeorder.order.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class OrderRequestHasher {

	public String hash(OrderCommand command) {
		String canonical = command.items().stream()
			.sorted((left, right) -> Long.compare(left.menuId(), right.menuId()))
			.map(item -> item.menuId() + ":" + item.quantity())
			.reduce((left, right) -> left + "," + right)
			.orElse("");
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(canonical.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
}
