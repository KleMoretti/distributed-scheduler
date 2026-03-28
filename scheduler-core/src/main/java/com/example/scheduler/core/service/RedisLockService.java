package com.example.scheduler.core.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RedisLockService {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("null")
    public String tryLock(@NonNull String key, long ttlSeconds) {
        String lockKey = Objects.requireNonNull(key, "key must not be null");
        String token = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, token, ttlSeconds, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(locked)) {
            return token;
        }
        return null;
    }

    @SuppressWarnings("null")
    public void unlock(@NonNull String key, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        RedisScript<Long> script = Objects.requireNonNull(UNLOCK_SCRIPT, "unlock script must not be null");
        List<String> keys = Collections.singletonList(Objects.requireNonNull(key, "key must not be null"));
        redisTemplate.execute(script, keys, token);
    }
}
