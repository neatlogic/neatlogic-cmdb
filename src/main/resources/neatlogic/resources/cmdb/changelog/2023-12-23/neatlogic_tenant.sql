
ALTER TABLE `neatlogic_develop`.`cmdb_group`
    MODIFY COLUMN `type` enum ('readonly','maintain','autoexec') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型' AFTER `name`;