package com.example.scheduler.common.dto;

public class JobMessage {

    private Long jobId;
    private String handlerName;
    private String param;
    private Integer retry;
    private Integer attempt;
    private Integer shardIndex;
    private Integer shardTotal;

    public JobMessage() {
    }

    public JobMessage(Long jobId, String handlerName, String param, Integer retry, Integer attempt, Integer shardIndex,
            Integer shardTotal) {
        this.jobId = jobId;
        this.handlerName = handlerName;
        this.param = param;
        this.retry = retry;
        this.attempt = attempt;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public Integer getRetry() {
        return retry;
    }

    public void setRetry(Integer retry) {
        this.retry = retry;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }

    public Integer getShardIndex() {
        return shardIndex;
    }

    public void setShardIndex(Integer shardIndex) {
        this.shardIndex = shardIndex;
    }

    public Integer getShardTotal() {
        return shardTotal;
    }

    public void setShardTotal(Integer shardTotal) {
        this.shardTotal = shardTotal;
    }
}
