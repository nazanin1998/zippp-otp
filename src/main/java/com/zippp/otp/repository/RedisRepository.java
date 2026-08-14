package com.zippp.otp.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RedisRepository {

    private final RedisTemplate<String, Object> redis;

    public void save(String key, Object value, Duration expiration) {
        redis.opsForValue().set(key, value, Expiration.from(expiration));
    }

    public Object find(String key) {
        return redis.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redis.delete(key);
    }
}
