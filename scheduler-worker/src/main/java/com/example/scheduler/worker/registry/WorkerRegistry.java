package com.example.scheduler.worker.registry;

import com.example.scheduler.common.constant.RedisKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class WorkerRegistry {

    private final StringRedisTemplate redisTemplate;

    @Value("${scheduler.worker.id:worker-1}")
    private String workerId;

    @Value("${scheduler.worker.address:127.0.0.1}")
    private String workerAddress;

    public WorkerRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(initialDelay = 1000, fixedDelay = 10000)
    public void heartbeat() {
        redisTemplate.opsForSet().add(RedisKeys.WORKER_LIST, workerId + ":" + workerAddress);
        redisTemplate.opsForValue().set(
            RedisKeys.WORKER_HEARTBEAT_PREFIX + workerId,
            "1",
            Duration.ofSeconds(30)
        );
    }
}
