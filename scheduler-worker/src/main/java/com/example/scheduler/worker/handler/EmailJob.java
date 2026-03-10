package com.example.scheduler.worker.handler;

import com.example.scheduler.api.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("emailJob")
public class EmailJob implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(EmailJob.class);

    @Override
    public void execute(String param) {
        log.info("send email with param={}", param);
    }
}
