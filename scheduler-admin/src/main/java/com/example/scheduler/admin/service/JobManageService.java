package com.example.scheduler.admin.service;

import com.example.scheduler.admin.dto.UpdateJobBasicRequest;
import com.example.scheduler.admin.entity.JobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JobManageService {

    private static final Logger log = LoggerFactory.getLogger(JobManageService.class);

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    @Value("${scheduler.core.base-url:http://localhost:8081}")
    private String schedulerCoreBaseUrl;

    public JobManageService(JdbcTemplate jdbcTemplate, RestTemplate restTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
    }

    public Long create(JobInfo jobInfo) {
        validateCron(jobInfo.getCron());

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
            "INSERT INTO job_info(job_name, cron, handler_name, param, status, retry_count, timeout, create_time, update_time) VALUES(?,?,?,?,?,?,?,?,?)",
            jobInfo.getJobName(),
            jobInfo.getCron(),
            jobInfo.getHandlerName(),
            jobInfo.getParam(),
            jobInfo.getStatus(),
            jobInfo.getRetryCount() == null ? 0 : jobInfo.getRetryCount(),
            jobInfo.getTimeout() == null ? 0 : jobInfo.getTimeout(),
            Timestamp.valueOf(now),
            Timestamp.valueOf(now)
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Long createdId = id == null ? -1L : id;

        if (createdId > 0 && jobInfo.getStatus() != null && jobInfo.getStatus() == 1) {
            syncSchedule(createdId, jobInfo.getCron());
        }
        return createdId;
    }

    public List<JobInfo> list() {
        return jdbcTemplate.query(
            "SELECT id, job_name, cron, handler_name, param, status, retry_count, timeout, create_time, update_time FROM job_info ORDER BY id DESC",
            (rs, rowNum) -> {
                JobInfo jobInfo = new JobInfo();
                jobInfo.setId(rs.getLong("id"));
                jobInfo.setJobName(rs.getString("job_name"));
                jobInfo.setCron(rs.getString("cron"));
                jobInfo.setHandlerName(rs.getString("handler_name"));
                jobInfo.setParam(rs.getString("param"));
                jobInfo.setStatus(rs.getInt("status"));
                jobInfo.setRetryCount(rs.getInt("retry_count"));
                jobInfo.setTimeout(rs.getInt("timeout"));
                Timestamp createTime = rs.getTimestamp("create_time");
                Timestamp updateTime = rs.getTimestamp("update_time");
                jobInfo.setCreateTime(createTime == null ? null : createTime.toLocalDateTime());
                jobInfo.setUpdateTime(updateTime == null ? null : updateTime.toLocalDateTime());
                return jobInfo;
            }
        );
    }

    public int updateStatus(Long id, int status) {
        int affected = jdbcTemplate.update(
            "UPDATE job_info SET status = ?, update_time = ? WHERE id = ?",
            status,
            Timestamp.valueOf(LocalDateTime.now()),
            id
        );

        if (affected > 0) {
            if (status == 1) {
                String cron = jdbcTemplate.queryForObject("SELECT cron FROM job_info WHERE id = ?", String.class, id);
                syncSchedule(id, cron);
                resumeSchedule(id);
            } else {
                pauseSchedule(id);
            }
        }
        return affected;
    }

    public int updateCron(Long id, String cron) {
        validateCron(cron);

        int affected = jdbcTemplate.update(
            "UPDATE job_info SET cron = ?, update_time = ? WHERE id = ?",
            cron,
            Timestamp.valueOf(LocalDateTime.now()),
            id
        );

        if (affected > 0) {
            Integer status = jdbcTemplate.queryForObject("SELECT status FROM job_info WHERE id = ?", Integer.class, id);
            if (status != null && status == 1) {
                syncSchedule(id, cron);
            }
        }
        return affected;
    }

    public int updateBasicInfo(Long id, UpdateJobBasicRequest request) {
        int affected = jdbcTemplate.update(
            "UPDATE job_info SET job_name = ?, handler_name = ?, param = ?, retry_count = ?, timeout = ?, update_time = ? WHERE id = ?",
            request.getJobName(),
            request.getHandlerName(),
            request.getParam(),
            request.getRetryCount(),
            request.getTimeout(),
            Timestamp.valueOf(LocalDateTime.now()),
            id
        );

        if (affected > 0) {
            Integer status = jdbcTemplate.queryForObject("SELECT status FROM job_info WHERE id = ?", Integer.class, id);
            if (status != null && status == 1) {
                String cron = jdbcTemplate.queryForObject("SELECT cron FROM job_info WHERE id = ?", String.class, id);
                syncSchedule(id, cron);
            }
        }
        return affected;
    }

    public int delete(Long id) {
        int affected = jdbcTemplate.update("DELETE FROM job_info WHERE id = ?", id);
        if (affected > 0) {
            deleteSchedule(id);
        }
        return affected;
    }

    private void syncSchedule(Long jobId, String cron) {
        Map<String, Object> body = new HashMap<>();
        body.put("jobId", jobId);
        body.put("cron", cron);
        try {
            restTemplate.postForObject(schedulerCoreBaseUrl + "/internal/schedules/sync", body, String.class);
        } catch (Exception ex) {
            log.warn("Sync schedule failed, jobId={}, cron={}", jobId, cron, ex);
        }
    }

    private void pauseSchedule(Long jobId) {
        try {
            restTemplate.postForObject(schedulerCoreBaseUrl + "/internal/schedules/" + jobId + "/pause", null, String.class);
        } catch (Exception ex) {
            log.warn("Pause schedule failed, jobId={}", jobId, ex);
        }
    }

    private void resumeSchedule(Long jobId) {
        try {
            restTemplate.postForObject(schedulerCoreBaseUrl + "/internal/schedules/" + jobId + "/resume", null, String.class);
        } catch (Exception ex) {
            log.warn("Resume schedule failed, jobId={}", jobId, ex);
        }
    }

    private void deleteSchedule(Long jobId) {
        try {
            restTemplate.postForObject(schedulerCoreBaseUrl + "/internal/schedules/" + jobId + "/delete", null, String.class);
        } catch (Exception ex) {
            log.warn("Delete schedule failed, jobId={}", jobId, ex);
        }
    }

    private void validateCron(String cron) {
        if (cron == null || cron.isBlank()) {
            throw new IllegalArgumentException("cron is required");
        }
        if (!CronExpression.isValidExpression(cron)) {
            throw new IllegalArgumentException("invalid cron expression: " + cron);
        }
    }
}
