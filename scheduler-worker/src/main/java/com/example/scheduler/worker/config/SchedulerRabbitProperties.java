package com.example.scheduler.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scheduler.rabbit")
public class SchedulerRabbitProperties {

    private String exchange = "scheduler.job.exchange";
    private String routeKey = "scheduler.job.route";
    private String queue = "scheduler.job.queue";
    private String retryExchange = "scheduler.job.retry.exchange";
    private String retryRouteKey = "scheduler.job.retry.route";
    private String retryQueue = "scheduler.job.retry.queue";
    private long retryDelayMs = 10000L;

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRouteKey() {
        return routeKey;
    }

    public void setRouteKey(String routeKey) {
        this.routeKey = routeKey;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getRetryExchange() {
        return retryExchange;
    }

    public void setRetryExchange(String retryExchange) {
        this.retryExchange = retryExchange;
    }

    public String getRetryRouteKey() {
        return retryRouteKey;
    }

    public void setRetryRouteKey(String retryRouteKey) {
        this.retryRouteKey = retryRouteKey;
    }

    public String getRetryQueue() {
        return retryQueue;
    }

    public void setRetryQueue(String retryQueue) {
        this.retryQueue = retryQueue;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }
}
