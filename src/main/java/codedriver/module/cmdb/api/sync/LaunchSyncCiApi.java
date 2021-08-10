/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.sync;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.sync.SyncConfigVo;
import codedriver.framework.cmdb.dto.sync.SyncMappingVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.ci.CiUniqueAttrNotFoundException;
import codedriver.framework.cmdb.exception.sync.SyncConfigNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.sync.SyncConfigMapper;
import codedriver.module.cmdb.service.sync.CiSyncManager;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class LaunchSyncCiApi extends PrivateApiComponentBase {

    @Autowired
    private SyncConfigMapper syncConfigMapper;

    @Resource
    private CiMapper ciMapper;


    @Override
    public String getToken() {
        return "/cmdb/sync/lanuch";
    }

    @Override
    public String getName() {
        return "发起配置项自动发现";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "配置id")})
    @Description(desc = "发起配置项自动发现接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        SyncConfigVo syncConfigVo = syncConfigMapper.getSyncConfigById(id);
        if (syncConfigVo == null) {
            throw new SyncConfigNotFoundException(id);
        }

        List<SyncMappingVo> mappingList = syncConfigVo.getMappingList();
        if (CollectionUtils.isNotEmpty(mappingList) && syncConfigVo.getCiId() != null) {
            CiVo ciVo = ciMapper.getCiById(syncConfigVo.getCiId());
            if (ciVo == null) {
                throw new CiNotFoundException(syncConfigVo.getCiId());
            }
            if (CollectionUtils.isEmpty(ciVo.getUniqueAttrIdList())) {
                throw new CiUniqueAttrNotFoundException(ciVo.getLabel());
            }

            syncConfigVo.setCiVo(ciVo);
            CiSyncManager.doSync(syncConfigVo);
        }
        return null;
    }

}
