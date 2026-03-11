package com.example.scheduler.worker.executor;

import com.example.scheduler.api.JobHandler;
import com.example.scheduler.common.dto.JobMessage;
import com.example.scheduler.worker.netty.NettyResultClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(JobExecutor.class);

    private final ApplicationContext applicationContext;
    private final JdbcTemplate jdbcTemplate;
    private final NettyResultClient resultClient;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Value("${scheduler.worker.id:worker-1}")
    private String workerId;

    public JobExecutor(ApplicationContext applicationContext,
                       JdbcTemplate jdbcTemplate,
                       NettyResultClient resultClient,
                       ThreadPoolTaskExecutor taskExecutor) {
        this.applicationContext = applicationContext;
        this.jdbcTemplate = jdbcTemplate;
        this.resultClient = resultClient;
        this.taskExecutor = taskExecutor;
    }

    public void execute(JobMessage message) {
        if (message == null || message.getJobId() == null) {
            log.warn("Ignore empty job message");
            return;
        }
        taskExecutor.execute(() -> runJob(message));
    }

    private void runJob(JobMessage message) {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end;
        int status = 1;
        String result = "SUCCESS";
        int maxRetries = Math.max(0, message.getRetry() == null ? 0 : message.getRetry());

        try {
            Map<String, JobHandler> handlers = applicationContext.getBeansOfType(JobHandler.class);
            JobHandler handler = handlers.get(message.getHandlerName());
            if (handler == null) {
                throw new IllegalArgumentException("No handler found: " + message.getHandlerName());
            }

            int attempt = 0;
            while (true) {
                try {
                    handler.execute(message.getParam());
                    break;
                } catch (Exception ex) {
                    attempt++;
                    if (attempt > maxRetries) {
                        throw ex;
                    }
                    log.warn("Execute job {} failed at attempt {}, retrying", message.getJobId(), attempt, ex);
                }
            }
        } catch (Exception ex) {
            status = 0;
            result = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            log.error("Execute job {} failed", message.getJobId(), ex);
        } finally {
            end = LocalDateTime.now();
            jdbcTemplate.update(
                "INSERT INTO job_log(job_id, worker, start_time, end_time, status, message) VALUES(?,?,?,?,?,?)",
                message.getJobId(), workerId, start, end, status, result
            );
            resultClient.sendResult(message.getJobId(), workerId, status, result);
        }
    }
}
