CREATE TABLE `cmdb_customview_globalattr`
(
    `customview_id`      bigint                                                    NOT NULL COMMENT '视图id',
    `customview_ci_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型在视图中的唯一id',
    `uuid`               char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'uuid',
    `attr_id`            bigint                                                        DEFAULT NULL COMMENT '属性id',
    `name`               varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
    `alias`              varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '别名',
    `sort`               int                                                           DEFAULT NULL COMMENT '顺序',
    `is_hidden`          tinyint                                                       DEFAULT '0' COMMENT '是否隐藏',
    `is_primary`         tinyint                                                       DEFAULT '0' COMMENT '是否主键',
    `condition`          text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '条件，json结构',
    PRIMARY KEY (`customview_id`, `uuid`) USING BTREE,
    KEY `idx_attr_id` (`attr_id`) USING BTREE,
    KEY `idx_customviewci_id` (`customview_ci_uuid`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='cmdb自定义视图属性表';