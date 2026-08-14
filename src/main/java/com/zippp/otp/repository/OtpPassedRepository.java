package com.zippp.otp.repository;

import com.zippp.otp.config.RedisConfig;
import com.zippp.otp.domain.OtpPassed;
import com.zippp.otp.domain.OtpRequest;
import com.zippp.otpapi.enums.OtpPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class OtpPassedRepository {

    private final RedisRepository redisRepository;

    public void save(String challengeKey, OtpPassed challenge, Duration expiration) {
        redisRepository.save(key(challengeKey, challenge.purpose()), challenge, expiration);
    }

    public Optional<OtpPassed> find(String challengeKey, OtpPurpose purpose) {
        Object value = redisRepository.find(key(challengeKey, purpose));
        if (value instanceof OtpPassed c) {
            return Optional.of(c);
        }
        return Optional.empty();
    }

    private static String key(String challengeKey, OtpPurpose purpose) {
        return RedisConfig.OTP_PASSED_KEY_PREFIX + purpose + ":" + challengeKey;
    }
}
