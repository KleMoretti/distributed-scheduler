package com.example.scheduler.admin.service;

import com.example.scheduler.admin.entity.JobInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobManageService {

    private final JdbcTemplate jdbcTemplate;

    public JobManageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long create(JobInfo jobInfo) {
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
        return id == null ? -1L : id;
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
        return jdbcTemplate.update(
            "UPDATE job_info SET status = ?, update_time = ? WHERE id = ?",
            status,
            Timestamp.valueOf(LocalDateTime.now()),
            id
        );
    }
}
