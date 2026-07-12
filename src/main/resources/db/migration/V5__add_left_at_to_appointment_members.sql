ALTER TABLE appointment_members
    ADD COLUMN left_at TIMESTAMPTZ;

CREATE INDEX idx_appointment_members_left_at
    ON appointment_members (left_at);
