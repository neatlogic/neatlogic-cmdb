/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.sync;

import codedriver.framework.cmdb.dto.sync.SyncCiCollectionVo;

import java.util.List;

public interface SyncService {
    void saveSyncCiCollection(SyncCiCollectionVo syncCiCollectionVo);

    List<SyncCiCollectionVo> searchSyncCiCollection(SyncCiCollectionVo syncCiCollectionVo);

    void deleteSyncCiCollectionById(Long id);
}
