/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetResourceEntityByNameApi extends PrivateApiComponentBase {
    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Override
    public String getName() {
        return "获取资源定义";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "name", isRequired = true, type = ApiParamType.STRING, desc = "资源定义名称")
    })
    @Output({
            @Param(explode = ResourceEntityVo.class)
    })
    @Description(desc = "获取资源定义接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return resourceEntityMapper.getResourceEntityByName(paramObj.getString("name"));
    }

    @Override
    public String getToken() {
        return "resourcecenter/resourceentity/get";
    }
}
