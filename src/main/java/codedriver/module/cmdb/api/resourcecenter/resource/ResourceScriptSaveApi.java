package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.RESOURCECENTER_MODIFY;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class ResourceScriptSaveApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getName() {
        return "保存资源脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "resourcecenter/resource/script/save";
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源id"),
            @Param(name = "scriptIdList", type = ApiParamType.JSONARRAY, desc = "脚本列表（自定义工具）"),
            @Param(name = "urlSequence", type = ApiParamType.STRING, desc = "url序列")
    })
    @Description(desc = "保存资源脚本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        JSONArray scriptIdArray = paramObj.getJSONArray("scriptIdList");
        Long resourceId = paramObj.getLong("resourceId");
        String urlSequence = paramObj.getString("urlSequence");
        resourceCenterMapper.deleteResourceScriptByResourceId(resourceId);
        if (CollectionUtils.isEmpty(scriptIdArray)) {
            return null;
        }
        List<Long> scriptIdList = scriptIdArray.toJavaList(Long.class);
        String schemaName = TenantContext.get().getDataDbName();
        if (resourceCenterMapper.checkResourceIsExists(resourceId, schemaName) == 0) {
            throw new ResourceNotFoundException(resourceId);
        }
        for (Long scriptId : scriptIdList) {
            AutoexecScriptVo script = autoexecScriptMapper.getScriptBaseInfoById(scriptId);
            if (script == null) {
                throw new AutoexecScriptNotFoundException(scriptId);
            }
        }
        resourceCenterMapper.insertResourceScriptIdList(resourceId, scriptIdList,urlSequence);

        return null;
    }


}
