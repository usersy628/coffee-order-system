package com.usersy628.coffeeorder.point.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class PointChargeRequestHasher {

	public String hash(long amount) {
		String canonicalPayload = "{\"amount\":" + amount + "}";
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 must be available", exception);
		}
	}
}
