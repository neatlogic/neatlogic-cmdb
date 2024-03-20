ALTER TABLE `cmdb_sync_ci_collection`
    ADD COLUMN `is_allow_multiple` tinyint NULL COMMENT '是否允许更新多条数据' AFTER `is_auto_commit`;