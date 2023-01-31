/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.resourcecenter.appmodule;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.resourcecenter.AppModuleVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAppModuleListForTreeApi extends PrivateApiComponentBase {

    @Resource
    private ResourceMapper resourceMapper;

    @Override
    public String getToken() {
        return "resourcecenter/appmodule/list/fortree";
    }

    @Override
    public String getName() {
        return "查询资源模块列表树";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id")
    })
    @Output({
            @Param(name = "tbodyList", explode = AppModuleVo[].class, desc = "模块列表")
    })
    @Description(desc = "查询资源模块列表树")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        List<AppModuleVo> tbodyList = resourceMapper.getAppModuleListByAppSystemId(appSystemId);
        return tbodyList;
    }
}
