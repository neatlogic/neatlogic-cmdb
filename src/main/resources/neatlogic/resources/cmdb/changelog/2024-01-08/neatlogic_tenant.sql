CREATE TABLE `cmdb_topo_template` (
                                      `id` bigint DEFAULT NULL COMMENT ' Id',
                                      `ci_id` bigint DEFAULT NULL COMMENT '模型id',
                                      `name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模板名称',
                                      `is_active` tinyint DEFAULT NULL COMMENT '是否激活',
                                      `config` longtext COLLATE utf8mb4_general_ci COMMENT '配置',
                                      `is_default` tinyint DEFAULT NULL COMMENT '是否默认',
                                      KEY `idx_ci` (`ci_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;