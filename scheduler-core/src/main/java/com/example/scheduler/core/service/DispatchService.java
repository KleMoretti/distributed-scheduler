package com.example.scheduler.core.service;

import com.example.scheduler.common.constant.RedisKeys;
import com.example.scheduler.common.dto.JobMessage;
import com.example.scheduler.core.producer.JobProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DispatchService {

    private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final JobProducer jobProducer;

    public DispatchService(StringRedisTemplate redisTemplate, JdbcTemplate jdbcTemplate, JobProducer jobProducer) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.jobProducer = jobProducer;
    }

    public void dispatch() {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(RedisKeys.SCHEDULER_LOCK, "1", 5, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }

        try {
            List<Map<String, Object>> jobs = jdbcTemplate.queryForList(
                    "SELECT id, handler_name, param, retry_count FROM job_info WHERE status = 1"
            );

            for (Map<String, Object> row : jobs) {
                JobMessage msg = new JobMessage(
                        ((Number) row.get("id")).longValue(),
                        String.valueOf(row.get("handler_name")),
                        String.valueOf(row.getOrDefault("param", "")),
                        ((Number) row.getOrDefault("retry_count", 0)).intValue()
                );
                jobProducer.send(msg);
                log.info("Dispatch job {} to kafka", msg.getJobId());
            }
        } catch (DataAccessException ex) {
            log.error("Dispatch failed due to DB access error", ex);
        } finally {
            redisTemplate.delete(RedisKeys.SCHEDULER_LOCK);
        }
    }

    public void dispatch(Long jobId) {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(RedisKeys.SCHEDULER_LOCK+ ":" + jobId, "1", 5, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }

        try {
            List<Map<String, Object>> jobs = jdbcTemplate.queryForList(
                    "SELECT id, handler_name, param, retry_count FROM job_info WHERE status = 1 and id=?", jobId
            );

            if (jobs.isEmpty()) {
                log.warn("No job found with id {}", jobId);
                return;
            }

            Map<String, Object> row = jobs.get(0);
            JobMessage msg = new JobMessage(
                    ((Number) row.get("id")).longValue(),
                    String.valueOf(row.get("handler_name")),
                    String.valueOf(row.getOrDefault("param", "")),
                    ((Number) row.getOrDefault("retry_count", 0)).intValue()
            );

            jobProducer.send(msg);
            log.info("Dispatch job {} to kafka", msg.getJobId());
        } catch (DataAccessException ex) {
            log.error("Dispatch failed due to DB access error", ex);
        } finally {
            redisTemplate.delete(RedisKeys.SCHEDULER_LOCK + ":" + jobId);
        }
    }
}
