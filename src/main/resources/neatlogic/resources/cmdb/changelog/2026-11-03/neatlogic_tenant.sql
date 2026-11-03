ALTER TABLE `cmdb_cientity_alert` ADD COLUMN `alert_uuid` CHAR(32) NULL COMMENT '告警源端生成的MD5标识';
ALTER TABLE `cmdb_cientity_alert` ADD COLUMN `source` VARCHAR(50) NULL COMMENT '告警来源';
CREATE UNIQUE INDEX `uk_alert_uuid` ON `cmdb_cientity_alert` (`alert_uuid`);
