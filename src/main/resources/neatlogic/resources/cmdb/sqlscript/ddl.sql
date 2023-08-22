-- ----------------------------
-- Table structure for cmdb_attr
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_attr` (
  `id` bigint NOT NULL COMMENT 'id',
  `ci_id` bigint NOT NULL COMMENT '引用ecmdb_ci的id',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '值类型',
  `prop_id` bigint DEFAULT NULL COMMENT '属性id',
  `target_ci_id` bigint DEFAULT NULL COMMENT '目标模型id',
  `expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表达式',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '英文名',
  `label` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '录入时的label名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `validator_id` bigint DEFAULT '0' COMMENT '验证器组件id，需要在代码中实现验证器',
  `is_required` tinyint NOT NULL COMMENT '是否必填',
  `is_unique` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0不唯一，1同类唯一，2全局唯一',
  `input_type` enum('at','mt') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'mt' COMMENT '属性录入方式',
  `group_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '属性分组',
  `is_private` tinyint(1) DEFAULT NULL COMMENT '私有属性',
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '属性设置',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_name` (`ci_id`,`name`) USING BTREE,
  KEY `idx_prop_id` (`prop_id`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb配置项属性表';

-- ----------------------------
-- Table structure for cmdb_attrentity
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_attrentity` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `from_cientity_id` bigint DEFAULT NULL COMMENT '所属配置项id',
  `to_cientity_id` bigint DEFAULT NULL COMMENT '引用配置项id',
  `attr_id` bigint DEFAULT NULL COMMENT '属性id',
  `from_ci_id` bigint DEFAULT NULL COMMENT '所属配置项模型id',
  `to_ci_id` bigint DEFAULT NULL COMMENT '引用配置项模型id',
  `transaction_id` bigint DEFAULT NULL COMMENT '事务id',
  `from_index` int DEFAULT NULL COMMENT '数据序号，用于检索时提高效率，只会生成前N条数据的序号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_from_cientity_id` (`from_cientity_id`,`attr_id`,`to_cientity_id`) USING BTREE,
  KEY `idx_to_cientity_id` (`to_cientity_id`) USING BTREE,
  KEY `idx_attr_id` (`attr_id`) USING BTREE,
  KEY `idx_from_ci_id` (`from_ci_id`) USING BTREE,
  KEY `idx_to_ci_id` (`to_ci_id`) USING BTREE,
  KEY `idx_transaction_id` (`transaction_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3205 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb配置项属性实例表';

-- ----------------------------
-- Table structure for cmdb_attrentity_content
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_attrentity_content` (
  `hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'hash',
  `value_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'value值的hash值，用于精确匹配',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '内容',
  PRIMARY KEY (`hash`) USING BTREE,
  KEY `idx_value_hash` (`value_hash`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb配置项属性实例内容表';

-- ----------------------------
-- Table structure for cmdb_attrentity_content_offset
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_attrentity_content_offset` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内容hash',
  `start` int DEFAULT NULL COMMENT 'start',
  `end` int DEFAULT NULL COMMENT 'end',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_field_id` (`hash`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_attrentity_content_offset';

-- ----------------------------
-- Table structure for cmdb_attrexpression_rebuild_audit
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_attrexpression_rebuild_audit` (
  `id` bigint NOT NULL COMMENT 'id',
  `ci_id` bigint DEFAULT NULL COMMENT '模型id',
  `cientity_id` bigint DEFAULT NULL COMMENT '配置项id',
  `attr_id` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '修改了哪些属性，多个属性用逗号分隔',
  `cientity_id_start` bigint DEFAULT NULL COMMENT '开始配置项id',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联方式，当cientityid不为空时才有效，invoke代表更新自己的表达式属性，invoked代表更新关联配置项的表达式属性',
  `server_id` int DEFAULT NULL COMMENT '服务器id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_attrexpression_rebuild_audit';

-- ----------------------------
-- Table structure for cmdb_attrexpression_rel
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_attrexpression_rel` (
  `expression_ci_id` bigint NOT NULL COMMENT 'expression_ci_id',
  `expression_attr_id` bigint NOT NULL COMMENT '表达式属性id',
  `value_attr_id` bigint NOT NULL COMMENT '值属性id',
  `value_ci_id` bigint NOT NULL COMMENT 'value_ci_id',
  PRIMARY KEY (`value_ci_id`,`value_attr_id`,`expression_attr_id`,`expression_ci_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_attrexpression_rel';

-- ----------------------------
-- Table structure for cmdb_ci
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_ci` (
  `id` bigint NOT NULL COMMENT 'ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '英文名',
  `label` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中文名',
  `description` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `type_id` bigint DEFAULT NULL COMMENT '类型ID ecmdb_ci_type',
  `parent_ci_id` bigint DEFAULT NULL COMMENT '父模型ID',
  `name_attr_id` bigint DEFAULT NULL COMMENT '名称属性',
  `lft` int DEFAULT NULL COMMENT '左编码',
  `rht` int DEFAULT NULL COMMENT '右编码',
  `is_private` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否私有模型，私有模型不允许修改和删除',
  `is_menu` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否在菜单中显示',
  `is_abstract` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否抽象模型',
  `is_virtual` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否虚拟模型，虚拟模型实际上是视图不是物理表',
  `file_id` bigint DEFAULT NULL COMMENT '虚拟模型配置文件id',
  `xml` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '虚拟模型配置内容',
  `expired_day` int DEFAULT '0' COMMENT '有效天数，0代表不超期',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_enname` (`name`) USING BTREE,
  KEY `idx_lft` (`lft`) USING BTREE,
  KEY `idx_rht` (`rht`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb配置项表';

-- ----------------------------
-- Table structure for cmdb_ci_auth
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_ci_auth` (
  `ci_id` bigint NOT NULL COMMENT '引用ecmdb_ci的id',
  `auth_type` enum('user','role','team','common') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `action` enum('cientityinsert','cientityupdate','cientitydelete','cientityrecover','cientityquery','cimanage','transactionmanage','passwordview') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '对应的操作动作',
  `auth_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'auth uuid',
  PRIMARY KEY (`ci_id`,`auth_type`,`action`,`auth_uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb配置项授权表';

-- ----------------------------
-- Table structure for cmdb_ci_customview
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_ci_customview` (
  `ci_id` bigint NOT NULL COMMENT '模型id',
  `customview_id` bigint NOT NULL COMMENT '自定义视图id',
  PRIMARY KEY (`ci_id`,`customview_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='配置模型自定义视图关联表';

-- ----------------------------
-- Table structure for cmdb_ci_group
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_ci_group` (
  `id` bigint NOT NULL COMMENT 'id',
  `ci_id` bigint DEFAULT NULL COMMENT '模型id',
  `group_id` bigint DEFAULT NULL COMMENT '团体id',
  `rule` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '规则',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_ci_id` (`ci_id`,`group_id`) USING BTREE,
  KEY `idx_group_id` (`group_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模型团体关系表';

-- ----------------------------
-- Table structure for cmdb_ci_unique
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_ci_unique` (
  `ci_id` bigint NOT NULL COMMENT 'ci id',
  `attr_id` bigint NOT NULL COMMENT '属性id',
  PRIMARY KEY (`ci_id`,`attr_id`) USING BTREE,
  KEY `idx_attr_id` (`attr_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_ci_unique';

-- ----------------------------
-- Table structure for cmdb_cientity
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_cientity` (
  `id` bigint NOT NULL COMMENT 'ID',
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'uuid',
  `ci_id` bigint NOT NULL COMMENT '引用ecmdb_ci的id',
  `name` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '配置项名称',
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态',
  `is_locked` tinyint(1) NOT NULL DEFAULT '0' COMMENT '如果有未提交事务，处于锁定状态',
  `fcu` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `lcu` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改用户',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  `inspect_status` enum('normal','warn','critical','','fatal') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '巡检状态',
  `inspect_time` timestamp(3) NULL DEFAULT NULL COMMENT '巡检时间',
  `monitor_status` enum('normal','warn','critical','','fatal') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '监控状态',
  `monitor_time` timestamp(3) NULL DEFAULT NULL COMMENT '监控时间',
  `renew_time` timestamp(3) NULL DEFAULT NULL COMMENT '刷新时间，用来判断是否老化',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_ci_id` (`ci_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb配置项实例表';

-- ----------------------------
-- Table structure for cmdb_cientity_alert
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_cientity_alert` (
  `id` bigint NOT NULL,
  `cientity_id` bigint DEFAULT NULL,
  `cientity_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `level` int DEFAULT NULL,
  `alert_time` timestamp(3) NULL DEFAULT NULL,
  `metric_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `metric_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `alert_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `alert_link` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_cientity_id` (`cientity_id`) USING BTREE,
  KEY `idx_cientity_uuid` (`cientity_uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for cmdb_cientity_alertlevel
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_cientity_alertlevel` (
  `level` int NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `color` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`level`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for cmdb_cientity_expiredtime
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_cientity_expiredtime` (
  `cientity_id` bigint NOT NULL COMMENT '配置项id',
  `expired_day` int DEFAULT NULL COMMENT '原来的超时天数，用于比对修正expired_time',
  `expired_time` timestamp(3) NULL DEFAULT NULL COMMENT '过期日期',
  `ci_id` bigint DEFAULT NULL COMMENT '模型id',
  PRIMARY KEY (`cientity_id`) USING BTREE,
  KEY `idx_expired_time` (`expired_time`) USING BTREE,
  KEY `idx_ci_id` (`ci_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for cmdb_cientity_group
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_cientity_group` (
  `cientity_id` bigint NOT NULL COMMENT '配置项ID',
  `group_id` bigint NOT NULL COMMENT '圈子id',
  `ci_group_id` bigint DEFAULT NULL COMMENT '命中的规则id',
  PRIMARY KEY (`group_id`,`cientity_id`) USING BTREE,
  KEY `idx_ci_group_id` (`ci_group_id`) USING BTREE,
  KEY `idx_cientity_id` (`cientity_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='配置项团体关系表';

-- ----------------------------
-- Table structure for cmdb_cientity_illegal
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_cientity_illegal` (
  `ci_id` bigint NOT NULL COMMENT '模型id',
  `cientity_id` bigint NOT NULL COMMENT '配置项id',
  `legalvalid_id` bigint NOT NULL COMMENT '规则id',
  `error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '异常',
  `valid_time` timestamp(3) NULL DEFAULT NULL COMMENT '校验时间',
  PRIMARY KEY (`cientity_id`,`legalvalid_id`) USING BTREE,
  KEY `idx_legalvalid_id` (`legalvalid_id`) USING BTREE,
  KEY `idx_ci_id` (`ci_id`) USING BTREE,
  KEY `idx_time` (`valid_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='非法配置项';

-- ----------------------------
-- Table structure for cmdb_cientity_inspect
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_cientity_inspect` (
  `id` bigint NOT NULL COMMENT '主键',
  `job_id` bigint NOT NULL COMMENT '作业id',
  `ci_entity_id` bigint NOT NULL COMMENT '配置项id',
  `inspect_status` enum('normal','warn','critical','','fatal') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '巡检状态',
  `inspect_time` timestamp(3) NULL DEFAULT NULL COMMENT '巡检时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_job_id_cientity_id` (`job_id`,`ci_entity_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='配置项的巡检状态表';

-- ----------------------------
-- Table structure for cmdb_cientity_snapshot
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_cientity_snapshot` (
  `hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'hash',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '内容',
  PRIMARY KEY (`hash`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb配置项实例快照表';

-- ----------------------------
-- Table structure for cmdb_cientity_transaction
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_cientity_transaction` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `ci_entity_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '配置项实例uuid',
  `ci_id` bigint NOT NULL COMMENT '引用ecmdb_ci的id',
  `ci_entity_id` bigint NOT NULL DEFAULT '0' COMMENT '引用ecmdb_ci_entity的id',
  `name` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `action` enum('insert','update','delete','recover') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作类型，I:insert,U:update；D:delete；S:recover；',
  `edit_mode` enum('global','partial') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '编辑模式',
  `transaction_id` bigint NOT NULL COMMENT '事务id',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '修改内容，json格式',
  `snapshot` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '修改前快照hash',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_NewIndex1` (`transaction_id`) USING BTREE,
  KEY `idx_NewIndex2` (`ci_entity_id`) USING BTREE,
  KEY `idx_NewIndex3` (`ci_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb配置项实例事务表';

-- ----------------------------
-- Table structure for cmdb_citype
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_citype` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型名称',
  `sort` int NOT NULL COMMENT '排序',
  `is_menu` tinyint(1) NOT NULL COMMENT '是否在菜单中显示',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `is_showintopo` tinyint(1) DEFAULT NULL COMMENT '是否在topo图中展示',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb配置项类型表';

-- ----------------------------
-- Table structure for cmdb_customview
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_customview` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `is_active` tinyint(1) DEFAULT NULL COMMENT '是否激活',
  `is_private` tinyint(1) DEFAULT NULL COMMENT '是否私有视图',
  `type` enum('private','public','scene') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类型',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '说明',
  `icon` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所有人uuid',
  `fcd` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `lcd` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb自定义视图表';

-- ----------------------------
-- Table structure for cmdb_customview_attr
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_customview_attr` (
  `customview_id` bigint NOT NULL COMMENT '视图id',
  `customview_ci_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型在视图中的唯一id',
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'uuid',
  `attr_id` bigint DEFAULT NULL COMMENT '属性id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
  `alias` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '别名',
  `sort` int DEFAULT NULL COMMENT '顺序',
  `is_hidden` tinyint DEFAULT '0' COMMENT '是否隐藏',
  `is_primary` tinyint DEFAULT '0' COMMENT '是否主键',
  `condition` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '条件，json结构',
  PRIMARY KEY (`customview_id`,`uuid`) USING BTREE,
  KEY `idx_attr_id` (`attr_id`) USING BTREE,
  KEY `idx_customviewci_id` (`customview_ci_uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb自定义视图属性表';

-- ----------------------------
-- Table structure for cmdb_customview_auth
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_customview_auth` (
  `customview_id` bigint NOT NULL COMMENT '视图id',
  `auth_type` enum('user','team','role','common') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '''user'',''team'',''role''',
  `auth_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'auth uuid',
  PRIMARY KEY (`customview_id`,`auth_type`,`auth_uuid`) USING BTREE,
  KEY `idx_customview_id` (`customview_id`) USING BTREE,
  KEY `idx_auth_uuid` (`auth_uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自定义视图授权表';

-- ----------------------------
-- Table structure for cmdb_customview_ci
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_customview_ci` (
  `customview_id` bigint NOT NULL COMMENT '视图id',
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '在视图中的uuid',
  `ci_id` bigint DEFAULT NULL COMMENT '模型id',
  `sort` int DEFAULT NULL COMMENT '排序',
  `is_hidden` tinyint(1) DEFAULT NULL COMMENT '是否在topo中展示',
  `alias` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '别名',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
  `is_start` tinyint(1) DEFAULT NULL COMMENT '是否起始模型',
  PRIMARY KEY (`customview_id`,`uuid`) USING BTREE,
  KEY `idx_ci_id` (`ci_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_customview_ci';

-- ----------------------------
-- Table structure for cmdb_customview_constattr
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_customview_constattr` (
  `customview_id` bigint NOT NULL COMMENT '视图id',
  `customview_ci_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型在视图中的唯一id',
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'uuid',
  `const_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '属性名称',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
  `alias` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '别名',
  `sort` int DEFAULT NULL COMMENT '顺序',
  `is_hidden` tinyint DEFAULT '0' COMMENT '是否隐藏',
  `is_primary` tinyint DEFAULT '0' COMMENT '是否主键',
  PRIMARY KEY (`customview_id`,`uuid`) USING BTREE,
  KEY `idx_attr_id` (`const_name`) USING BTREE,
  KEY `idx_customviewci_id` (`customview_ci_uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='视图属性配置';

-- ----------------------------
-- Table structure for cmdb_customview_link
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_customview_link` (
  `customview_id` bigint NOT NULL COMMENT '视图id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '别名',
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'uuid',
  `from_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '源视图uuid',
  `to_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '目标视图uuid',
  `from_type` enum('attr','rel','ci','constattr') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源类型',
  `to_type` enum('attr','rel','ci','constattr') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '目标类型',
  `join_type` enum('join','leftjoin','rightjoin','outerjoin') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '连接方式',
  `from_ci_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '源配置项uuid',
  `to_ci_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '目标配置项uuid',
  PRIMARY KEY (`customview_id`,`uuid`) USING BTREE,
  KEY `idx_view_id` (`from_uuid`) USING BTREE,
  KEY `idx_to_attr_id` (`from_type`) USING BTREE,
  KEY `idx_fromcustomviewci_id` (`to_uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb自定义视图关联表';

-- ----------------------------
-- Table structure for cmdb_customview_rel
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_customview_rel` (
  `customview_id` bigint NOT NULL COMMENT '视图id',
  `customview_ci_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'customview_ci_uuid',
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'uuid',
  `rel_id` bigint DEFAULT NULL COMMENT 'rel_id',
  PRIMARY KEY (`customview_id`,`uuid`) USING BTREE,
  KEY `idx_customci_uuid` (`customview_ci_uuid`) USING BTREE,
  KEY `idx_rel_id` (`rel_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_customview_rel';

-- ----------------------------
-- Table structure for cmdb_customview_tag
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_customview_tag` (
  `customview_id` bigint NOT NULL COMMENT '视图id',
  `tag_id` bigint NOT NULL COMMENT '标签id',
  PRIMARY KEY (`customview_id`,`tag_id`) USING BTREE,
  KEY `idx_tag_id` (`tag_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb自定义视图标签表';

-- ----------------------------
-- Table structure for cmdb_customview_template
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_customview_template` (
  `customview_id` bigint NOT NULL COMMENT '自定义模板id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置',
  `template` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '内容模板',
  `is_active` tinyint(1) DEFAULT NULL COMMENT '是否激活',
  PRIMARY KEY (`customview_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb自定义视图模板\r\n';

-- ----------------------------
-- Table structure for cmdb_graph
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_graph` (
  `id` bigint NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `is_active` tinyint DEFAULT NULL,
  `icon` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `type` enum('private','public','scene') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `fcd` timestamp(3) NULL DEFAULT NULL,
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `lcd` timestamp(3) NULL DEFAULT NULL,
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for cmdb_graph_auth
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_graph_auth` (
  `graph_id` bigint NOT NULL,
  `auth_type` enum('user','team','role','common') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `auth_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`graph_id`,`auth_type`,`auth_uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for cmdb_graph_cientity
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_graph_cientity` (
  `graph_id` bigint NOT NULL,
  `cientity_id` bigint NOT NULL,
  PRIMARY KEY (`graph_id`,`cientity_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for cmdb_graph_rel
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_graph_rel` (
  `from_graph_id` bigint NOT NULL,
  `to_graph_id` bigint NOT NULL,
  PRIMARY KEY (`from_graph_id`,`to_graph_id`) USING BTREE,
  KEY `idx_to_graph_id` (`to_graph_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for cmdb_group
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_group` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名字',
  `type` enum('readonly','maintain') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类型',
  `is_active` tinyint DEFAULT NULL COMMENT '是否激活',
  `cientity_count` int DEFAULT NULL COMMENT '配置项数量',
  `status` enum('doing','done') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '异常',
  `fcd` timestamp(3) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '创建日期',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改日期',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `server_id` int DEFAULT NULL COMMENT '服务器id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_server_id` (`server_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='团体';

-- ----------------------------
-- Table structure for cmdb_group_auth
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_group_auth` (
  `group_id` bigint NOT NULL COMMENT '引用cmdb_group表id',
  `auth_type` enum('user','role','team','common') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'user,role,team',
  `auth_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'auth uuid',
  PRIMARY KEY (`group_id`,`auth_uuid`,`auth_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_group_auth';

-- ----------------------------
-- Table structure for cmdb_import_audit
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_import_audit` (
  `id` bigint NOT NULL COMMENT 'ID',
  `import_user` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '导入用户',
  `import_date` timestamp(3) NULL DEFAULT NULL COMMENT '导入时间',
  `finish_date` timestamp(3) NULL DEFAULT NULL COMMENT '完成时间',
  `action` enum('append','update','all') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'append：增量导入；update：存量导入；all：全部导入',
  `file_id` bigint DEFAULT NULL COMMENT '导入附件的id',
  `ci_id` bigint DEFAULT NULL COMMENT '引用cmdb_ci的id',
  `success_count` int DEFAULT NULL COMMENT '导入数量',
  `failed_count` int DEFAULT NULL COMMENT '失败数量',
  `status` enum('running','success','failed') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态',
  `error` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '错误信息',
  `total_count` int DEFAULT NULL COMMENT '总数',
  `server_id` int DEFAULT NULL COMMENT '应用ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_import_date` (`import_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb批量导入审计表';

-- ----------------------------
-- Table structure for cmdb_import_file
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_import_file` (
  `file_id` bigint NOT NULL COMMENT '文件ID',
  PRIMARY KEY (`file_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_import_file';

-- ----------------------------
-- Table structure for cmdb_legalvalid
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_legalvalid` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `type` enum('ci','custom') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '校验类型',
  `ci_id` bigint DEFAULT NULL COMMENT '模型id',
  `rule` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '规则',
  `cron` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '定时表达式',
  `is_active` tinyint(1) DEFAULT NULL COMMENT '是否激活',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_ci_id` (`ci_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='合规设置表';

-- ----------------------------
-- Table structure for cmdb_rel
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_rel` (
  `id` bigint NOT NULL COMMENT 'ID',
  `type_id` bigint DEFAULT NULL COMMENT '类型id',
  `input_type` enum('at','mt') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'mt' COMMENT '录入方式',
  `from_ci_id` bigint NOT NULL COMMENT '来源模型id',
  `from_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '来源端名称',
  `from_label` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '来源端标签',
  `from_rule` enum('1:N','0:N','1:1','0:1','O','N') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '来源端规则',
  `from_group_id` bigint DEFAULT NULL COMMENT '来源端分组,引用cmdb_relgroup的id',
  `from_is_unique` tinyint DEFAULT NULL COMMENT '来源引用是否唯一',
  `from_is_cascade_delete` tinyint DEFAULT '0' COMMENT '来源端是否级联删除',
  `from_is_required` tinyint DEFAULT NULL COMMENT '来源端是否必填',
  `to_ci_id` bigint NOT NULL COMMENT '目标端模型id',
  `to_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标端名称',
  `to_label` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标端标签',
  `to_rule` enum('1:N','0:N','0:1','1:1','O','N') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标端规则',
  `to_group_id` bigint DEFAULT NULL COMMENT '目标端分组,引用cmdb_relgroup的id',
  `to_is_unique` tinyint DEFAULT NULL COMMENT '目标引用是否唯一',
  `to_is_cascade_delete` tinyint DEFAULT '0' COMMENT '目标端是否级联删除',
  `to_is_required` tinyint DEFAULT NULL COMMENT '目标端是否必填',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_from_to_name` (`from_name`,`to_name`) USING BTREE,
  UNIQUE KEY `uk_fromlabel_tolabel` (`from_label`,`to_label`) USING BTREE,
  KEY `idx_from_ciid` (`from_ci_id`) USING BTREE,
  KEY `idx_to_ciid` (`to_ci_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='CI关联类型表';

-- ----------------------------
-- Table structure for cmdb_relativerel
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_relativerel` (
  `rel_id` bigint NOT NULL COMMENT '关系id',
  `from_path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '上游端路径',
  `to_path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '下游端路径',
  `path_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '全路径hash',
  `relative_rel_id` bigint NOT NULL COMMENT '级联关系id',
  PRIMARY KEY (`rel_id`,`relative_rel_id`,`path_hash`) USING BTREE,
  UNIQUE KEY `uk_path` (`path_hash`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb引用关系';

-- ----------------------------
-- Table structure for cmdb_relentity
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_relentity` (
  `id` bigint NOT NULL COMMENT 'ID',
  `rel_id` bigint NOT NULL COMMENT '关联属性ID',
  `from_cientity_id` bigint NOT NULL COMMENT '来源配置项ID',
  `to_cientity_id` bigint NOT NULL COMMENT '目标配置项ID',
  `transaction_id` bigint DEFAULT NULL COMMENT '生效事务id',
  `from_index` int DEFAULT NULL COMMENT '数据序号，用于检索时提高效率，只会生成前N条数据的序号',
  `to_index` int DEFAULT NULL COMMENT '数据序号，用于检索时提高效率，只会生成前N条数据的序号',
  `insert_time` timestamp(3) NULL DEFAULT NULL COMMENT '插入时间',
  `renew_time` timestamp(3) NULL DEFAULT NULL COMMENT '更新时间，用于判断老化',
  `valid_day` int DEFAULT NULL COMMENT '有效天数，为空代表长期有效',
  `expired_time` timestamp(3) NULL DEFAULT NULL COMMENT '过期日期',
  `relativerel_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联关系hash',
  `source_relentity_id` bigint DEFAULT NULL COMMENT '源头关系id，不为空代表是级联关系，源头关系删除后这条关系也要删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_from_to` (`rel_id`,`from_cientity_id`,`to_cientity_id`) USING BTREE,
  KEY `idx_from_cientity_id` (`from_cientity_id`) USING BTREE,
  KEY `idx_to_cientity_id` (`to_cientity_id`) USING BTREE,
  KEY `idx_from_index` (`rel_id`,`from_cientity_id`,`from_index`) USING BTREE,
  KEY `idx_to_index` (`rel_id`,`to_cientity_id`,`to_index`) USING BTREE,
  KEY `idx_expiredtime` (`expired_time`) USING BTREE,
  KEY `idx_source_relentity_id` (`source_relentity_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb配置项实例关联表';

-- ----------------------------
-- Table structure for cmdb_relgroup
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_relgroup` (
  `id` bigint NOT NULL COMMENT 'id',
  `ci_id` bigint NOT NULL COMMENT '配置项id',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_group_name` (`ci_id`,`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_relgroup';

-- ----------------------------
-- Table structure for cmdb_reltype
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_reltype` (
  `id` bigint NOT NULL COMMENT 'ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `is_showintopo` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否在拓扑图中显示',
  `from_cnname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '调用方名称',
  `to_cnname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '目标方名称',
  `from_enname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '调用方英文名',
  `to_enname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '目标方英文名',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_reltype';

-- ----------------------------
-- Table structure for cmdb_resourcecenter_account
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_resourcecenter_account` (
  `id` bigint NOT NULL COMMENT '主键id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '账号',
  `password` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '密码',
  `protocol_id` bigint NOT NULL COMMENT '协议id',
  `type` enum('public','private') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类型',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `fcd` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `lcd` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否该协议默认账号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY ` idx_protocolid` (`protocol_id`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb资源中心账号表';

-- ----------------------------
-- Table structure for cmdb_resourcecenter_account_ip
-- ----------------------------
-- CREATE TABLE IF NOT EXISTS `cmdb_resourcecenter_account_ip` (
--   `account_id` bigint NOT NULL COMMENT '账号id',
--   `ip` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '账号对应的ip',
--   PRIMARY KEY (`ip`) USING BTREE
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用于ip账号匹配';

-- ----------------------------
-- Table structure for cmdb_resourcecenter_account_protocol
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_resourcecenter_account_protocol` (
  `id` bigint NOT NULL COMMENT '协议id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '协议名称',
  `port` int DEFAULT NULL COMMENT '协议端口',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最后一次修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后一次修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb资源中心账号协议表';

-- ----------------------------
-- Table structure for cmdb_resourcecenter_account_tag
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_resourcecenter_account_tag` (
  `account_id` bigint NOT NULL COMMENT '账号id',
  `tag_id` bigint NOT NULL COMMENT '标签id',
  PRIMARY KEY (`account_id`,`tag_id`) USING BTREE,
  KEY `idx_tag_id` (`tag_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_resourcecenter_account_tag';

-- ----------------------------
-- Table structure for cmdb_resourcecenter_entity
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_resourcecenter_entity` (
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `label` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标识',
  `status` enum('ready','pending','error','') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态',
  `error` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'error',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '描述',
  `init_time` timestamp(3) NULL DEFAULT NULL COMMENT 'init time',
  `ci_id` bigint DEFAULT NULL COMMENT '模型id',
  `config` mediumtext COLLATE utf8mb4_general_ci COMMENT '配置信息',
  PRIMARY KEY (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_resourcecenter_entity';

-- ----------------------------
-- Table structure for cmdb_resourcecenter_resource_account
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_resourcecenter_resource_account` (
  `resource_id` bigint NOT NULL COMMENT '资源id',
  `account_id` bigint NOT NULL COMMENT '账号id',
  PRIMARY KEY (`resource_id`,`account_id`) USING BTREE,
  KEY `idx_account_id` (`account_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资源中心资源与账号关联表';

-- ----------------------------
-- Table structure for cmdb_resourcecenter_resource_tag
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_resourcecenter_resource_tag` (
  `resource_id` bigint NOT NULL COMMENT '资源id',
  `tag_id` bigint NOT NULL COMMENT '标签id',
  PRIMARY KEY (`resource_id`,`tag_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_resourcecenter_resource_tag';

-- ----------------------------
-- Table structure for cmdb_schema_audit
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_schema_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `target_id` bigint NOT NULL COMMENT '目标id',
  `target_type` enum('ci','attr','rel','cientity') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标类型',
  `action` enum('insert','delete','update') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '动作',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  `server_id` int NOT NULL COMMENT 'server id',
  `data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'data',
  `is_failed` tinyint(1) DEFAULT '0' COMMENT '是否失败',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk` (`target_id`,`target_type`) USING BTREE,
  KEY `idx_lcd` (`lcd`) USING BTREE,
  KEY `idx_server_id` (`server_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=99 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_schema_audit';

-- ----------------------------
-- Table structure for cmdb_sync_audit
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_sync_audit` (
  `id` bigint NOT NULL COMMENT 'id',
  `ci_collection_id` bigint DEFAULT NULL COMMENT '同步配置id',
  `start_time` timestamp(3) NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` timestamp(3) NULL DEFAULT NULL COMMENT '结束时间',
  `data_count` int DEFAULT NULL COMMENT '需处理的数据量',
  `input_from` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '触发方式',
  `status` enum('doing','done') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态',
  `transaction_group_id` bigint DEFAULT NULL COMMENT '事务组id',
  `server_id` int DEFAULT NULL COMMENT '服务器id',
  `error` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '异常',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_ci_collection_id` (`ci_collection_id`) USING BTREE,
  KEY `idx_start_time` (`start_time`) USING BTREE,
  KEY `idx_end_time` (`end_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb同步记录表';

-- ----------------------------
-- Table structure for cmdb_sync_ci_collection
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_sync_ci_collection` (
  `id` bigint NOT NULL COMMENT 'id',
  `ci_id` bigint NOT NULL COMMENT '配置项id',
  `collection_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'collection name',
  `parent_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父属性，可以为空',
  `collect_mode` enum('initiative','passive') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'collect mode',
  `is_auto_commit` tinyint DEFAULT NULL COMMENT '是否自动提交',
  `match_mode` enum('key','level') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'key' COMMENT '匹配模式',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `last_sync_date` timestamp(3) NULL DEFAULT NULL COMMENT '最后一次成功同步时间',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '说明',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk` (`ci_id`,`collection_name`,`parent_key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_sync_ci_collection';

-- ----------------------------
-- Table structure for cmdb_sync_mapping
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_sync_mapping` (
  `id` bigint NOT NULL COMMENT 'id',
  `ci_collection_id` bigint DEFAULT NULL COMMENT '模型集合id',
  `rel_id` bigint DEFAULT NULL COMMENT '关系id',
  `direction` enum('from','to') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关系方向',
  `attr_id` bigint DEFAULT NULL COMMENT '属性id',
  `field` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集合字段，支持jsonpath',
  `action` enum('insert','replace','delete') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'insert' COMMENT '动作',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_sync_mapping';

-- ----------------------------
-- Table structure for cmdb_sync_policy
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_sync_policy` (
  `id` bigint NOT NULL COMMENT '主键',
  `ci_collection_id` bigint DEFAULT NULL COMMENT '模型id',
  `ci_id` bigint DEFAULT NULL COMMENT '模型id',
  `is_active` tinyint(1) DEFAULT NULL COMMENT '是否激活',
  `condition` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'mongodb过滤条件',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_ci_id` (`ci_collection_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb同步策略';

-- ----------------------------
-- Table structure for cmdb_sync_schedule
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_sync_schedule` (
  `id` bigint NOT NULL COMMENT 'id',
  `cron` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'cron表达式',
  `policy_id` bigint NOT NULL COMMENT '策略id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_policy_id` (`policy_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb同步计划表';

-- ----------------------------
-- Table structure for cmdb_sync_unique
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_sync_unique` (
  `ci_collection_id` bigint NOT NULL COMMENT '配置集合id',
  `attr_id` bigint NOT NULL COMMENT '配置属性id',
  PRIMARY KEY (`ci_collection_id`,`attr_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='配置同步唯一属性';

-- ----------------------------
-- Table structure for cmdb_tag
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_tag` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '描述',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb标签表';

-- ----------------------------
-- Table structure for cmdb_transaction
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_transaction` (
  `id` bigint unsigned NOT NULL COMMENT '事务ID',
  `ci_id` bigint DEFAULT NULL COMMENT '模型id',
  `status` enum('commited','uncommit','recover') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否提交',
  `create_user` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建用户ID',
  `commit_user` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提交用户ID',
  `recover_user` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '恢复用户',
  `input_from` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据来源类型',
  `source` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源',
  `expire_time` timestamp(3) NULL DEFAULT NULL COMMENT '快照超时时间，这时候以后没提交，自动删除',
  `create_time` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `commit_time` timestamp(3) NULL DEFAULT NULL COMMENT '提交时间',
  `recover_time` timestamp(3) NULL DEFAULT NULL COMMENT '恢复时间',
  `error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '最后一次错误原因',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改备注',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_commit_time` (`commit_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb事务表';

-- ----------------------------
-- Table structure for cmdb_transactiongroup
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_transactiongroup` (
  `id` bigint NOT NULL COMMENT 'id',
  `transaction_id` bigint NOT NULL COMMENT '事务id',
  PRIMARY KEY (`id`,`transaction_id`) USING BTREE,
  KEY `idx_transaction_id` (`transaction_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb事务组表';

-- ----------------------------
-- Table structure for cmdb_validator
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_validator` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `handler` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '处理器类路径',
  `config` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'config',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `error_template` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误模版',
  `is_active` tinyint(1) DEFAULT NULL COMMENT '是否激活',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb校验器表';

-- ----------------------------
-- Table structure for cmdb_view
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_view` (
  `ci_id` bigint NOT NULL COMMENT '模型id',
  `item_id` bigint NOT NULL COMMENT '关系或属性id',
  `type` enum('attr','relfrom','relto','const') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型',
  `sort` int NOT NULL COMMENT '排序',
  `show_type` enum('none','all','list','detail') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '显示方式',
  `allow_edit` tinyint(1) DEFAULT NULL COMMENT '允许修改',
  PRIMARY KEY (`ci_id`,`item_id`,`type`) USING BTREE,
  KEY `idx_item_id` (`item_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb视图表';

-- ----------------------------
-- Table structure for cmdb_viewconst
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmdb_viewconst` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `label` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '显示名',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='cmdb_viewconst';

-- ----------------------------
-- Table structure for cmdb_resourcecenter_type_ci
-- ----------------------------
CREATE TABLE `cmdb_resourcecenter_type_ci` (
  `ci_id` bigint NOT NULL COMMENT '模型id',
  PRIMARY KEY (`ci_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产清单树形模型';
