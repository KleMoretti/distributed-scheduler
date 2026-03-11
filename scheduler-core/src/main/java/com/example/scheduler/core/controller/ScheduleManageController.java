package com.example.scheduler.core.controller;

import com.example.scheduler.core.service.QuartzScheduleService;
import org.quartz.CronExpression;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/schedules")
public class ScheduleManageController {

    private final QuartzScheduleService quartzScheduleService;

    public ScheduleManageController(QuartzScheduleService quartzScheduleService) {
        this.quartzScheduleService = quartzScheduleService;
    }

    @PostMapping("/sync")
    public String sync(@RequestBody SyncRequest request) {
        if (request.getJobId() == null || request.getCron() == null || request.getCron().isBlank()) {
            throw new IllegalArgumentException("jobId and cron are required");
        }
        if (!CronExpression.isValidExpression(request.getCron())) {
            throw new IllegalArgumentException("invalid cron expression");
        }
        quartzScheduleService.scheduleOrUpdate(request.getJobId(), request.getCron());
        return "OK";
    }

    @PostMapping("/{jobId}/pause")
    public String pause(@PathVariable Long jobId) {
        quartzScheduleService.pauseJob(jobId);
        return "OK";
    }

    @PostMapping("/{jobId}/resume")
    public String resume(@PathVariable Long jobId) {
        quartzScheduleService.resumeJob(jobId);
        return "OK";
    }

    @PostMapping("/{jobId}/delete")
    public String delete(@PathVariable Long jobId) {
        quartzScheduleService.deleteJob(jobId);
        return "OK";
    }

    public static class SyncRequest {

        private Long jobId;

        private String cron;

        public Long getJobId() {
            return jobId;
        }

        public void setJobId(Long jobId) {
            this.jobId = jobId;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }
}
