/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.resourcecenter.resource;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 根据应用名称、模块名称环境名称查询应用ID、模块ID、环境ID
 *
 * @author laiwt
 * @since 2022/7/11 16:14
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAppSystemIdAndModuleIdAndEnvIdApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "resourcecenter/resource/appidmoduleidenvid/get";
    }

    @Override
    public String getName() {
        return "根据应用名称、模块名称环境名称查询应用ID、模块ID、环境ID";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    @Input({
            @Param(name = "sysName", type = ApiParamType.STRING, isRequired = true, desc = "应用名称"),
            @Param(name = "moduleName", type = ApiParamType.STRING, isRequired = true, desc = "模块名称"),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "环境名称"),
    })
    @Output({
    })
    @Description(desc = "根据应用名称、模块名称环境名称查询应用ID、模块ID、环境ID")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        String sysName = jsonObj.getString("sysName");
        String moduleName = jsonObj.getString("moduleName");
        String envName = jsonObj.getString("envName");
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemByName(sysName);
        if (appSystem == null) {
            throw new AppSystemNotFoundException(sysName);
        }
        ResourceVo appModule = resourceCrossoverMapper.getAppModuleByName(moduleName);
        if (appModule == null) {
            throw new AppModuleNotFoundException(moduleName);
        }
        result.put("sysId", appSystem.getId());
        result.put("moduleId", appModule.getId());
        if (StringUtils.isNotBlank(envName)) {
            ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
            Long envId = ciEntityCrossoverMapper.getCiEntityIdByCiNameAndCiEntityName("APPEnv", envName);
            if (envId == null) {
                throw new AppEnvNotFoundException(envName);
            }
            result.put("envId", envId);
        }
        return result;
    }


}
