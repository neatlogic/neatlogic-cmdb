ALTER TABLE `cmdb_topo_template`
    MODIFY COLUMN `id` bigint NOT NULL COMMENT ' Id' FIRST,
    ADD PRIMARY KEY (`id`);