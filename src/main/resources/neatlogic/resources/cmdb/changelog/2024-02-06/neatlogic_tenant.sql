ALTER TABLE `cmdb_sync_mapping`
    MODIFY COLUMN `action` enum('insert','replace','delete','update') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'insert' COMMENT '动作' AFTER `field`;