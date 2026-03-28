package com.example.scheduler.worker.consumer;

import com.example.scheduler.common.dto.JobMessage;
import com.example.scheduler.common.util.JacksonUtil;
import com.example.scheduler.worker.config.SchedulerRabbitProperties;
import com.example.scheduler.worker.executor.JobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobConsumer.class);

    private final JobExecutor jobExecutor;
    private final RabbitTemplate rabbitTemplate;
    private final SchedulerRabbitProperties rabbitProperties;

    public JobConsumer(JobExecutor jobExecutor,
            RabbitTemplate rabbitTemplate,
            SchedulerRabbitProperties rabbitProperties) {
        this.jobExecutor = jobExecutor;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitProperties = rabbitProperties;
    }

    @RabbitListener(queues = "${scheduler.rabbit.queue:scheduler.job.queue}")
    public void consume(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("Ignore blank rabbitmq payload");
            return;
        }

        JobMessage message;
        try {
            message = JacksonUtil.fromJson(payload, JobMessage.class);
        } catch (Exception ex) {
            log.error("Parse rabbitmq payload failed, payload={}", payload, ex);
            return;
        }

        if (message == null || message.getJobId() == null || message.getHandlerName() == null
                || message.getHandlerName().isBlank()) {
            log.warn("Ignore invalid job message payload={}", payload);
            return;
        }

        int attempt = message.getAttempt() == null ? 0 : message.getAttempt();
        int maxRetry = Math.max(0, message.getRetry() == null ? 0 : message.getRetry());
        JobExecutor.JobExecutionResult result = jobExecutor.execute(message);

        if (!result.success() && attempt < maxRetry) {
            int nextAttempt = attempt + 1;
            message.setAttempt(nextAttempt);
            long delayMs = rabbitProperties.getRetryDelayMs() * nextAttempt;
            rabbitTemplate.convertAndSend(
                    rabbitProperties.getRetryExchange(),
                    rabbitProperties.getRetryRouteKey(),
                    JacksonUtil.toJson(message),
                    msg -> {
                        msg.getMessageProperties().setExpiration(String.valueOf(delayMs));
                        return msg;
                    });
            log.warn("Job {} failed, publish to retry queue, attempt={}/{}, delay={}ms",
                    message.getJobId(), nextAttempt, maxRetry, delayMs);
        }
    }
}
