-- V3 migration for remote medication verification
-- Add important flag to medications
ALTER TABLE medications ADD COLUMN important BOOLEAN NOT NULL DEFAULT FALSE;

-- Add video attachment link to medication_administration
ALTER TABLE medication_administration ADD COLUMN video_attachment_id UUID;
ALTER TABLE medication_administration ADD CONSTRAINT fk_video_attachment FOREIGN KEY (video_attachment_id) REFERENCES file_attachments(id);