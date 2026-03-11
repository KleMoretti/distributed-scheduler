package com.example.scheduler.core.service;

import com.example.scheduler.core.quartz.DispatchQuartzJob;
import jakarta.annotation.PostConstruct;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class QuartzScheduleService {

    private static final Logger log = LoggerFactory.getLogger(QuartzScheduleService.class);

    private final Scheduler scheduler;
    private final JdbcTemplate jdbcTemplate;

    public QuartzScheduleService(Scheduler scheduler, JdbcTemplate jdbcTemplate) {
        this.scheduler = scheduler;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        refreshAllEnabledJobs();
    }

    /**
     * 启动时全量加载启用任务到 Quartz。
     */
    public void refreshAllEnabledJobs() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, cron, status FROM job_info"
            );

            for (Map<String, Object> row : rows) {
                Long jobId = ((Number) row.get("id")).longValue();
                String cron = String.valueOf(row.get("cron"));
                int status = ((Number) row.getOrDefault("status", 0)).intValue();

                try {
                    if (status == 1) {
                        scheduleOrUpdate(jobId, cron);
                    } else {
                        pauseJob(jobId);
                    }
                } catch (Exception ex) {
                    log.error("Failed to load quartz job jobId={}, skipping", jobId, ex);
                }
            }

            log.info("Quartz jobs refreshed, total={}", rows.size());
        } catch (DataAccessException ex) {
            log.error("Load jobs from DB failed", ex);
        }
    }

    /**
     * 新增任务或更新 cron。
     */
    public void scheduleOrUpdate(Long jobId, String cron) {
        JobKey jobKey = jobKey(jobId);
        TriggerKey triggerKey = triggerKey(jobId);

        try {
            if (!scheduler.checkExists(jobKey)) {
                JobDetail jobDetail = JobBuilder.newJob(DispatchQuartzJob.class)
                    .withIdentity(jobKey)
                    .usingJobData(DispatchQuartzJob.JOB_ID, jobId)
                    .storeDurably(false)
                    .build();

                CronTrigger trigger = buildCronTrigger(triggerKey, jobKey, cron);
                scheduler.scheduleJob(jobDetail, trigger);
                log.info("Scheduled new quartz job, jobId={}, cron={}", jobId, cron);
                return;
            }

            Trigger oldTrigger = scheduler.getTrigger(triggerKey);
            if (oldTrigger == null) {
                CronTrigger trigger = buildCronTrigger(triggerKey, jobKey, cron);
                scheduler.scheduleJob(trigger);
                log.info("Created missing trigger, jobId={}, cron={}", jobId, cron);
                return;
            }

            String oldCron = ((CronTrigger) oldTrigger).getCronExpression();
            if (!oldCron.equals(cron)) {
                CronTrigger newTrigger = buildCronTrigger(triggerKey, jobKey, cron);
                scheduler.rescheduleJob(triggerKey, newTrigger);
                log.info("Rescheduled quartz job, jobId={}, oldCron={}, newCron={}", jobId, oldCron, cron);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException("scheduleOrUpdate failed, jobId=" + jobId, ex);
        }
    }

    public void pauseJob(Long jobId) {
        JobKey jobKey = jobKey(jobId);
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.pauseJob(jobKey);
                log.info("Paused quartz job, jobId={}", jobId);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException("pauseJob failed, jobId=" + jobId, ex);
        }
    }

    public void resumeJob(Long jobId) {
        JobKey jobKey = jobKey(jobId);
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.resumeJob(jobKey);
                log.info("Resumed quartz job, jobId={}", jobId);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException("resumeJob failed, jobId=" + jobId, ex);
        }
    }

    public void deleteJob(Long jobId) {
        JobKey jobKey = jobKey(jobId);
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("Deleted quartz job, jobId={}", jobId);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException("deleteJob failed, jobId=" + jobId, ex);
        }
    }

    private CronTrigger buildCronTrigger(TriggerKey triggerKey, JobKey jobKey, String cron) {
        return TriggerBuilder.newTrigger()
            .withIdentity(triggerKey)
            .forJob(jobKey)
            .withSchedule(
                CronScheduleBuilder.cronSchedule(cron)
                    .withMisfireHandlingInstructionDoNothing()
            )
            .build();
    }

    private JobKey jobKey(Long jobId) {
        return JobKey.jobKey("job-" + jobId, "scheduler-jobs");
    }

    private TriggerKey triggerKey(Long jobId) {
        return TriggerKey.triggerKey("trigger-" + jobId, "scheduler-triggers");
    }
}