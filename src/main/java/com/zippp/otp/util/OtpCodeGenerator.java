package com.zippp.otp.util;

import com.zippp.otp.config.properties.OtpProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public final class OtpCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final OtpProperties otpProperties;

    public String generate() {
        int n = RANDOM.nextInt(otpProperties.getBound());
        return String.format(buildFormat(otpProperties.getCodeLength()), n);
    }

    private static String buildFormat(int len) {
        return "%0" + len + "d";
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
