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

}
