-- Create recognition test records table for experiment data collection
CREATE TABLE IF NOT EXISTS recognition_test_records (
    id SERIAL PRIMARY KEY,
    batch_id VARCHAR(50),
    image_name VARCHAR(255),
    image_type VARCHAR(20),
    recognition_method VARCHAR(10),
    success BOOLEAN,
    cost_time_ms BIGINT,
    medicine_count INTEGER,
    ground_truth_count INTEGER,
    correct_count INTEGER,
    raw_text TEXT,
    result_json TEXT,
    error_message VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recognition_test_batch ON recognition_test_records(batch_id);
CREATE INDEX IF NOT EXISTS idx_recognition_test_type ON recognition_test_records(image_type);
CREATE INDEX IF NOT EXISTS idx_recognition_test_method ON recognition_test_records(recognition_method);
CREATE INDEX IF NOT EXISTS idx_recognition_test_success ON recognition_test_records(success);
