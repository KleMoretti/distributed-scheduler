package com.example.scheduler.core.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange schedulerJobExchange(SchedulerRabbitProperties properties) {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    @Bean
    public DirectExchange schedulerRetryExchange(SchedulerRabbitProperties properties) {
        return new DirectExchange(properties.getRetryExchange(), true, false);
    }

    @Bean
    public Queue schedulerJobQueue(SchedulerRabbitProperties properties) {
        return QueueBuilder.durable(properties.getQueue()).build();
    }

    @Bean
    public Queue schedulerRetryQueue(SchedulerRabbitProperties properties) {
        return QueueBuilder.durable(properties.getRetryQueue())
                .withArgument("x-dead-letter-exchange", properties.getExchange())
                .withArgument("x-dead-letter-routing-key", properties.getRouteKey())
                .build();
    }

    @Bean
    public Binding schedulerJobBinding(Queue schedulerJobQueue,
            DirectExchange schedulerJobExchange,
            SchedulerRabbitProperties properties) {
        return BindingBuilder.bind(schedulerJobQueue).to(schedulerJobExchange).with(properties.getRouteKey());
    }

    @Bean
    public Binding schedulerRetryBinding(Queue schedulerRetryQueue,
            DirectExchange schedulerRetryExchange,
            SchedulerRabbitProperties properties) {
        return BindingBuilder.bind(schedulerRetryQueue).to(schedulerRetryExchange).with(properties.getRetryRouteKey());
    }
}