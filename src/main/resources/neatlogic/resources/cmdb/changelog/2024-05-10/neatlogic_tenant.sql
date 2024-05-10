ALTER TABLE `cmdb_sync_mapping`
    MODIFY COLUMN `action` enum ('insert','replace','delete','update','merge') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'insert' COMMENT '动作' AFTER `field`;
ALTER TABLE `cmdb_cientity_illegal`
    ADD INDEX `idx_cientity_id` (`cientity_id`) USING BTREE;
ALTER TABLE `cmdb_cientity_inspect`
    ADD INDEX `idx_cientity_id`(`ci_entity_id`) USING BTREE;
ALTER TABLE `cmdb_cientity_globalattritem`
    ADD INDEX `idx_cientity_id`(`cientity_id`) USING BTREE;