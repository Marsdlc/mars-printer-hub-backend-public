ALTER TABLE printers
    ADD COLUMN chamber_temp_entity_id VARCHAR(255) NULL COMMENT 'Home Assistant 仓温实体ID' AFTER power_entity_id;
