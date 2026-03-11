package com.example.scheduler.admin.service;

import com.example.scheduler.admin.dto.PageResult;
import com.example.scheduler.admin.entity.JobLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobLogService {

    private final JdbcTemplate jdbcTemplate;

    public JobLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<JobLog> page(Long jobId, LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;

        StringBuilder whereSql = new StringBuilder(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();

        if (jobId != null) {
            whereSql.append(" AND job_id = ? ");
            args.add(jobId);
        }
        if (startTime != null) {
            whereSql.append(" AND start_time >= ? ");
            args.add(Timestamp.valueOf(startTime));
        }
        if (endTime != null) {
            whereSql.append(" AND start_time <= ? ");
            args.add(Timestamp.valueOf(endTime));
        }

        String countSql = "SELECT COUNT(1) FROM job_log" + whereSql;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(safeSize);
        queryArgs.add(offset);

        String listSql = "SELECT id, job_id, worker, start_time, end_time, status, message FROM job_log"
            + whereSql
            + " ORDER BY id DESC LIMIT ? OFFSET ?";

        List<JobLog> records = jdbcTemplate.query(listSql, (rs, rowNum) -> {
            JobLog log = new JobLog();
            log.setId(rs.getLong("id"));
            log.setJobId(rs.getLong("job_id"));
            log.setWorker(rs.getString("worker"));
            Timestamp start = rs.getTimestamp("start_time");
            Timestamp end = rs.getTimestamp("end_time");
            log.setStartTime(start == null ? null : start.toLocalDateTime());
            log.setEndTime(end == null ? null : end.toLocalDateTime());
            log.setStatus(rs.getInt("status"));
            log.setMessage(rs.getString("message"));
            return log;
        }, queryArgs.toArray());

        PageResult<JobLog> result = new PageResult<>();
        result.setTotal(total == null ? 0 : total);
        result.setPage(safePage);
        result.setSize(safeSize);
        result.setRecords(records);
        return result;
    }
}
