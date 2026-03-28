package com.example.scheduler.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class DistributedSchedulerWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedSchedulerWorkerApplication.class, args);
    }
}
