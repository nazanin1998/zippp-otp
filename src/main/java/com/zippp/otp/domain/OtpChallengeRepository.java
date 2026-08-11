package com.zippp.otp.domain;

import com.zippp.otp.config.RedisConfig;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Redis-backed repository for {@link OtpChallenge}.
 *
 * Keys:   otp:challenge:{challengeKey}
 * Value:  JSON {@link OtpChallenge}
 * TTL:    2 minutes (set on every save)
 */
@Repository
public class OtpChallengeRepository {

    public static final Duration TTL = Duration.ofMinutes(2);

    private final RedisTemplate<String, Object> redis;

    public OtpChallengeRepository(RedisTemplate<String, Object> otpRedisTemplate) {
        this.redis = otpRedisTemplate;
    }

    public void save(String challengeKey, OtpChallenge challenge) {
        redis.opsForValue().set(key(challengeKey), challenge, TTL);
    }

    public Optional<OtpChallenge> find(String challengeKey) {
        Object value = redis.opsForValue().get(key(challengeKey));
        if (value instanceof OtpChallenge c) {
            return Optional.of(c);
        }
        return Optional.empty();
    }

    /**
     * Read-modify-write of attempts. Uses Redis transactions so concurrent
     * verify calls on the same challenge don't lose updates.
     */
    public Optional<OtpChallenge> incrementAttempts(String challengeKey) {
        String k = key(challengeKey);
        Object value = redis.opsForValue().get(k);
        if (!(value instanceof OtpChallenge current)) {
            return Optional.empty();
        }
        OtpChallenge updated = current.withIncrementedAttempts();
        Long ttl = redis.getExpire(k);
        if (ttl == null || ttl <= 0) {
            // Already expired — Redis will purge on access; treat as missing.
            redis.delete(k);
            return Optional.empty();
        }
        redis.opsForValue().set(k, updated, Duration.ofSeconds(ttl));
        return Optional.of(updated);
    }

    public void delete(String challengeKey) {
        redis.delete(key(challengeKey));
    }

    private static String key(String challengeKey) {
        return RedisConfig.CHALLENGE_KEY_PREFIX + challengeKey;
    }
}
