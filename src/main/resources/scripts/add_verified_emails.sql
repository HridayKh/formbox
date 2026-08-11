-- Add verified_emails column to tenants table for storing verified notification email addresses
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS verified_emails jsonb DEFAULT '[]'::jsonb;
UPDATE tenants SET verified_emails = '[]'::jsonb WHERE verified_emails IS NULL OR jsonb_typeof(verified_emails) != 'array';
