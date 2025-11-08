/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.dao.mapper.sync;

import neatlogic.framework.cmdb.crossover.ISyncCrossoverMapper;
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.cmdb.dto.sync.SyncMappingVo;
import neatlogic.framework.cmdb.dto.sync.SyncPolicyVo;
import neatlogic.framework.cmdb.dto.sync.SyncScheduleVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SyncMapper extends ISyncCrossoverMapper {
    List<SyncCiCollectionVo> getPassiveSyncCiCollectionByCiId(Long ciId);

    List<SyncCiCollectionVo> getInitiativeSyncCiCollectionByCollectNameList(@Param("collectionNameList") List<String> collectionNameList);

    SyncCiCollectionVo getInitiativeSyncCiCollectionByCollectName(String collectName);

    List<SyncCiCollectionVo> getInitiativeSyncCiCollectionByCollectNameAndCiId(@Param("collectionName") String collectionName, @Param("ciId") Long ciId);

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

    List<String> getSyncCiCollectionNameListByCiNameListAndCollectMode(@Param("ciNameList") List<String> ciNameList, @Param("collectMode") String collectMode);

    String getSyncCiCollectionNameListByCiNameAndCollectMode(@Param("ciName") String ciName, @Param("collectMode") String collectMode);

    SyncCiCollectionVo getSyncCiCollectionById(Long id);

    List<SyncCiCollectionVo> getSyncCiCollectionByIdList(@Param("idList") List<Long> idList);

    List<SyncCiCollectionVo> getSyncCiCollectionByCollectionName(String collectionName);

    SyncCiCollectionVo getSyncCiCollectionByCiIdAndCollectionName(@Param("ciId") Long ciId, @Param("collectionName") String collectionName);

    void insertSyncPolicy(SyncPolicyVo syncPolicyVo);

    void insertSyncSchedule(SyncScheduleVo syncScheduleVo);

    void updateSyncPolicy(SyncPolicyVo syncPolicyVo);

    void updateSyncCiCollection(SyncCiCollectionVo syncCiCollectionVo);

    void updateSyncCiCollectionLastSyncDate(Long id);

    void insertSyncCiCollection(SyncCiCollectionVo syncCiCollectionVo);

    void insertSyncMapping(SyncMappingVo syncMappingVo);

    void insertSyncUnique(@Param("ciCollectionId") Long ciCollectionId, @Param("attrId") Long attrId);

    void deleteSyncPolicyById(Long policyId);

    void deleteSyncScheduleByPolicyId(Long policyId);

    void deleteSyncMappingByCiCollectionId(Long ciCollectionId);

    void deleteSyncUniqueByCiCollectionId(Long ciCollectionId);

    void deleteSyncCiCollectionById(Long ciCollectionId);
}
