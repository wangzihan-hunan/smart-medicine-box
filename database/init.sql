-- ============================================
-- Smart Medicine Box Database Initialization
-- Database: PostgreSQL
-- Encoding: UTF-8
-- Created: 2026-07-13
-- Usage: python run_init.py
-- ============================================

-- ============================================
-- Drop existing objects (for re-run)
-- ============================================

DROP VIEW IF EXISTS user_medicine_stats;
DROP VIEW IF EXISTS user_reminder_stats;

DROP PROCEDURE IF EXISTS unlock_user;
DROP PROCEDURE IF EXISTS reset_login_attempts;
DROP PROCEDURE IF EXISTS update_medicine_stocks;

DROP FUNCTION IF EXISTS update_timestamp;
DROP FUNCTION IF EXISTS check_login_attempts;
DROP FUNCTION IF EXISTS check_medicine_stock;
DROP FUNCTION IF EXISTS validate_reminder_time;
DROP FUNCTION IF EXISTS log_command_status;
DROP FUNCTION IF EXISTS get_today_pending_reminders;
DROP FUNCTION IF EXISTS get_low_stock_medicines;

DROP TABLE IF EXISTS commands CASCADE;
DROP TABLE IF EXISTS reminders CASCADE;
DROP TABLE IF EXISTS prescriptions CASCADE;
DROP TABLE IF EXISTS medicines CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ============================================
-- Create trigger functions
-- ============================================

CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION check_login_attempts()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.login_attempts >= 5 THEN
        NEW.status = 'LOCKED';
        NEW.lock_time = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION check_medicine_stock()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.stock < 0 THEN
        RAISE EXCEPTION 'Medicine stock cannot be negative, current value: %', NEW.stock;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION validate_reminder_time()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.time IS NOT NULL AND NEW.time != '' THEN
        IF NOT NEW.time ~ '^([01]\d|2[0-3]):[0-5]\d$' THEN
            RAISE EXCEPTION 'Reminder time format is invalid, should be HH:MM, current value: %', NEW.time;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION log_command_status()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        NEW.time = CURRENT_TIMESTAMP::VARCHAR(20);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- Users table
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    user_name VARCHAR(50),
    role VARCHAR(20) DEFAULT 'USER' NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    phone_verified BOOLEAN DEFAULT FALSE,
    login_attempts INTEGER DEFAULT 0,
    lock_time TIMESTAMP,
    last_login_time TIMESTAMP,
    last_ip VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

CREATE TRIGGER users_update_timestamp BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
    
CREATE TRIGGER users_check_login_attempts BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION check_login_attempts();

