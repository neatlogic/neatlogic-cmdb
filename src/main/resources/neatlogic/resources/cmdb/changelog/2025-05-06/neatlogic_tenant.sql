ALTER TABLE `cmdb_resourcecenter_account`
    CHANGE `name` `name` VARCHAR (255) CHARSET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称';
ALTER TABLE `cmdb_resourcecenter_account`
    CHANGE `account` `account` VARCHAR (100) CHARSET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '账号';