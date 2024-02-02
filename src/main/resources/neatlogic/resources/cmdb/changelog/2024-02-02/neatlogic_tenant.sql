CREATE TABLE `cmdb_sync_objtype` (
                                     `obj_category` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '对象大类',
                                     `obj_type` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '对象小类',
                                     `ci_id` bigint DEFAULT NULL COMMENT '模型id',
                                     PRIMARY KEY (`obj_category`,`obj_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;