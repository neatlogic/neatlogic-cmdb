/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.cmdb.api.rel;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.relativerel.RelativeRelManager;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class RebuildRelativeRelApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "/cmdb/rel/relative/rebuild";
    }

    @Override
    public String getName() {
        return "重建级联关系数据";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({@Param(name = "relId", type = ApiParamType.LONG, isRequired = true, desc = "关系id")})
    @Description(desc = "重建级联关系数据接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long relId = jsonObj.getLong("relId");
        RelativeRelManager.rebuild(relId);
        return null;
    }

}
