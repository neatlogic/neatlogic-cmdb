/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.config;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterResourceFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.RESOURCECENTER_MODIFY;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author linbq
 * @since 2021/11/9 11:28
 **/
@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetResourceEntityApi extends PrivateApiComponentBase {
    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resourceentity/get";
    }

    @Override
    public String getName() {
        return "获取资源配置信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "视图名")
    })
    @Output({
            @Param(name = "Return", explode = ResourceEntityVo.class, desc = "配置信息")
    })
    @Description(desc = "获取资源配置信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String name = paramObj.getString("name");
        ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(name);
        if (resourceEntityVo == null) {
            throw new ResourceCenterResourceFoundException(name);
        }
        return resourceEntityVo;
    }
}
