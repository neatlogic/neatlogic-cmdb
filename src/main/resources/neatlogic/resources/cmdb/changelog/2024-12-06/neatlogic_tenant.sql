CREATE TABLE `cmdb_sync_data_hash`
(
    `data_id`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据_id值',
    `data_hash`       char(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据指纹',
    `update_time`     datetime                            DEFAULT NULL COMMENT '更新时间',
    `collection_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '集合名称',
    PRIMARY KEY (`data_id`, `collection_name`) USING BTREE,
    KEY `idx_updatetime` (`update_time`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;