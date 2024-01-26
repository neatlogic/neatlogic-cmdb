package neatlogic.module.cmdb.api.resourcecenter.app;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAppSystemSimpleApi extends PrivateApiComponentBase {

    @Resource
    CiEntityMapper ciEntityMapper;

    @Resource
    CiEntityService ciEntityService;

    @Override
    public String getName() {
        return "查询应用系统详细配置信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "app/system/simple/get";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id")
    })
    @Output({
            @Param(explode = CiEntityVo.class),
    })
    @Description(desc = "查询应用系统详细配置信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验应用系统id是否存在
        if (ciEntityMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appSystemId"));
        }

        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId"));
        CiEntityVo appSystemInfo = ciEntityService.getCiEntityById(ciEntityVo.getCiId(), paramObj.getLong("appSystemId"));

        JSONObject appSystemInfoObject = new JSONObject();
        appSystemInfoObject.put("appSystemInfo", appSystemInfo);
        return appSystemInfoObject;
    }
}
