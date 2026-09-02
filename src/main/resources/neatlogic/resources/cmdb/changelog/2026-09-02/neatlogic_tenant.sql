CREATE TABLE IF NOT EXISTS `cmdb_attr_invoke` (
    `id` bigint NOT NULL COMMENT '主键ID',
    `cientity_id` bigint NOT NULL COMMENT '配置项ID',
    `attr_id` bigint NOT NULL COMMENT '属性ID',
    `type` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '引用类型',
    `invoke_id` bigint NOT NULL COMMENT '引用ID',
    PRIMARY KEY (`id`),
    KEY `idx_cientity_attr_type` (`cientity_id`, `attr_id`, `type`),
    KEY `idx_attr_type_invoke_cientity` (`attr_id`, `type`, `invoke_id`, `cientity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='配置项属性引用数据';
