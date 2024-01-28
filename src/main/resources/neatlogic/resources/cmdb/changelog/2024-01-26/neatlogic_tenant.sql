ALTER TABLE `cmdb_attr`
    ADD COLUMN `is_searchable` tinyint NULL COMMENT '是否允许搜索' AFTER `is_required`;
ALTER TABLE `mq_topic`
    ADD COLUMN `config` longtext NULL COMMENT '配置' AFTER `is_active`;