ALTER TABLE `cmdb_cientity_alertlevel`
    MODIFY COLUMN `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '唯一标识' AFTER `level`,
    MODIFY COLUMN `color` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '颜色' AFTER `name`,
    ADD COLUMN `label` varchar(255) NULL COMMENT '名称' AFTER `color`,
    ADD COLUMN `type` enum('inspect','monitor') NULL COMMENT '类型' AFTER `label`,
    ADD UNIQUE INDEX `uk`(`name`, `type`) USING BTREE;

ALTER TABLE `cmdb_cientity_alertlevel`
    ADD COLUMN `id` bigint NOT NULL COMMENT 'id' AFTER `type`,
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (`id`) USING BTREE,
    ADD UNIQUE INDEX `uk2`(`level`) USING BTREE;