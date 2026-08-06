UPDATE `cmdb_resourcecenter_account`
SET `is_default` = 0
WHERE `is_default` IS NULL;
