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


package neatlogic.module.cmdb.publicapi;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.AlertLevelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityAlertVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.alertlevel.AlertLevelType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.cientity.AlertLevelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityAlertMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateCiEntityInspectStatusApi extends PrivateApiComponentBase {


    @Resource
    private CiEntityMapper ciEntityMapper;


    @Resource
    private CiEntityAlertMapper ciEntityAlertMapper;

    @Resource
    private AlertLevelMapper alertLevelMapper;

    @Override
    public String getToken() {
        return "cmdb/cientity/updateinspectstatus";
    }

    @Override
    public String getName() {
        return "修改配置项巡检状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业id"),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
            @Param(name = "inspectStatus", type = ApiParamType.ENUM, rule = "normal,warn,critical,fatal", isRequired = true, desc = "巡检状态"),
            @Param(name = "inspectTime", type = ApiParamType.LONG, isRequired = true, desc = "巡检时间，格式：距1970年1月1日0时0分0秒的毫秒数")})
    @Output({})
    @Description(desc = "修改配置项巡检状态接口，自动化巡检时使用此接口更新巡检状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String status = paramObj.getString("inspectStatus");
        Long ciEntityId = paramObj.getLong("ciEntityId");
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setId(paramObj.getLong("ciEntityId"));
        ciEntityVo.setInspectTime(new Date(paramObj.getLong("inspectTime")));
        ciEntityVo.setInspectStatus(paramObj.getString("inspectStatus"));

        ciEntityMapper.updateCiEntityInspectStatus(ciEntityVo);
        //CiEntityInspectVo ciEntityInspectVo = new CiEntityInspectVo(paramObj);
        //ciEntityMapper.insertCiEntityInspect(ciEntityInspectVo);

        List<AlertLevelVo> levelList = alertLevelMapper.getAlertLevelByType(AlertLevelType.INSPECT.getValue());
        if (CollectionUtils.isNotEmpty(levelList)) {
            //删除所有巡检类告警
            ciEntityAlertMapper.deleteCiEntityAlertByCiEntityIdAndLevelList(ciEntityId, levelList.stream().map(AlertLevelVo::getLevel).collect(Collectors.toList()));

            if (!Objects.equals("normal", status)) {
                //写入新告警
                AlertLevelVo level = alertLevelMapper.getAlertLevelByNameAndType(status, AlertLevelType.INSPECT.getValue());
                if (level != null) {
                    CiEntityAlertVo ciEntityAlertVo = new CiEntityAlertVo();
                    ciEntityAlertVo.setLevel(level.getLevel());
                    ciEntityAlertVo.setCiEntityId(ciEntityId);
                    ciEntityAlertVo.setAlertTime(new Date(paramObj.getLong("inspectTime")));
                    ciEntityAlertMapper.insertCiEntityAlert(ciEntityAlertVo);
                }
            }
        }
        return null;
    }


}
