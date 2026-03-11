package com.example.scheduler.worker.consumer;

import com.example.scheduler.common.dto.JobMessage;
import com.example.scheduler.common.util.JacksonUtil;
import com.example.scheduler.worker.executor.JobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobConsumer.class);

    private final JobExecutor jobExecutor;

    public JobConsumer(JobExecutor jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    @KafkaListener(topics = "${scheduler.kafka.topic:scheduler_job_topic}", groupId = "${scheduler.kafka.group:worker-group}")
    public void consume(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("Ignore blank kafka payload");
            return;
        }

        JobMessage message;
        try {
            message = JacksonUtil.fromJson(payload, JobMessage.class);
        } catch (Exception ex) {
            log.error("Parse kafka payload failed, payload={}", payload, ex);
            return;
        }

        if (message == null || message.getJobId() == null || message.getHandlerName() == null || message.getHandlerName().isBlank()) {
            log.warn("Ignore invalid job message payload={}", payload);
            return;
        }

        jobExecutor.execute(message);
    }
}
