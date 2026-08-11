package com.zippp.otp.domain;

import com.zippp.otpapi.enums.OtpChannel;
import com.zippp.otpapi.enums.OtpPurpose;

import java.time.Instant;

/**
 * State stored in Redis under {@code otp:challenge:{challengeKey}}.
 *
 * The plaintext code is NEVER stored — only its SHA-256 hash. The plaintext
 * is delivered via SMS/PUSH and discarded immediately.
 *
 * attempts is incremented atomically on every verify call. When it reaches
 * {@link #MAX_ATTEMPTS}, the challenge is considered burned and deleted.
 */
public record OtpChallenge(
        String     codeHash,
        OtpChannel channel,
        OtpPurpose purpose,
        Instant    expiresAt,
        int        attempts
) {
    public static final int MAX_ATTEMPTS = 5;

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isExhausted() {
        return attempts >= MAX_ATTEMPTS;
    }

    public OtpChallenge withIncrementedAttempts() {
        return new OtpChallenge(codeHash, channel, purpose, expiresAt, attempts + 1);
    }
}
