package com.example.scheduler.worker.consumer;

import com.example.scheduler.common.dto.JobMessage;
import com.example.scheduler.common.util.JacksonUtil;
import com.example.scheduler.worker.executor.JobExecutor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobConsumer {

    private final JobExecutor jobExecutor;

    public JobConsumer(JobExecutor jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    @KafkaListener(topics = "${scheduler.kafka.topic:scheduler_job_topic}", groupId = "${scheduler.kafka.group:worker-group}")
    public void consume(String payload) {
        JobMessage message = JacksonUtil.fromJson(payload, JobMessage.class);
        jobExecutor.execute(message);
    }
}
