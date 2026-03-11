package com.example.scheduler.worker.registry;

import com.example.scheduler.common.constant.RedisKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

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
        cleanupStaleWorkers();
        redisTemplate.opsForSet().add(RedisKeys.WORKER_LIST, workerId + ":" + workerAddress);
        redisTemplate.opsForValue().set(
            RedisKeys.WORKER_HEARTBEAT_PREFIX + workerId,
            "1",
            30,
            TimeUnit.SECONDS
        );
    }

    private void cleanupStaleWorkers() {
        Set<String> workers = redisTemplate.opsForSet().members(RedisKeys.WORKER_LIST);
        if (workers == null || workers.isEmpty()) {
            return;
        }

        for (String worker : workers) {
            if (worker == null || worker.isBlank()) {
                continue;
            }
            int index = worker.indexOf(':');
            if (index <= 0) {
                redisTemplate.opsForSet().remove(RedisKeys.WORKER_LIST, worker);
                continue;
            }

            String id = worker.substring(0, index);
            Boolean alive = redisTemplate.hasKey(RedisKeys.WORKER_HEARTBEAT_PREFIX + id);
            if (!Boolean.TRUE.equals(alive)) {
                redisTemplate.opsForSet().remove(RedisKeys.WORKER_LIST, worker);
            }
        }
    }
}
