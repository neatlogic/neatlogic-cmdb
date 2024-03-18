/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.api.resourcecenter.app;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AppSystemVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

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
            @Param(name = "uuid", type = ApiParamType.STRING, desc = "应用UUID"),
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
        List<Long> appModuleIdList = resourceMapper.getAppSystemModuleIdListByAppSystemId(resourceVo.getId());
        if (CollectionUtils.isNotEmpty(appModuleIdList)) {
            appSystemVo.setIsHasModule(1);
        }
        return appSystemVo;
    }
}
