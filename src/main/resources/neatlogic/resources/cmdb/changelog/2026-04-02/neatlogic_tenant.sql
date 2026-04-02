CREATE TABLE IF NOT EXISTS `cmdb_cientity_tag` (
  `cientity_id` bigint NOT NULL COMMENT '配置项id',
  `tag_id` bigint NOT NULL COMMENT '标签id',
  PRIMARY KEY (`cientity_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
