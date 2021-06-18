/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.appmodule;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnviromentVo;
import codedriver.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author linbq
 * @since 2021/6/16 20:06
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class EnvironmentListApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getToken() {
        return "resourcecenter/environment/list";
    }

    @Override
    public String getName() {
        return "查询资源环境列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "应用模块id")
    })
    @Output({
            @Param(name = "tbodyList", explode = AppEnviromentVo[].class, desc = "资源环境列表")
    })
    @Description(desc = "查询资源环境列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String schemaName = TenantContext.get().getDataDbName();
        Long appModuleId = paramObj.getLong("appModuleId");
        if (appModuleId != null) {
            if (resourceCenterMapper.checkAppModuleIsExists(appModuleId, schemaName) == 0) {
                throw new AppModuleNotFoundException(appModuleId);
            }
        }
        List<AppEnviromentVo> enviromentList = resourceCenterMapper.getEnvironmentListByAppModuleId(appModuleId, schemaName);
        JSONObject resultObj = new JSONObject();
        resultObj.put("tbodyList", enviromentList);
        return resultObj;
    }
}
