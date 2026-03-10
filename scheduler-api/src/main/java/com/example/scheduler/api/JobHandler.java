package com.example.scheduler.api;

public interface JobHandler {

    void execute(String param) throws Exception;
}
