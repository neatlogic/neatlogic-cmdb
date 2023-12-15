ALTER TABLE `cmdb_global_attr`
    ADD COLUMN `is_private` tinyint NULL COMMENT '私有属性不能编辑和删除' AFTER `description`;