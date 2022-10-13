/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.sync;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.sync.SyncCiCollectionVo;
import codedriver.framework.cmdb.enums.sync.CollectMode;
import codedriver.framework.cmdb.exception.sync.InitiativeSyncCiCollectionIsExistsException;
import codedriver.framework.cmdb.exception.sync.SyncMappingNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.SYNC_MODIFY;
import codedriver.module.cmdb.dao.mapper.sync.SyncMapper;
import codedriver.module.cmdb.service.sync.SyncService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveSyncCiCollectionApi extends PrivateApiComponentBase {

    @Resource
    private SyncService syncService;

    @Resource
    private SyncMapper syncMapper;


    @Override
    public String getToken() {
        return "/cmdb/sync/cicollection/save";
    }

    @Override
    public String getName() {
        return "保存配置项集合映射设置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，存在代表修改，不存在代表新增"),
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "collectionName", isRequired = true, maxLength = 200, xss = true, type = ApiParamType.STRING, desc = "集合名称"),
            @Param(name = "parentKey", type = ApiParamType.STRING, desc = "父属性"),
            @Param(name = "mappingList", type = ApiParamType.JSONARRAY, desc = "映射内容"),
            @Param(name = "description", type = ApiParamType.STRING, xss = true, desc = "说明", maxLength = 500),
            @Param(name = "uniqueAttrIdList", type = ApiParamType.JSONARRAY, desc = "唯一规则")})
    @Description(desc = "保存配置项集合映射设置接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        SyncCiCollectionVo syncCiCollectionVo = JSONObject.toJavaObject(jsonObj, SyncCiCollectionVo.class);

        if (CollectionUtils.isEmpty(syncCiCollectionVo.getMappingList())) {
            throw new SyncMappingNotFoundException();
        }
        if (syncCiCollectionVo.getCollectMode().equals(CollectMode.INITIATIVE.getValue()) && syncMapper.checkInitiativeSyncCiCollectionIsExists(syncCiCollectionVo) > 0) {
            throw new InitiativeSyncCiCollectionIsExistsException(syncCiCollectionVo.getCollectionName());
        }
        syncService.saveSyncCiCollection(syncCiCollectionVo);
        return null;
    }
}
