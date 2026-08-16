package com.zippp.otp.repository;

import com.zippp.otp.config.RedisConfig;
import com.zippp.otp.domain.OtpRequest;
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
public class OtpRequestRepository {

    private final RedisRepository redisRepository;
    private final JsonMapper jsonMapper;

    public void save(String challengeKey, OtpRequest challenge, Duration expiration) {
        redisRepository.save(key(challengeKey, challenge.getPurpose()), challenge, expiration);
    }

    public Optional<OtpRequest> find(String challengeKey, OtpPurpose purpose) {
        Object value = redisRepository.find(key(challengeKey, purpose));
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof OtpRequest) {
            return Optional.of((OtpRequest) value);
        }
        return Optional.empty();
    }

    /**
     * Read-modify-write of attempts. Uses Redis transactions so concurrent
     * verify calls on the same challenge don't lose updates.
     */
//    public Optional<OtpRequest> incrementAttempts(String challengeKey) {
//        String k = key(challengeKey);
//        Object value = redis.opsForValue().get(k);
//        if (!(value instanceof OtpRequest current)) {
//            return Optional.empty();
//        }
//        OtpRequest updated = current.withIncrementedAttempts();
//        Long ttl = redis.getExpire(k);
//        if (ttl == null || ttl <= 0) {
//            // Already expired — Redis will purge on access; treat as missing.
//            redis.delete(k);
//            return Optional.empty();
//        }
//        redis.opsForValue().set(k, updated, Duration.ofSeconds(ttl));
//        return Optional.of(updated);
//    }

    public void delete(String challengeKey, OtpPurpose purpose) {
        redisRepository.delete(key(challengeKey, purpose));
    }

    private static String key(String challengeKey, OtpPurpose purpose) {
        return RedisConfig.OTP_REQUEST_KEY_PREFIX + purpose + ":" + challengeKey;
    }
}
