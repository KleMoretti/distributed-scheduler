package com.example.scheduler.worker.executor;

import com.example.scheduler.api.JobContext;
import com.example.scheduler.api.JobContextHolder;
import com.example.scheduler.api.JobHandler;
import com.example.scheduler.common.dto.JobMessage;
import com.example.scheduler.worker.netty.NettyResultClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(JobExecutor.class);

    private final ApplicationContext applicationContext;
    private final JdbcTemplate jdbcTemplate;
    private final NettyResultClient resultClient;

    @Value("${scheduler.worker.id:worker-1}")
    private String workerId;

    public JobExecutor(ApplicationContext applicationContext,
            JdbcTemplate jdbcTemplate,
            NettyResultClient resultClient) {
        this.applicationContext = applicationContext;
        this.jdbcTemplate = jdbcTemplate;
        this.resultClient = resultClient;
    }

    public JobExecutionResult execute(JobMessage message) {
        if (message == null || message.getJobId() == null) {
            log.warn("Ignore empty job message");
            return new JobExecutionResult(false, "INVALID_MESSAGE");
        }
        return runJob(message);
    }

    private JobExecutionResult runJob(JobMessage message) {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end;
        int status = 1;
        String result = "SUCCESS";
        int attempt = message.getAttempt() == null ? 0 : message.getAttempt();
        int shardIndex = message.getShardIndex() == null ? 0 : message.getShardIndex();
        int shardTotal = Math.max(1, message.getShardTotal() == null ? 1 : message.getShardTotal());

        try {
            JobContext context = new JobContext();
            context.setJobId(message.getJobId());
            context.setWorkerId(workerId);
            context.setStartTime(start);
            context.setAttempt(attempt);
            context.setShardIndex(shardIndex);
            context.setShardTotal(shardTotal);
            JobContextHolder.set(context);

            Map<String, JobHandler> handlers = applicationContext.getBeansOfType(JobHandler.class);
            JobHandler handler = handlers.get(message.getHandlerName());
            if (handler == null) {
                throw new IllegalArgumentException("No handler found: " + message.getHandlerName());
            }

            handler.execute(message.getParam());
        } catch (Exception ex) {
            status = 0;
            result = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            log.error("Execute job {} failed, attempt={}, shard={}/{}", message.getJobId(), attempt, shardIndex + 1,
                    shardTotal, ex);
        } finally {
            JobContextHolder.clear();
            end = LocalDateTime.now();
            String logMessage = String.format("attempt=%d, shard=%d/%d, result=%s", attempt, shardIndex + 1, shardTotal,
                    result);
            jdbcTemplate.update(
                    "INSERT INTO job_log(job_id, worker, start_time, end_time, status, message) VALUES(?,?,?,?,?,?)",
                    message.getJobId(), workerId, start, end, status, logMessage);
            resultClient.sendResult(message.getJobId(), workerId, status, result, attempt, shardIndex, shardTotal);
        }

        return new JobExecutionResult(status == 1, result);
    }

    public record JobExecutionResult(boolean success, String message) {
    }
}
