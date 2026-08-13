package com.zippp.otp.domain;

import com.zippp.otpapi.enums.OtpChannel;
import com.zippp.otpapi.enums.OtpPurpose;

import java.time.Instant;

public record OtpRequest(
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

    public OtpRequest withIncrementedAttempts() {
        return new OtpRequest(codeHash, channel, purpose, expiresAt, attempts + 1);
    }
}
