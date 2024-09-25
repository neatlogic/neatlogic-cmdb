ALTER TABLE `cmdb_attr`
    ADD COLUMN `is_term` tinyint NULL COMMENT '是否专有名词' AFTER `is_searchable`;