-- ============================================
-- Medicines table
-- ============================================
CREATE TABLE IF NOT EXISTS medicines (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    user_id INTEGER NOT NULL,
    category VARCHAR(50),
    spec VARCHAR(100),
    manufacturer VARCHAR(100),
    stock INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_medicines_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_medicines_name ON medicines(name);
CREATE INDEX IF NOT EXISTS idx_medicines_user_id ON medicines(user_id);

CREATE TRIGGER medicines_update_timestamp BEFORE UPDATE ON medicines
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
    
CREATE TRIGGER medicines_check_stock BEFORE INSERT OR UPDATE ON medicines
    FOR EACH ROW EXECUTE FUNCTION check_medicine_stock();

-- ============================================
-- Prescriptions table
-- ============================================
CREATE TABLE IF NOT EXISTS prescriptions (
    id SERIAL PRIMARY KEY,
    file_url VARCHAR(255),
    user_id INTEGER NOT NULL,
    medicine_id INTEGER,
    diagnosis VARCHAR(500),
    dosage_instructions VARCHAR(1000),
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prescriptions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_prescriptions_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_prescriptions_user_id ON prescriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_upload_date ON prescriptions(upload_date);
CREATE INDEX IF NOT EXISTS idx_prescriptions_medicine_id ON prescriptions(medicine_id);

CREATE TRIGGER prescriptions_update_timestamp BEFORE UPDATE ON prescriptions
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- ============================================
-- Reminders table
-- ============================================
CREATE TABLE IF NOT EXISTS reminders (
    id SERIAL PRIMARY KEY,
    medicine_id INTEGER,
    medicine_name VARCHAR(100) NOT NULL,
    user_id INTEGER NOT NULL,
    prescription_id INTEGER,
    time VARCHAR(20),
    dosage VARCHAR(50),
    frequency VARCHAR(50) DEFAULT '每日一次',
    is_repeat BOOLEAN DEFAULT FALSE,
    taken BOOLEAN DEFAULT FALSE,
    remark VARCHAR(255),
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reminders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_reminders_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_reminders_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_reminders_medicine_name ON reminders(medicine_name);
CREATE INDEX IF NOT EXISTS idx_reminders_user_id ON reminders(user_id);
CREATE INDEX IF NOT EXISTS idx_reminders_taken ON reminders(taken);
CREATE INDEX IF NOT EXISTS idx_reminders_medicine_id ON reminders(medicine_id);
CREATE INDEX IF NOT EXISTS idx_reminders_prescription_id ON reminders(prescription_id);

CREATE TRIGGER reminders_update_timestamp BEFORE UPDATE ON reminders
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
    
CREATE TRIGGER reminders_validate_time BEFORE INSERT OR UPDATE ON reminders
    FOR EACH ROW EXECUTE FUNCTION validate_reminder_time();

-- ============================================
-- Commands table
-- ============================================
CREATE TABLE IF NOT EXISTS commands (
    id SERIAL PRIMARY KEY,
    device_id VARCHAR(50),
    user_id INTEGER NOT NULL,
    medicine_id INTEGER,
    action VARCHAR(50),
    params VARCHAR(255),
    time VARCHAR(20),
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_commands_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_commands_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_commands_device_id ON commands(device_id);
CREATE INDEX IF NOT EXISTS idx_commands_user_id ON commands(user_id);
CREATE INDEX IF NOT EXISTS idx_commands_status ON commands(status);
CREATE INDEX IF NOT EXISTS idx_commands_medicine_id ON commands(medicine_id);

CREATE TRIGGER commands_update_timestamp BEFORE UPDATE ON commands
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
    
CREATE TRIGGER commands_log_status BEFORE UPDATE ON commands
    FOR EACH ROW EXECUTE FUNCTION log_command_status();

-- ============================================
-- Views
-- ============================================

CREATE OR REPLACE VIEW user_medicine_stats AS
SELECT 
    u.id AS user_id,
    u.user_name,
    u.phone,
    COUNT(m.id) AS medicine_count,
    SUM(m.stock) AS total_stock,
    SUM(CASE WHEN m.stock < 10 THEN 1 ELSE 0 END) AS low_stock_count
FROM users u
LEFT JOIN medicines m ON u.id = m.user_id
GROUP BY u.id, u.user_name, u.phone;

CREATE OR REPLACE VIEW user_reminder_stats AS
SELECT 
    u.id AS user_id,
    u.user_name,
    COUNT(r.id) AS total_reminders,
    COUNT(CASE WHEN r.taken = FALSE THEN 1 ELSE NULL END) AS pending_reminders,
    COUNT(CASE WHEN r.taken = TRUE THEN 1 ELSE NULL END) AS completed_reminders
FROM users u
LEFT JOIN reminders r ON u.id = r.user_id
GROUP BY u.id, u.user_name;

-- ============================================
-- Procedures
-- ============================================

CREATE OR REPLACE PROCEDURE unlock_user(p_user_id INTEGER)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE users 
    SET status = 'ACTIVE', 
        login_attempts = 0, 
        lock_time = NULL
    WHERE id = p_user_id;
    
    RAISE NOTICE 'User % has been unlocked', p_user_id;
END;
$$;

CREATE OR REPLACE PROCEDURE reset_login_attempts(p_user_id INTEGER)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE users 
    SET login_attempts = 0
    WHERE id = p_user_id;
    
    RAISE NOTICE 'Login attempts for user % have been reset', p_user_id;
END;
$$;

CREATE OR REPLACE PROCEDURE update_medicine_stocks(p_user_id INTEGER, p_stock_changes JSONB)
LANGUAGE plpgsql
AS $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN SELECT * FROM jsonb_to_recordset(p_stock_changes) AS x(medicine_id INTEGER, delta INTEGER)
    LOOP
        UPDATE medicines 
        SET stock = stock + rec.delta
        WHERE id = rec.medicine_id AND user_id = p_user_id;
    END LOOP;
    
    RAISE NOTICE 'Medicine stocks for user % have been updated', p_user_id;
END;
$$;

-- ============================================
-- Custom functions
-- ============================================

CREATE OR REPLACE FUNCTION get_today_pending_reminders(p_user_id INTEGER)
RETURNS INTEGER AS $$
BEGIN
    RETURN (SELECT COUNT(*) FROM reminders 
            WHERE user_id = p_user_id AND taken = FALSE);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_low_stock_medicines(p_user_id INTEGER, p_threshold INTEGER DEFAULT 10)
RETURNS TABLE(id INTEGER, name VARCHAR(100), stock INTEGER) AS $$
BEGIN
    RETURN QUERY SELECT m.id, m.name, m.stock
                 FROM medicines m
                 WHERE m.user_id = p_user_id AND m.stock < p_threshold;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- Test data
-- ============================================

INSERT INTO users (phone, password, user_name, role, status, phone_verified, login_attempts) VALUES
('13800138000', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', 'Zhang San', 'USER', 'ACTIVE', TRUE, 0),
('13900139000', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', 'Li Si', 'USER', 'ACTIVE', FALSE, 0),
('18800188000', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', 'Admin', 'ADMIN', 'ACTIVE', TRUE, 0);

INSERT INTO medicines (name, user_id, category, spec, manufacturer, stock) VALUES
('Amoxicillin', 1, 'Antibiotic', '0.5g*20', 'North China Pharma', 50),
('Ibuprofen', 1, 'Painkiller', '0.3g*20', 'Fenbid', 30),
('Vitamin C', 1, 'Vitamin', '100mg*60', 'Yangshengtang', 100),
('Cold Medicine', 2, 'Chinese Medicine', '10g*9', '999', 20),
('Digestive Pills', 2, 'Chinese Medicine', '0.8g*36', 'Jiangzhong', 45);

INSERT INTO prescriptions (file_url, user_id, medicine_id, diagnosis, dosage_instructions, upload_date) VALUES
('/uploads/prescription_1.pdf', 1, 1, 'Upper respiratory tract infection', 'Amoxicillin, 3 times daily, 1 pill each time, take after meals', '2026-07-10 10:30:00'),
('/uploads/prescription_2.pdf', 2, 4, 'Cold and fever', 'Cold medicine, 3 times daily, 1 bag each time, take with boiled water', '2026-07-11 14:20:00');

INSERT INTO reminders (medicine_id, medicine_name, user_id, prescription_id, time, dosage, frequency, is_repeat, taken, start_date, end_date) VALUES
(1, 'Amoxicillin', 1, 1, '08:00', '1 pill', '3 times daily', TRUE, FALSE, '2026-07-10', '2026-07-17'),
(1, 'Amoxicillin', 1, 1, '14:00', '1 pill', '3 times daily', TRUE, FALSE, '2026-07-10', '2026-07-17'),
(1, 'Amoxicillin', 1, 1, '20:00', '1 pill', '3 times daily', TRUE, FALSE, '2026-07-10', '2026-07-17'),
(2, 'Ibuprofen', 1, NULL, '09:00', '1 pill', '2 times daily', TRUE, TRUE, '2026-07-10', '2026-07-15'),
(3, 'Vitamin C', 1, NULL, '12:00', '1 pill', '1 time daily', TRUE, FALSE, '2026-07-01', '2026-08-01'),
(4, 'Cold Medicine', 2, 2, '08:00', '1 bag', '3 times daily', TRUE, FALSE, '2026-07-11', '2026-07-18'),
(5, 'Digestive Pills', 2, NULL, '18:00', '3 pills', '2 times daily', TRUE, FALSE, '2026-07-05', '2026-07-20');

INSERT INTO commands (device_id, user_id, medicine_id, action, params, time, status) VALUES
('DEVICE_001', 1, NULL, 'open_door', '{"compartment": 1}', '2026-07-12 08:00', 'completed'),
('DEVICE_001', 1, 1, 'dispense_medicine', '{"medicine_id": 1, "quantity": 1}', '2026-07-12 08:01', 'completed'),
('DEVICE_002', 2, NULL, 'open_door', '{"compartment": 2}', '2026-07-12 09:00', 'completed'),
('DEVICE_001', 1, NULL, 'check_stock', '{}', '2026-07-13 10:00', 'pending');

SELECT 'Database initialization completed successfully' AS result;