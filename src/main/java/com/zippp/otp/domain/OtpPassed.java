package com.zippp.otp.domain;

import com.zippp.otpapi.enums.OtpPurpose;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class OtpPassed {
    private OtpPurpose purpose;
    private Instant expiresAt;

    public boolean isExpired(Instant now) {
        return now.isAfter(this.expiresAt);
    }
}