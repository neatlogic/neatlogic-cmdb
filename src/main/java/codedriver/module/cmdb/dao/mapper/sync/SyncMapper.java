/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.sync;

import codedriver.framework.cmdb.dto.sync.SyncCiCollectionVo;
import codedriver.framework.cmdb.dto.sync.SyncMappingVo;
import codedriver.framework.cmdb.dto.sync.SyncPolicyVo;
import codedriver.framework.cmdb.dto.sync.SyncScheduleVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SyncMapper {
    SyncCiCollectionVo getInitiativeSyncCiCollectionByCollectName(String collectName);

    int checkInitiativeSyncCiCollectionIsExists(SyncCiCollectionVo syncCiCollectionVo);

    List<SyncPolicyVo> getSyncPolicyByCiCollectionId(Long collectionId);

    List<SyncScheduleVo> getAllActivePolicySchedule();

    SyncScheduleVo getSyncScheduleById(Long id);

    List<SyncPolicyVo> searchSyncPolicy(SyncPolicyVo syncPolicyVo);

    int checkCiHasSyncCiCollection(Long ciId);

    int checkSyncCiCollectionIsExists(SyncCiCollectionVo syncCiCollectionVo);

    SyncPolicyVo getSyncPolicyById(Long ciId);

    List<SyncCiCollectionVo> searchSyncCiCollection(SyncCiCollectionVo syncCiCollectionVo);

    int searchSyncCiCollectionCount(SyncCiCollectionVo syncCiCollectionVo);

    SyncCiCollectionVo getSyncCiCollectionById(Long id);

    List<SyncCiCollectionVo> getSyncCiCollectionByCollectionName(String collectionName);

    SyncCiCollectionVo getSyncCiCollectionByCiIdAndCollectionName(@Param("ciId") Long ciId, @Param("collectionName") String collectionName);

    void insertSyncPolicy(SyncPolicyVo syncPolicyVo);

    void insertSyncSchedule(SyncScheduleVo syncScheduleVo);

    void updateSyncPolicy(SyncPolicyVo syncPolicyVo);

    void updateSyncCiCollection(SyncCiCollectionVo syncCiCollectionVo);

    void updateSyncCiCollectionLastSyncDate(Long id);

    void insertSyncCiCollection(SyncCiCollectionVo syncCiCollectionVo);

    void insertSyncMapping(SyncMappingVo syncMappingVo);

    void deleteSyncPolicyById(Long policyId);

    void deleteSyncScheduleByPolicyId(Long policyId);

    void deleteSyncMappingByCiCollectionId(Long ciCollectionId);

    void deleteSyncCiCollectionById(Long ciCollectionId);
}
