ALTER TABLE `cmdb_viewconst`
    ADD COLUMN `sort`      int                                 NULL COMMENT '排序' AFTER `label`,
    ADD COLUMN `show_type` enum ('none','all','list','detail') NULL AFTER `sort`;

replace into cmdb_viewconst (id, name, label, sort, show_type)
values (1, '_id', 'ID#', 1, 'all');
replace into cmdb_viewconst (id, name, label, sort, show_type)
values (2, '_ciLabel', '模型', 3, 'all');
replace into cmdb_viewconst (id, name, label, sort, show_type)
values (3, '_typeName', '类型', 2, 'all');
replace into cmdb_viewconst (id, name, label, sort, show_type)
values (4, '_inspectTime', '巡检时间', 1000, 'none');
replace into cmdb_viewconst (id, name, label, sort, show_type)
values (5, '_inspectStatus', '巡检状态', 1001, 'none');
replace into cmdb_viewconst (id, name, label, sort, show_type)
values (6, '_monitorTime', '监控时间', 1002, 'none');
replace into cmdb_viewconst (id, name, label, sort, show_type)
values (7, '_monitorStatus', '监控状态', 1003, 'none');