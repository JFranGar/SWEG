package com.cleancodecrew.sweg.config;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PasswordHasher
 *
 * Propósito: Hash SHA-256 con sal para contraseñas.
 * Sprint 1 - HU1: Hash SHA-256 + salt.
 */
@Component
public class PasswordHasher {

	private static final int SALT_BYTES = 16;

	public String hash(String plain) {
		try {
			byte[] salt = new byte[SALT_BYTES];
			SecureRandom sr = new SecureRandom();
			sr.nextBytes(salt);

			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(salt);
			md.update(plain.getBytes(StandardCharsets.UTF_8));
			byte[] digest = md.digest();

			String sSalt = Base64.getEncoder().encodeToString(salt);
			String sDigest = Base64.getEncoder().encodeToString(digest);
			return sSalt + ":" + sDigest;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	public boolean matches(String plain, String stored) {
		try {
			String[] parts = stored.split(":");
			if (parts.length != 2) return false;
			byte[] salt = Base64.getDecoder().decode(parts[0]);
			byte[] expected = Base64.getDecoder().decode(parts[1]);

			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(salt);
			md.update(plain.getBytes(StandardCharsets.UTF_8));
			byte[] digest = md.digest();

			return MessageDigest.isEqual(expected, digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
