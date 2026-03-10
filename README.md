# distributed-scheduler

基于 `Spring Boot + MySQL + Redis + Quartz + Kafka + Netty` 的简化分布式调度系统。

## 模块说明

- `scheduler-common`: 公共常量、DTO、工具类
- `scheduler-api`: 任务执行接口定义
- `scheduler-core`: 调度中心（Quartz 触发，Redis 分布式锁，Kafka 投递）
- `scheduler-worker`: 执行节点（Kafka 消费，任务执行，日志写库，Netty 回传）
- `scheduler-admin`: 管理后台 API（任务增删改查）

## 快速开始

1. 创建数据库并执行 `sql/schema.sql`
2. 启动 MySQL/Redis/Kafka
3. 修改各模块 `application.yml` 连接信息
4. 启动顺序建议:
   - `scheduler-core`
   - `scheduler-worker` (可多实例)
   - `scheduler-admin`

## 任务流

Admin 创建任务 -> MySQL 持久化 -> Scheduler Quartz 触发扫描 -> Kafka 投递 -> Worker 消费执行 -> 写入 `job_log`。
