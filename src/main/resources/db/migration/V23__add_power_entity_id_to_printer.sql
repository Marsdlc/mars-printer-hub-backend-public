ALTER TABLE printers
    ADD COLUMN power_entity_id VARCHAR(255) NULL COMMENT 'Home Assistant 功耗实体ID' AFTER location;
