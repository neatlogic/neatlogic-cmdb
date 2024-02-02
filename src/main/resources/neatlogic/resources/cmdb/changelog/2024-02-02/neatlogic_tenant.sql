CREATE TABLE IF NOT EXISTS `cmdb_sync_objtype` (
                                                   `id` bigint NOT NULL,
                                                   `obj_category` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '对象大类',
                                                   `obj_type` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '对象小类',
                                                   `ci_id` bigint DEFAULT NULL COMMENT '模型id',
                                                   PRIMARY KEY (`id`) USING BTREE,
                                                   UNIQUE KEY `uk_obj` (`obj_category`,`obj_type`) USING BTREE,
                                                   UNIQUE KEY `uk_ciid` (`ci_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;