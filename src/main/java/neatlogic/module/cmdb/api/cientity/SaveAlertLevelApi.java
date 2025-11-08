/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

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
