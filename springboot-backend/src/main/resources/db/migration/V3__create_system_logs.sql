CREATE TABLE IF NOT EXISTS system_logs (
    id SERIAL PRIMARY KEY,
    user_id INTEGER,
    user_name VARCHAR(50),
    user_phone VARCHAR(20),
    module VARCHAR(50),
    operation VARCHAR(100),
    description VARCHAR(255),
    request_method VARCHAR(10),
    request_url VARCHAR(500),
    request_params TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    response_status INTEGER,
    success BOOLEAN DEFAULT true,
    error_message VARCHAR(500),
    duration_ms BIGINT,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_system_logs_user_id ON system_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_system_logs_module ON system_logs(module);
CREATE INDEX IF NOT EXISTS idx_system_logs_operation ON system_logs(operation);
CREATE INDEX IF NOT EXISTS idx_system_logs_created_at ON system_logs(created_at);