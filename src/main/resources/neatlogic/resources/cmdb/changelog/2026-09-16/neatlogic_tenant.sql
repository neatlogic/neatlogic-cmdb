ALTER TABLE `cmdb_citype` ADD COLUMN `is_showincientityquery` tinyint NOT NULL DEFAULT 1 COMMENT '配置项查询中显示';

ALTER TABLE `cmdb_rel` ADD COLUMN `from_filter` mediumtext NULL COMMENT '上游端候选配置项过滤条件';
ALTER TABLE `cmdb_rel` ADD COLUMN `to_filter` mediumtext NULL COMMENT '下游端候选配置项过滤条件';
