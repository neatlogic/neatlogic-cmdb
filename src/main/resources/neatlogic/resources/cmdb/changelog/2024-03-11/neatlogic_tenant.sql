ALTER TABLE `cmdb_sync_mapping`
    ADD COLUMN `global_attr_id` bigint NULL COMMENT '全局属性id' AFTER `direction`;