CREATE TABLE IF NOT EXISTS `matrix_cmdb_custom_view`(
    `matrix_uuid` CHAR(32) NOT NULL COMMENT '矩阵UUID',
    `custom_view_id` BIGINT NOT NULL COMMENT '自定义视图ID',
    `config` MEDIUMTEXT NOT NULL COMMENT '配置信息',
    PRIMARY KEY (`matrix_uuid`)
) ENGINE=INNODB CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CMDB自定义视图矩阵配置表';