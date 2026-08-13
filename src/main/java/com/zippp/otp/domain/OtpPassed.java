package com.zippp.otp.domain;

import java.time.Instant;

public record OtpPassed(
        String token,
        String phone,
        Instant expiresAt,
        Instant consumedAt
){
    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}