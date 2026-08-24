INSERT IGNORE INTO `cmdb_cientity_tag` (`cientity_id`, `tag_id`)
SELECT `resource_id`, `tag_id`
FROM `cmdb_resourcecenter_resource_tag`;
