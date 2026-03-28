package com.example.scheduler.api;

public final class JobContextHolder {

    private static final ThreadLocal<JobContext> CONTEXT = new ThreadLocal<>();

    private JobContextHolder() {
    }

    public static void set(JobContext context) {
        CONTEXT.set(context);
    }

    public static JobContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
