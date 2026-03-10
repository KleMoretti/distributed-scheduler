package com.example.scheduler.core.config;

import com.example.scheduler.core.quartz.DispatchQuartzJob;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail dispatchJobDetail() {
        return newJob(DispatchQuartzJob.class)
            .withIdentity("dispatchJob")
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger dispatchTrigger(JobDetail dispatchJobDetail) {
        return newTrigger()
            .forJob(dispatchJobDetail)
            .withIdentity("dispatchTrigger")
            .withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever())
            .build();
    }
}
