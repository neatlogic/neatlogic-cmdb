CREATE TABLE IF NOT EXISTS `cmdb_cientity_event_queue` (
    `id` bigint NOT NULL COMMENT 'id',
    `event_type` varchar(50) NOT NULL COMMENT '事件类型',
    `cientity_id` bigint NOT NULL COMMENT '配置项id',
    `ci_id` bigint DEFAULT NULL COMMENT '模型id',
    `status` varchar(50) NOT NULL COMMENT '状态',
    `server_id` int DEFAULT NULL COMMENT '执行服务器id',
    `retry_count` int NOT NULL DEFAULT 0 COMMENT '执行次数',
    `payload` longtext NOT NULL COMMENT '事件快照',
    `fcd` timestamp(3) NOT NULL COMMENT '创建时间',
    `lcd` timestamp(3) NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_status_lcd` (`status`, `lcd`) USING BTREE,
    KEY `idx_server_status` (`server_id`, `status`) USING BTREE,
    KEY `idx_cientity_id` (`cientity_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='配置项事件队列表';
