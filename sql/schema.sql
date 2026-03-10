CREATE TABLE IF NOT EXISTS job_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    job_name VARCHAR(128) COMMENT '任务名称',
    cron VARCHAR(64) COMMENT 'Cron表达式',
    handler_name VARCHAR(128) COMMENT '执行器名称',
    param VARCHAR(255) COMMENT '任务参数',
    status INT DEFAULT 1 COMMENT '任务状态: 0停止 1运行',
    retry_count INT DEFAULT 0 COMMENT '失败重试次数',
    timeout INT DEFAULT 0 COMMENT '超时时间(秒)',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间'
) COMMENT='任务定义表';

CREATE TABLE IF NOT EXISTS job_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志主键ID',
    job_id BIGINT COMMENT '任务ID',
    worker VARCHAR(128) COMMENT '执行节点标识',
    start_time DATETIME COMMENT '开始执行时间',
    end_time DATETIME COMMENT '执行结束时间',
    status INT COMMENT '执行状态: 0失败 1成功',
    message TEXT COMMENT '执行日志详情'
) COMMENT='任务执行日志表';
