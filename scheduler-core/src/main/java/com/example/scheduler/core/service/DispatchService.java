package com.example.scheduler.core.service;

import com.example.scheduler.common.constant.RedisKeys;
import com.example.scheduler.common.dto.JobMessage;
import com.example.scheduler.core.producer.JobProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DispatchService {

    private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

    private final RedisLockService redisLockService;
    private final JdbcTemplate jdbcTemplate;
    private final JobProducer jobProducer;

    public DispatchService(RedisLockService redisLockService,
            JdbcTemplate jdbcTemplate,
            JobProducer jobProducer) {
        this.redisLockService = redisLockService;
        this.jdbcTemplate = jdbcTemplate;
        this.jobProducer = jobProducer;
    }

    public void dispatch() {
        String lockToken = redisLockService.tryLock(RedisKeys.SCHEDULER_LOCK, 5);
        if (lockToken == null) {
            return;
        }

        try {
            List<Map<String, Object>> jobs = jdbcTemplate.queryForList(
                    "SELECT id, handler_name, param, retry_count, shard_total FROM job_info WHERE status = 1");

            for (Map<String, Object> row : jobs) {
                dispatchOneRow(row);
            }
        } catch (DataAccessException ex) {
            log.error("Dispatch failed due to DB access error", ex);
        } finally {
            redisLockService.unlock(RedisKeys.SCHEDULER_LOCK, lockToken);
        }
    }

    public void dispatch(Long jobId) {
        String lockKey = RedisKeys.SCHEDULER_LOCK + ":" + jobId;
        String lockToken = redisLockService.tryLock(lockKey, 5);
        if (lockToken == null) {
            return;
        }

        try {
            List<Map<String, Object>> jobs = jdbcTemplate.queryForList(
                    "SELECT id, handler_name, param, retry_count, shard_total FROM job_info WHERE status = 1 and id=?",
                    jobId);

            if (jobs.isEmpty()) {
                log.warn("No job found with id {}", jobId);
                return;
            }

            Map<String, Object> row = jobs.get(0);
            dispatchOneRow(row);
        } catch (DataAccessException ex) {
            log.error("Dispatch failed due to DB access error", ex);
        } finally {
            redisLockService.unlock(lockKey, lockToken);
        }
    }

    private void dispatchOneRow(Map<String, Object> row) {
        long currentJobId = ((Number) row.get("id")).longValue();
        String handlerName = String.valueOf(row.get("handler_name"));
        String param = String.valueOf(row.getOrDefault("param", ""));
        int maxRetry = ((Number) row.getOrDefault("retry_count", 0)).intValue();
        int shardTotal = Math.max(1, ((Number) row.getOrDefault("shard_total", 1)).intValue());

        for (int shardIndex = 0; shardIndex < shardTotal; shardIndex++) {
            JobMessage msg = new JobMessage(currentJobId, handlerName, param, maxRetry, 0, shardIndex, shardTotal);
            jobProducer.send(msg);
            log.info("Dispatch job {} to rabbitmq, shard {}/{}", currentJobId, shardIndex + 1, shardTotal);
        }
    }
}
