CREATE INDEX idx_name_prefix ON cmdb_cientity (name(500));

CREATE TABLE `cmdb_sync_data_audit`
(
    `id`               bigint NOT NULL COMMENT 'id',
    `audit_id`         bigint                                  DEFAULT NULL COMMENT '审计id，每次只会记录最后一次审计id',
    `ci_collection_id` bigint                                  DEFAULT NULL COMMENT '采集id',
    `collection_name`  varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集合名称',
    `data_id`          varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'mongodb数据id',
    `error`            longtext COLLATE utf8mb4_general_ci COMMENT '异常',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk` (`data_id`, `ci_collection_id`) USING BTREE,
    KEY `idx_collection` (`collection_name`) USING BTREE,
    KEY `idx_audit_id` (`audit_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;