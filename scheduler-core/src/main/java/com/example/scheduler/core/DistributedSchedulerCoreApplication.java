package com.example.scheduler.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class DistributedSchedulerCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedSchedulerCoreApplication.class, args);
    }
}
