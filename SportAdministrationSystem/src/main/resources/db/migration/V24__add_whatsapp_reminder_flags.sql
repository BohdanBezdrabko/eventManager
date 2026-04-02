-- Add reminder tracking fields to event_subscriptions_whatsapp table
ALTER TABLE event_subscriptions_whatsapp
ADD COLUMN IF NOT EXISTS reminder_72h_sent BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS reminder_24h_sent BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for finding subscriptions that need reminders
CREATE INDEX IF NOT EXISTS ix_esw_reminder_72h
    ON event_subscriptions_whatsapp (event_id, active, reminder_72h_sent)
    WHERE active = TRUE AND reminder_72h_sent = FALSE;

CREATE INDEX IF NOT EXISTS ix_esw_reminder_24h
    ON event_subscriptions_whatsapp (event_id, active, reminder_24h_sent)
    WHERE active = TRUE AND reminder_24h_sent = FALSE;
