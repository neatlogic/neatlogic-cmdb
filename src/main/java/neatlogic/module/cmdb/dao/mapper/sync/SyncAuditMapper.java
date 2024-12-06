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

    void deleteSyncDataHashById(@Param("dataId") String dataId, @Param("collectionName") String collectionName);

    void deleteSyncDataAuditByDataIdAndCollectionId(@Param("dataId") String dataId, @Param("ciCollectionId") Long collectionId);

}
