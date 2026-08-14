package com.zippp.otp.domain;

import com.zippp.otpapi.enums.OtpPurpose;

import java.time.Instant;

public record OtpPassed(
        OtpPurpose purpose,
        Instant expiresAt
){
    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}