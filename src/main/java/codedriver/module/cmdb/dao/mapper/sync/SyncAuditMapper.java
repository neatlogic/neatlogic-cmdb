/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.sync;

import codedriver.framework.cmdb.dto.sync.SyncAuditVo;

import java.util.List;

public interface SyncAuditMapper {
    List<SyncAuditVo> getDoingSyncByServerId(Integer serverId);

    List<SyncAuditVo> getDoingSyncByCiId(Long ciId);

    List<SyncAuditVo> searchSyncAudit(SyncAuditVo syncAuditVo);

    int searchSyncAuditCount(SyncAuditVo syncAuditVo);

    void insertSyncAudit(SyncAuditVo syncAuditVo);

    void updateSyncAuditStatus(SyncAuditVo syncAuditVo);

    void deleteSyncAuditById(Long syncAuditId);

    void deleteAuditByDayBefore(int dayBefore);

}
