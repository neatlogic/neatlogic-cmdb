package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceScriptVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceAccessEndPointGetApi extends PublicApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getName() {
        return "根据资源id获取对应接入点信息";
    }

    @Override
    public String getToken() {
        return "resourcecenter/resource/accessendpoint/get";
    }

    @Override
    public String getConfig() {
        return null;
    }
    @Input({
            @Param(name = "resourceId", isRequired = true, type = ApiParamType.LONG, desc = "资源id")
    })
    @Output({
            @Param(explode = ResourceScriptVo.class)
    })
    @Description(desc = "根据资源id获取对应接入点信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject returnObject = new JSONObject();
        Long resourceId = paramObj.getLong("resourceId");
        String schemaName = TenantContext.get().getDataDbName();
        if (resourceCenterMapper.checkResourceIsExists(resourceId, schemaName) == 0) {
            throw new ResourceNotFoundException(resourceId);
        }
        ResourceScriptVo resourceScriptVo = resourceCenterMapper.getResourceScriptByResourceId(resourceId);
        returnObject.put("config", resourceScriptVo.getConfig());
        returnObject.put("resourceId", resourceScriptVo.getResourceId());
        returnObject.put("scriptId", resourceScriptVo.getScriptId());
        return returnObject;
    }
}
