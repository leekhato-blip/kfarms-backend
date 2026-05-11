-- Add missing columns to billing_checkout_sessions
ALTER TABLE billing_checkout_sessions ADD COLUMN IF NOT EXISTS billing_interval VARCHAR(20) NOT NULL DEFAULT 'MONTHLY';
ALTER TABLE billing_checkout_sessions ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;
ALTER TABLE billing_checkout_sessions ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;
