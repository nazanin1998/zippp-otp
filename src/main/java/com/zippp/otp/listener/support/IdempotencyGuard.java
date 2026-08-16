package com.zippp.otp.listener.support;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Atomic dedup gate for inbound AMQP messages.
 * <p>
 * If {@link #tryAcquire(String)} returns {@code true} the caller is the first
 * instance to process the given correlationId within the TTL window and must
 * proceed. If it returns {@code false} the message is a duplicate (a retry or
 * a re-delivery) and the caller must short-circuit.
 * <p>
 * Backed by Redis {@code SET key value NX EX ttl} via {@code setIfAbsent}, so
 * the check is atomic across multiple consumer instances.
 */
@Component
@RequiredArgsConstructor
public class IdempotencyGuard {

    private static final String KEY_PREFIX = "otp:idem:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redis;

    public boolean tryAcquire(String correlationId) {
        return tryAcquire(correlationId, DEFAULT_TTL);
    }

    public boolean tryAcquire(String correlationId, Duration ttl) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(KEY_PREFIX + correlationId, "1", ttl));
    }
}
