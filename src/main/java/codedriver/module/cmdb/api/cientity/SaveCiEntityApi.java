package codedriver.module.cmdb.api.cientity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.service.ci.CiAuthService;
import codedriver.module.cmdb.service.cientity.CiEntityService;

@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCiEntityApi extends PrivateApiComponentBase {

    @Autowired
    private CiEntityService ciEntityService;

    @Autowired
    private CiAuthService ciAuthService;

    @Override
    public String getToken() {
        return "/cmdb/cientity/save";
    }

    @Override
    public String getName() {
        return "保存配置项";
    }

    @Override
    public String getConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，不提供代表新增模型"),
        @Param(name = "name", type = ApiParamType.STRING, xss = true, isRequired = true, maxLength = 50, desc = "英文名称"),
        @Param(name = "label", type = ApiParamType.STRING, desc = "中文名称", xss = true, maxLength = 100,
            isRequired = true),
        @Param(name = "description", type = ApiParamType.STRING, desc = "备注", maxLength = 500, xss = true),
        @Param(name = "icon", type = ApiParamType.STRING, desc = "图标"),
        @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id", isRequired = true),
        @Param(name = "isMenu", type = ApiParamType.INTEGER, desc = "是否在菜单显示")})
    @Output({@Param(name = "id", type = ApiParamType.STRING, desc = "模型id")})
    @Description(desc = "保存配置项接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return null;
    }

}
