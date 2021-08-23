/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.sync;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.sync.SyncCiCollectionVo;
import codedriver.framework.cmdb.dto.sync.SyncMappingVo;
import codedriver.framework.cmdb.dto.sync.SyncPolicyVo;
import codedriver.framework.cmdb.dto.sync.SyncScheduleVo;
import codedriver.framework.cmdb.exception.sync.SyncCiCollectionIsExistsException;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.sync.SyncMapper;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class SyncServiceImpl implements SyncService {
    @Resource
    private SyncMapper syncMapper;

    @Resource
    private CiMapper ciMapper;

    public void saveSyncPolicy(SyncPolicyVo syncPolicyVo) {
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
            throw new SyncCiCollectionIsExistsException(syncCiCollectionVo);
        }
        SyncCiCollectionVo oldSyncCiCollectionVo = syncMapper.getSyncCiCollectionById(syncCiCollectionVo.getId());
        if (oldSyncCiCollectionVo == null) {
            syncCiCollectionVo.setFcu(UserContext.get().getUserUuid());
            syncMapper.insertSyncCiCollection(syncCiCollectionVo);
        } else {
            syncCiCollectionVo.setLcu(UserContext.get().getUserUuid());
            syncMapper.updateSyncCiCollection(syncCiCollectionVo);
            syncMapper.deleteSyncMappingByCiCollectionId(syncCiCollectionVo.getId());
        }
        if (CollectionUtils.isNotEmpty(syncCiCollectionVo.getMappingList())) {
            for (SyncMappingVo syncMappingVo : syncCiCollectionVo.getMappingList()) {
                syncMappingVo.setCiCollectionId(syncCiCollectionVo.getId());
                syncMapper.insertSyncMapping(syncMappingVo);
            }
        }
    }


    @Override
    public List<SyncCiCollectionVo> searchSyncCiCollection(SyncCiCollectionVo syncCiCollectionVo) {
        List<SyncCiCollectionVo> syncCiCollectionList = syncMapper.searchSyncCiCollection(syncCiCollectionVo);
        if (CollectionUtils.isNotEmpty(syncCiCollectionList)) {
            int rowNum = syncMapper.searchSyncCiCollectionCount(syncCiCollectionVo);
            syncCiCollectionVo.setRowNum(rowNum);
        }
        return syncCiCollectionList;
    }

}
