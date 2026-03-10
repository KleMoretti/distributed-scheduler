package com.example.scheduler.common.dto;

public class JobMessage {

    private Long jobId;
    private String handlerName;
    private String param;
    private Integer retry;

    public JobMessage() {
    }

    public JobMessage(Long jobId, String handlerName, String param, Integer retry) {
        this.jobId = jobId;
        this.handlerName = handlerName;
        this.param = param;
        this.retry = retry;
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
}
