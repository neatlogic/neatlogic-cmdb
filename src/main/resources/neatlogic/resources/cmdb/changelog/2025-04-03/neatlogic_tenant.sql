CREATE TABLE IF NOT EXISTS `cmdb_resourcecenter_applicationlist_display` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `app_system_id` BIGINT COMMENT '应用系统ID',
    `app_module_id` BIGINT COMMENT '应用模块ID',
    `env_id` BIGINT COMMENT '环境ID',
    `config` LONGTEXT NOT NULL COMMENT '配置信息',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk` (`app_system_id`, `app_module_id`, `env_id`)
) ENGINE = INNODB CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `cmdb_resourcecenter_assetlist_display` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `root_ci_name` VARCHAR (100) NOT NULL COMMENT '根模型名称',
    `config` LONGTEXT NOT NULL COMMENT '配置',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk` (`root_ci_name`)
) ENGINE = INNODB CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;