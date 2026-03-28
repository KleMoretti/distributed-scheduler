# distributed-scheduler

基于 `Spring Boot + MySQL + Redis + Quartz + RabbitMQ + WebSocket` 的分布式任务调度平台。

支持 `Cron` 定时任务、任务异步分发、执行监控与日志管理，通过调度中心与 Worker 执行节点解耦实现分布式执行。

## 模块说明

- `scheduler-common`: 公共常量、DTO、工具类
- `scheduler-api`: 任务执行接口定义
- `scheduler-core`: 调度中心（Quartz 触发，Redis 分布式锁，RabbitMQ 投递，WebSocket 推送）
- `scheduler-worker`: 执行节点（RabbitMQ 消费，任务执行，日志写库，Netty 回传）
- `scheduler-admin`: 管理后台 API（任务增删改查）

## 技术亮点

- 基于 Redis 分布式锁实现任务唯一调度，`SETNX + Lua` 释放锁，避免多节点重复执行
- 基于 Redis TTL 实现 Worker 动态注册与心跳续约，支持节点自动摘除
- 基于 RabbitMQ 构建任务异步分发链路，提升调度吞吐
- 设计任务失败重试机制，结合死信队列实现延迟重试
- 支持任务分片执行，将批量任务拆分为多个分片并行处理

## 快速开始

1. 创建数据库并执行 `sql/schema.sql`
2. 启动 MySQL/Redis/RabbitMQ
3. 修改各模块 `application.yml` 连接信息
4. 启动顺序建议:
   - `scheduler-core`
   - `scheduler-worker` (可多实例)
   - `scheduler-admin`

## 任务流

Admin 创建任务 -> MySQL 持久化 -> Scheduler Quartz 触发扫描 -> RabbitMQ 投递 -> Worker 消费执行 -> 写入 `job_log` -> Netty 回传结果 -> Core WebSocket 推送。

## WebSocket 监控

- 连接端点: `/ws/scheduler`
- 订阅主题: `/topic/job-result`
- 消息示例: `jobId=1,worker=worker-1,status=1,attempt=0,shardIndex=0,shardTotal=2,message=SUCCESS`
