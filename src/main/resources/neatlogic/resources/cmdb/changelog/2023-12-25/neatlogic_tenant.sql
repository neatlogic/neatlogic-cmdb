CREATE TABLE `cmdb_ci_catalog` (
   `id` bigint NOT NULL COMMENT 'id',
   `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
   `parent_id` bigint NOT NULL COMMENT '父id',
   `lft` int DEFAULT NULL COMMENT '左编码',
   `rht` int DEFAULT NULL COMMENT '右编码',
   PRIMARY KEY (`id`) USING BTREE,
   KEY `idx_lft` (`lft`) USING BTREE,
   KEY `idx_rht` (`rht`) USING BTREE,
   KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模型目录';

ALTER TABLE `cmdb_ci`
    ADD COLUMN `catalog_id` BIGINT NULL   COMMENT '模型目录id' AFTER `type_id`;

