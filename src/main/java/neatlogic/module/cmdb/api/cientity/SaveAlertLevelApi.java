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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.ALERTLEVEL_MODIFY;
import neatlogic.framework.cmdb.dto.cientity.AlertLevelVo;
import neatlogic.framework.cmdb.exception.alertlevel.AlertLevelIsExistsException;
import neatlogic.framework.cmdb.exception.alertlevel.AlertLevelNameTypeIsExistsException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.cientity.AlertLevelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERTLEVEL_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveAlertLevelApi extends PrivateApiComponentBase {

    @Resource
    private AlertLevelMapper alertLevelMapper;

    @Override
    public String getToken() {
        return "/cmdb/alertlevel/save";
    }

    @Override
    public String getName() {
        return "nmcac.savealertlevelapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "level", desc = "common.level", type = ApiParamType.INTEGER, isRequired = true),
            @Param(name = "name", desc = "common.uniquename", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "label", desc = "common.name", type = ApiParamType.STRING),
            @Param(name = "type", desc = "common.type", type = ApiParamType.STRING, rule = "inspect,monitor", isRequired = true),
            @Param(name = "color", desc = "common.color", type = ApiParamType.STRING)})
    @Output({@Param(explode = AlertLevelVo[].class)})
    @Description(desc = "nmcac.savealertlevelapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AlertLevelVo alertLevelVo = JSON.toJavaObject(jsonObj, AlertLevelVo.class);
        if (alertLevelMapper.checkAlertLevelIsExists(alertLevelVo) > 0) {
            throw new AlertLevelIsExistsException(alertLevelVo.getLevel());
        }
        if (alertLevelMapper.checkAlertNameTypeIsExists(alertLevelVo) > 0) {
            throw new AlertLevelNameTypeIsExistsException(alertLevelVo.getTypeText(), alertLevelVo.getName());
        }
        alertLevelMapper.saveAlertLevel(alertLevelVo);
        return null;
    }

}
