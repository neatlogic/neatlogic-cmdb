CREATE TABLE IF NOT EXISTS `cmdb_resourcecenter_applicationlist_display` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `appSystemId` BIGINT COMMENT '应用系统ID',
    `appModuleId` BIGINT COMMENT '应用模块ID',
    `envId` BIGINT COMMENT '环境ID',
    `config` LONGTEXT NOT NULL COMMENT '配置信息',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk` (`appSystemId`, `appModuleId`, `envId`)
) ENGINE = INNODB CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;