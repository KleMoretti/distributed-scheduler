package com.example.scheduler.core.quartz;

import com.example.scheduler.core.service.DispatchService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
public class DispatchQuartzJob implements Job {

    private final DispatchService dispatchService;
    public static final String JOB_ID = "jobId";

    public DispatchQuartzJob(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        long jobId = jobDataMap.getLong(JOB_ID);
        dispatchService.dispatch(jobId);
    }
}
