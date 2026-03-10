package com.example.scheduler.core.producer;

import com.example.scheduler.common.dto.JobMessage;
import com.example.scheduler.common.util.JacksonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${scheduler.kafka.topic:scheduler_job_topic}")
    private String topic;

    public JobProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(JobMessage jobMessage) {
        kafkaTemplate.send(topic, String.valueOf(jobMessage.getJobId()), JacksonUtil.toJson(jobMessage));
    }
}
