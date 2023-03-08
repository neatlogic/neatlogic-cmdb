/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.service.sync;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.sync.*;
import neatlogic.framework.cmdb.exception.sync.SyncCiCollectionIsExistsException;
import neatlogic.framework.cmdb.exception.sync.SyncCiCollectionNotFoundException;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.sync.SyncAuditMapper;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class SyncServiceImpl implements SyncService {
    @Resource
    private SyncMapper syncMapper;

    @Resource
    private SyncAuditMapper syncAuditMapper;

    @Resource
    private CiMapper ciMapper;

    public void saveSyncPolicy(SyncPolicyVo syncPolicyVo) {
        SyncCiCollectionVo syncCiCollectionVo = syncMapper.getSyncCiCollectionById(syncPolicyVo.getCiCollectionId());
        if (syncCiCollectionVo == null) {
            throw new SyncCiCollectionNotFoundException(syncPolicyVo.getCiCollectionId());
        }
        syncPolicyVo.setCiId(syncCiCollectionVo.getCiId());
        SyncPolicyVo vo = syncMapper.getSyncPolicyById(syncPolicyVo.getId());
        if (vo == null) {
            syncMapper.insertSyncPolicy(syncPolicyVo);
        } else {
            syncMapper.deleteSyncScheduleByPolicyId(syncPolicyVo.getId());
            syncMapper.updateSyncPolicy(syncPolicyVo);
        }
        if (CollectionUtils.isNotEmpty(syncPolicyVo.getCronList())) {
            for (SyncScheduleVo cron : syncPolicyVo.getCronList()) {
                cron.setPolicyId(syncPolicyVo.getId());
                syncMapper.insertSyncSchedule(cron);
            }
        }
    }


    @Override
    public void saveSyncCiCollection(SyncCiCollectionVo syncCiCollectionVo) {
        if (syncMapper.checkSyncCiCollectionIsExists(syncCiCollectionVo) > 0) {
            CiVo ciVo = ciMapper.getCiById(syncCiCollectionVo.getCiId());
            syncCiCollectionVo.setCiLabel(ciVo.getLabel());
            syncCiCollectionVo.setCiName(ciVo.getName());
            if (StringUtils.isNotEmpty(syncCiCollectionVo.getParentKey())) {
                throw new SyncCiCollectionIsExistsException(syncCiCollectionVo, true);

            } else {
                throw new SyncCiCollectionIsExistsException(syncCiCollectionVo);
            }
        }
        SyncCiCollectionVo oldSyncCiCollectionVo = syncMapper.getSyncCiCollectionById(syncCiCollectionVo.getId());
        if (oldSyncCiCollectionVo == null) {
            syncCiCollectionVo.setFcu(UserContext.get().getUserUuid());
            syncMapper.insertSyncCiCollection(syncCiCollectionVo);
        } else {
            syncCiCollectionVo.setLcu(UserContext.get().getUserUuid());
            syncMapper.deleteSyncUniqueByCiCollectionId(syncCiCollectionVo.getId());
            syncMapper.updateSyncCiCollection(syncCiCollectionVo);
            syncMapper.deleteSyncMappingByCiCollectionId(syncCiCollectionVo.getId());
        }
        if (CollectionUtils.isNotEmpty(syncCiCollectionVo.getMappingList())) {
            for (SyncMappingVo syncMappingVo : syncCiCollectionVo.getMappingList()) {
                syncMappingVo.setCiCollectionId(syncCiCollectionVo.getId());
                syncMapper.insertSyncMapping(syncMappingVo);
            }
        }
        if (CollectionUtils.isNotEmpty(syncCiCollectionVo.getUniqueAttrIdList())) {
            for (Long attrId : syncCiCollectionVo.getUniqueAttrIdList()) {
                syncMapper.insertSyncUnique(syncCiCollectionVo.getId(), attrId);
            }
        }
    }


    @Override
    public List<SyncCiCollectionVo> searchSyncCiCollection(SyncCiCollectionVo syncCiCollectionVo) {
        List<SyncCiCollectionVo> syncCiCollectionList = syncMapper.searchSyncCiCollection(syncCiCollectionVo);
        if (CollectionUtils.isNotEmpty(syncCiCollectionList)) {
            if (CollectionUtils.isEmpty(syncCiCollectionVo.getIdList())) {
                int rowNum = syncMapper.searchSyncCiCollectionCount(syncCiCollectionVo);
                syncCiCollectionVo.setRowNum(rowNum);
            } else {
                syncCiCollectionVo.setRowNum(syncCiCollectionList.size());
            }
            for (SyncCiCollectionVo ciCollectionVo : syncCiCollectionList) {
                SyncAuditVo syncAuditVo = new SyncAuditVo();
                syncAuditVo.setPageSize(100);
                syncAuditVo.setCiCollectionId(ciCollectionVo.getId());
                List<SyncAuditVo> syncAuditList = syncAuditMapper.searchSyncAudit(syncAuditVo);
                if (syncAuditList.size() > 0) {
                    ciCollectionVo.setStatus(syncAuditList.get(0).getStatus());
                    ciCollectionVo.setError(syncAuditList.get(0).getError());
                }
                ciCollectionVo.setExecCount(syncAuditList.size());
            }
        }
        return syncCiCollectionList;
    }

}
