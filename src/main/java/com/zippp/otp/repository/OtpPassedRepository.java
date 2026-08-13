package com.zippp.otp.repository;

import com.zippp.otp.config.RedisConfig;
import com.zippp.otp.domain.OtpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class OtpPassedRepository {

    private final RedisRepository redisRepository;

    public void save(String challengeKey, OtpRequest challenge, Duration expiration) {
        redisRepository.save(key(challengeKey), challenge, expiration);
    }

    public Optional<OtpRequest> find(String challengeKey) {
        Object value = redisRepository.find(key(challengeKey));
        if (value instanceof OtpRequest c) {
            return Optional.of(c);
        }
        return Optional.empty();
    }

    private static String key(String challengeKey) {
        return RedisConfig.OTP_PASSED_KEY_PREFIX + challengeKey;
    }
}
