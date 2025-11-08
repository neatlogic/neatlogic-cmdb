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

import neatlogic.framework.cmdb.dto.sync.SyncAuditVo;
import neatlogic.framework.cmdb.dto.sync.SyncDataAuditVo;
import neatlogic.framework.cmdb.dto.sync.SyncDataHashVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SyncAuditMapper {
    List<SyncAuditVo> getDoingSyncByServerId(Integer serverId);

    List<SyncAuditVo> getDoingSyncByCiId(Long ciId);

    List<SyncAuditVo> searchSyncAudit(SyncAuditVo syncAuditVo);

    List<SyncDataAuditVo> searchSyncDataAudit(SyncDataAuditVo syncDataAuditVo);

    SyncAuditVo getSyncAuditStatusById(Long id);

    SyncAuditVo getSyncAuditById(Long id);

    SyncDataHashVo getSyncDataHashById(@Param("dataId") String dataId, @Param("collectionName") String collectionName);

    int searchSyncDataAuditCount(SyncDataAuditVo syncDataAuditVo);

    int searchSyncAuditCount(SyncAuditVo syncAuditVo);

    void insertSyncAudit(SyncAuditVo syncAuditVo);

    void insertSyncDataAudit(SyncDataAuditVo syncDataAuditVo);

    void updateSyncAuditToEnd(SyncAuditVo syncAuditVo);

    void updateSyncAuditToStart(SyncAuditVo syncAuditVo);

    void updateSyncAuditDataCount(SyncAuditVo syncAuditVo);

    void saveSyncDataHash(SyncDataHashVo syncDataHashVo);

    void deleteSyncAuditById(Long syncAuditId);

    void deleteAuditByDayBefore(int dayBefore);

    void deleteDataHashByDayBefore(int dayBefore);

    void deleteSyncDataHashById(@Param("dataId") String dataId, @Param("collectionName") String collectionName);

    void deleteSyncDataAuditByDataIdAndCollectionId(@Param("dataId") String dataId, @Param("ciCollectionId") Long collectionId);

}
