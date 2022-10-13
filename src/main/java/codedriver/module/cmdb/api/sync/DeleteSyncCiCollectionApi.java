/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.sync;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.sync.SyncPolicyVo;
import codedriver.framework.cmdb.exception.sync.CiCollectionIsInUsedException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.SYNC_MODIFY;
import codedriver.module.cmdb.dao.mapper.sync.SyncMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteSyncCiCollectionApi extends PrivateApiComponentBase {

    @Resource
    private SyncMapper syncMapper;

    @Override
    public String getToken() {
        return "/cmdb/sync/cicollection/delete";
    }

    @Override
    public String getName() {
        return "删除配置项集合映射信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "模型id")})
    @Description(desc = "删除配置项集合映射信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        List<SyncPolicyVo> policyList = syncMapper.getSyncPolicyByCiCollectionId(id);
        if (CollectionUtils.isNotEmpty(policyList)) {
            throw new CiCollectionIsInUsedException(policyList);
        }
        syncMapper.deleteSyncCiCollectionById(id);
        return null;
    }

}
