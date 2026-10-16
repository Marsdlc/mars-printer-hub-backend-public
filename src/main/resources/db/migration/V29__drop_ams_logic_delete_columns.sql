DELETE FROM ams WHERE is_delete = 1;
DELETE FROM ams_slots WHERE is_delete = 1;

ALTER TABLE ams
    DROP COLUMN is_delete;

ALTER TABLE ams_slots
    DROP COLUMN is_delete;
