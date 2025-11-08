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
import neatlogic.framework.cmdb.auth.label.CIENTITY_ALERT_MODIFY;
import neatlogic.framework.cmdb.dto.cientity.CiEntityAlertVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityAlertMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

@Service
@AuthAction(action = CIENTITY_ALERT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveCiEntityAlertApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private CiEntityAlertMapper ciEntityAlertMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/alert/save";
    }

    @Override
    public String getName() {
        return "nmcac.savecientityalertapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciEntityUuid", type = ApiParamType.STRING, isRequired = true, desc = "term.cmdb.cientityuniquename", help = "nmcac.savecientityalertapi.input.param.help.uniquename"),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, desc = "term.cmdb.cientityid", help = "nmcac.savecientityalertapi.input.param.help.cientityid"),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "ip", isRequired = true, maxLength = 50),
            @Param(name = "level", type = ApiParamType.INTEGER, desc = "common.alertlevel", isRequired = true, help = "nmcac.savecientityalertapi.input.param.help.alertlevel"),
            @Param(name = "metricName", type = ApiParamType.STRING, desc = "common.alertattr", isRequired = true, xss = true, help = "nmcac.savecientityalertapi.input.param.help.alertattr"),
            @Param(name = "metricValue", type = ApiParamType.STRING, desc = "common.alertvalue"),
            @Param(name = "alertTime", type = ApiParamType.LONG, desc = "common.alerttime", help = "nmcac.savecientityalertapi.input.param.help.alerttime"),
            @Param(name = "alertMessage", type = ApiParamType.STRING, desc = "common.alertdetail", xss = true, maxLength = 500),
            @Param(name = "alertLink", type = ApiParamType.STRING, desc = "common.alertlink", xss = true, maxLength = 255),
    })
    @Description(desc = "nmcac.savecientityalertapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiEntityAlertVo ciEntityAlertVo = JSON.toJavaObject(jsonObj, CiEntityAlertVo.class);
        String ciEntityUuid = jsonObj.getString("ciEntityUuid");
        if (!Md5Util.isMd5(ciEntityUuid)) {
            ciEntityUuid = Md5Util.encryptMD5(ciEntityUuid);
        }
        Long ciEntityId = jsonObj.getLong("ciEntityId");
        CiEntityVo ciEntityVo = null;
        if (ciEntityId != null) {
            ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(ciEntityId);
            if (ciEntityVo == null) {
                ciEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(ciEntityUuid);
            }
        } else {
            ciEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(ciEntityUuid);
        }
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(ciEntityUuid);
        }
        ciEntityAlertVo.setCiEntityId(ciEntityVo.getId());
        CiEntityAlertVo checkCiEntityAlertVo = ciEntityAlertMapper.getCiEntityAlert(ciEntityAlertVo);
        if (ciEntityAlertVo.getAlertTime() == null) {
            ciEntityAlertVo.setAlertTime(new Date(System.currentTimeMillis()));
        }
        if (checkCiEntityAlertVo == null) {
            if (ciEntityAlertVo.getLevel() > 0) {
                ciEntityAlertMapper.insertCiEntityAlert(ciEntityAlertVo);
            }
        } else {
            if (ciEntityAlertVo.getLevel() > 0) {
                ciEntityAlertVo.setId(checkCiEntityAlertVo.getId());
                ciEntityAlertMapper.updateCiEntityAlert(ciEntityAlertVo);
            } else {
                ciEntityAlertMapper.deleteCiEntityAlertById(checkCiEntityAlertVo.getId());
            }
        }
        return null;
    }

}
