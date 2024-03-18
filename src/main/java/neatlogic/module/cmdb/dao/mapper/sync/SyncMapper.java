/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
