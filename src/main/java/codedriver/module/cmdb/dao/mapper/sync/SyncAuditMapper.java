/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.sync;

import codedriver.framework.cmdb.dto.sync.SyncAuditVo;

public interface SyncAuditMapper {
    void insertSyncAudit(SyncAuditVo syncAuditVo);
}
