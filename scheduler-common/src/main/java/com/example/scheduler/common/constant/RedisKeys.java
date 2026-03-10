package com.example.scheduler.common.constant;

public final class RedisKeys {

    public static final String SCHEDULER_LOCK = "scheduler:lock";
    public static final String WORKER_LIST = "scheduler:worker:list";
    public static final String WORKER_HEARTBEAT_PREFIX = "worker:heartbeat:";

    private RedisKeys() {
    }
}
