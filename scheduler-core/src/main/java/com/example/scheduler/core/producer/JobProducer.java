package com.example.scheduler.core.producer;

import com.example.scheduler.common.dto.JobMessage;
import com.example.scheduler.common.util.JacksonUtil;
import com.example.scheduler.core.config.SchedulerRabbitProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobProducer {

    private final RabbitTemplate rabbitTemplate;
    private final SchedulerRabbitProperties rabbitProperties;

    public JobProducer(RabbitTemplate rabbitTemplate, SchedulerRabbitProperties rabbitProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitProperties = rabbitProperties;
    }

    public void send(JobMessage jobMessage) {
        rabbitTemplate.convertAndSend(
                rabbitProperties.getExchange(),
                rabbitProperties.getRouteKey(),
                JacksonUtil.toJson(jobMessage));
    }
}
