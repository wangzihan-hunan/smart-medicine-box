-- Add plan columns to users table
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS plan VARCHAR(20) DEFAULT 'FREE',
    ADD COLUMN IF NOT EXISTS plan_expire_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS month_ai_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS month_ai_limit INTEGER DEFAULT 5,
    ADD COLUMN IF NOT EXISTS month_reset_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Create index for plan queries
CREATE INDEX IF NOT EXISTS idx_users_plan ON users(plan);
CREATE INDEX IF NOT EXISTS idx_users_plan_expire ON users(plan_expire_at);

-- Update existing users to FREE plan
UPDATE users SET plan = 'FREE', month_ai_limit = 5, month_ai_count = 0
WHERE plan IS NULL;
