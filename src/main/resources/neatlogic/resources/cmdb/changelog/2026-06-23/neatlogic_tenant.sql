CREATE TABLE IF NOT EXISTS `cmdb_cientity_attr_metric` (
    `id` bigint NOT NULL COMMENT 'id',
    `cientity_id` bigint NOT NULL COMMENT '配置项id',
    `attr_id` bigint NOT NULL COMMENT '属性id',
    `metric_time` timestamp(3) NOT NULL COMMENT '性能数据时间',
    `value` decimal(30,10) NOT NULL COMMENT '性能数据值',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_cientity_attr_time` (`cientity_id`, `attr_id`, `metric_time`) USING BTREE,
    KEY `idx_attr_time` (`attr_id`, `metric_time`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='配置项属性性能数据表';
