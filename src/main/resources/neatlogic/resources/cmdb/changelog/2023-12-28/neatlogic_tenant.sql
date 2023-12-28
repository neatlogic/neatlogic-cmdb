CREATE TABLE `cmdb_ci_tree`
(
    `ci_id`        bigint NOT NULL,
    `parent_ci_id` bigint DEFAULT NULL,
    `sort`         int    DEFAULT NULL,
    PRIMARY KEY (`ci_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

DROP table if exists `cmdb_ci_catalog`;

ALTER TABLE `cmdb_ci`
    DROP COLUMN `catalog_id`;