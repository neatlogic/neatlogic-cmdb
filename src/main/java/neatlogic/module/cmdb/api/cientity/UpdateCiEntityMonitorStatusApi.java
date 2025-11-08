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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateCiEntityMonitorStatusApi extends PrivateApiComponentBase {


    @Resource
    private CiEntityMapper ciEntityMapper;


    @Override
    public String getToken() {
        return "cmdb/cientity/updatemonitorstatus";
    }

    @Override
    public String getName() {
        return "修改配置项监控状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
            @Param(name = "monitorStatus", type = ApiParamType.ENUM, rule = "normal,warn,critical,fatal", isRequired = true, desc = "监控状态"),
            @Param(name = "monitorTime", type = ApiParamType.LONG, isRequired = true, desc = "监控时间，格式：距1970年1月1日0时0分0秒的毫秒数")})
    @Output({})
    @Description(desc = "修改配置项监控状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setId(paramObj.getLong("ciEntityId"));
        ciEntityVo.setMonitorTime(new Date(paramObj.getLong("monitorTime")));
        ciEntityVo.setMonitorStatus(paramObj.getString("monitorStatus"));
        ciEntityMapper.updateCiEntityMonitorStatus(ciEntityVo);
        return null;
    }


}
