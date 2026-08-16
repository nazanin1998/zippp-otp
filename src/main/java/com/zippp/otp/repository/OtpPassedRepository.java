package com.zippp.otp.repository;

import com.zippp.otp.config.RedisConfig;
import com.zippp.otp.domain.OtpPassed;
import com.zippp.otpapi.enums.OtpPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;


@Slf4j
@Repository
@RequiredArgsConstructor
public class OtpPassedRepository {

    private final RedisRepository redisRepository;
    private final JsonMapper jsonMapper;

    public void save(String challengeKey, OtpPassed challenge, Duration expiration) {
        redisRepository.save(key(challengeKey, challenge.getPurpose()), challenge, expiration);
    }

    public Optional<OtpPassed> find(String challengeKey, OtpPurpose purpose) {
        Object value = redisRepository.find(key(challengeKey, purpose));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(jsonMapper.readValue(value.toString(), OtpPassed.class));
        } catch (Exception e) {
            log.error("exception in parse otp verify, otpPassed: {}", value, e);
        }
        return Optional.empty();
    }

    private static String key(String challengeKey, OtpPurpose purpose) {
        return RedisConfig.OTP_PASSED_KEY_PREFIX + purpose + ":" + challengeKey;
    }
}
