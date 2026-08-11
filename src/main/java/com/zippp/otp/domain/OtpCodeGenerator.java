package com.zippp.otp.domain;

import java.security.SecureRandom;

/**
 * Generates a 6-digit numeric OTP. Padded with leading zeros so the wire
 * format is always 6 chars (e.g. "042317").
 *
 * Uses {@link SecureRandom} — not {@link Math#random()} — because predictability
 * of OTP codes is a critical security property.
 */
public final class OtpCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private static final int BOUND = 1_000_000; // 10^6

    private OtpCodeGenerator() { }

    public static String generate() {
        int n = RANDOM.nextInt(BOUND);
        return String.format("%0" + CODE_LENGTH + "d", n);
    }

    public static String hash(String code) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            // SHA-256 is a JDK-mandated algorithm — this branch is unreachable.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
