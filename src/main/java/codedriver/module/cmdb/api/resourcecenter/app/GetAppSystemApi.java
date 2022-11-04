/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.app;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.auth.label.CMDB;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.AppSystemVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAppSystemApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityMapper ciEntityMapper;
    @Resource
    private ResourceMapper resourceMapper;

    @Override
    public String getToken() {
        return "resourcecenter/appsystem/get";
    }

    @Override
    public String getName() {
        return "查询单个应用信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "应用ID"),
            @Param(name = "uuid", type = ApiParamType.LONG, desc = "应用UUID"),
    })
    @Output({
           @Param(explode = AppSystemVo.class, desc = "应用信息")
    })
    @Description(desc = "查询单个应用信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        if (id == null) {
            String uuid = paramObj.getString("uuid");
            if (StringUtils.isBlank(uuid)) {
                throw new ParamNotExistsException("应用ID（id）", "应用UUID（uuid）");
            }
            CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(uuid);
            if (ciEntityVo == null) {
                return null;
            }
            id = ciEntityVo.getId();
        }
        ResourceVo resourceVo = resourceMapper.getAppSystemById(id);
        if (resourceVo == null) {
            return null;
        }
        AppSystemVo appSystemVo = new AppSystemVo();
        appSystemVo.setId(resourceVo.getId());
        appSystemVo.setName(resourceVo.getName());
        appSystemVo.setAbbrName(resourceVo.getAbbrName());
        return appSystemVo;
    }
}
