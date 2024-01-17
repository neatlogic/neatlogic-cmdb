ALTER TABLE `cmdb_view`
    ADD COLUMN `alias` varchar(255) NULL COMMENT '别名' AFTER `allow_edit`;