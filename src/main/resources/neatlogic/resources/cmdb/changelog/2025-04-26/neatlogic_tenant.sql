INSERT IGNORE INTO `cmdb_resourcecenter_entity` (
    `name`,
    `label`,
    `status`,
    `description`,
    `init_time`,
    `ci_id`,
    `config`
) VALUES(
    'scence_os',
    '操作系统IP视图',
    'ready',
    '配置管理/添加tagent数据进配置项',
    NOW(3),
    '479593471418368',
    '{\"mainCi\":\"OS\",\"relNode\":{\"children\":[],\"ciLabel\":\"操作系统\",\"ciName\":\"OS\"},\"fieldMappingList\":[{\"field\":\"id\",\"fromCi\":\"OS\",\"fromAttr\":\"_id\",\"type\":\"const\"},{\"field\":\"name\",\"fromCi\":\"OS\",\"fromAttr\":\"name\",\"type\":\"attr\"},{\"field\":\"ip\",\"fromCi\":\"OS\",\"fromAttr\":\"ip\",\"type\":\"attr\"},{\"field\":\"type_id\",\"fromCi\":\"OS\",\"fromAttr\":\"_typeId\",\"type\":\"const\"},{\"field\":\"type_name\",\"fromCi\":\"OS\",\"fromAttr\":\"_typeName\",\"type\":\"const\"},{\"field\":\"type_label\",\"fromCi\":\"OS\",\"fromAttr\":\"_typeLabel\",\"type\":\"const\"}]}'
);