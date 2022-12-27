/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListResourceCustomApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "高级查询资源中心数据列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Param(name = "conditionConfig", type = ApiParamType.JSONOBJECT, desc = "条件设置，为空则使用数据库中保存的条件")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ResourceVo resourceVo = JSONObject.toJavaObject(paramObj, ResourceVo.class);
        return null;
    }

    @Override
    public String getToken() {
        return "resourcecenter/resource/custom/list";
    }
}
