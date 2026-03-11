package com.example.scheduler.admin.controller;

import com.example.scheduler.admin.dto.UpdateJobBasicRequest;
import com.example.scheduler.admin.entity.JobInfo;
import com.example.scheduler.admin.service.JobManageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobManageService jobManageService;

    public JobController(JobManageService jobManageService) {
        this.jobManageService = jobManageService;
    }

    @PostMapping
    public Long create(@Valid @RequestBody JobInfo jobInfo) {
        return jobManageService.create(jobInfo);
    }

    @GetMapping
    public List<JobInfo> list() {
        return jobManageService.list();
    }

    @PutMapping("/{id}/status")
    public int changeStatus(@PathVariable Long id, @RequestParam int status) {
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("status must be 0 (stopped) or 1 (running)");
        }
        return jobManageService.updateStatus(id, status);
    }

    @PutMapping("/{id}/cron")
    public int changeCron(@PathVariable Long id, @RequestParam String cron) {
        return jobManageService.updateCron(id, cron);
    }

    @PutMapping("/{id}/basic")
    public int updateBasic(@PathVariable Long id, @Valid @RequestBody UpdateJobBasicRequest request) {
        return jobManageService.updateBasicInfo(id, request);
    }

    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) {
        return jobManageService.delete(id);
    }

}
