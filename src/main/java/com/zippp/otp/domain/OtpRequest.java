package com.zippp.otp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zippp.otpapi.enums.OtpChannel;
import com.zippp.otpapi.enums.OtpPurpose;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OtpRequest {

    private String codeHash;
    private OtpChannel channel;
    private OtpPurpose purpose;
    private Instant expiresAt;
    private int attempts;

    public static final int MAX_ATTEMPTS = 5;

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isExhausted() {
        return this.attempts >= MAX_ATTEMPTS;
    }

    public OtpRequest withIncrementedAttempts() {
        return new OtpRequest(codeHash, channel, purpose, expiresAt, attempts + 1);
    }
}
