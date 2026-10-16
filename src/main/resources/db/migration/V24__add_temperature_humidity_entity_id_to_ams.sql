ALTER TABLE ams
    ADD COLUMN temperature_entity_id VARCHAR(255) NULL COMMENT 'Home Assistant 温度实体ID' AFTER ams_no,
    ADD COLUMN humidity_entity_id VARCHAR(255) NULL COMMENT 'Home Assistant 湿度实体ID' AFTER temperature_entity_id;
