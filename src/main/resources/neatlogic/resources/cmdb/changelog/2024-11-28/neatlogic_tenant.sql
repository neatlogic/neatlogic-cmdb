ALTER TABLE `cmdb_global_attritem`
    ADD UNIQUE INDEX `uk`(`attr_id`, `value`) USING BTREE;