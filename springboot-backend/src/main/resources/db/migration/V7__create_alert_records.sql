-- V7: 创建告警记录表
CREATE TABLE IF NOT EXISTS alert_records (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    level VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    metric_value VARCHAR(100),
    threshold_value VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    handled_by VARCHAR(50),
    handled_at TIMESTAMP,
    handle_remark TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alert_level ON alert_records(level);
CREATE INDEX IF NOT EXISTS idx_alert_type ON alert_records(type);
CREATE INDEX IF NOT EXISTS idx_alert_status ON alert_records(status);
CREATE INDEX IF NOT EXISTS idx_alert_created_at ON alert_records(created_at);

COMMENT ON TABLE alert_records IS '系统告警记录表';
COMMENT ON COLUMN alert_records.type IS '告警类型：error_rate-异常率, response_time-响应时间, cpu_usage-CPU使用率, memory_usage-内存使用率';
COMMENT ON COLUMN alert_records.level IS '告警级别：INFO-提示, WARNING-警告, CRITICAL-严重';
COMMENT ON COLUMN alert_records.status IS '告警状态：PENDING-待处理, RESOLVED-已解决, IGNORED-已忽略';
