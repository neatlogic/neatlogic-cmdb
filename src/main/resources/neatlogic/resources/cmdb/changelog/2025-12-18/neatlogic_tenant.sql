ALTER TABLE `cmdb_customview_ci`
    ADD COLUMN `is_aggregation` tinyint NULL COMMENT '是否聚合模型所有属性' AFTER `is_start`;