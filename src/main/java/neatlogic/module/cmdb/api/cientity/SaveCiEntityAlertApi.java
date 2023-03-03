/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.cientity;

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
        return "/cmdb/relentity/alert/save";
    }

    @Override
    public String getName() {
        return "保存配置项告警";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciEntityUuid", type = ApiParamType.STRING, isRequired = true, desc = "配置项唯一标识", help = "如果是32字符的散列，系统会直接保存，否则，系统会先做散列处理"),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, desc = "配置项id", help = "如果提供了，会优先使用，找不到配置项才会使用ciEntityUuid"),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "ip地址", isRequired = true, maxLength = 50),
            @Param(name = "level", type = ApiParamType.INTEGER, desc = "告警级别", isRequired = true, help = "必须是大于等于0的整数，数字越大级别越高，如果等于0代表消除告警"),
            @Param(name = "metricName", type = ApiParamType.STRING, desc = "告警属性", isRequired = true, xss = true, help = "每个配置项对应一个告警属性只能有一条告警信息"),
            @Param(name = "metricValue", type = ApiParamType.STRING, desc = "告警值"),
            @Param(name = "alertTime", type = ApiParamType.LONG, desc = "告警时间", help = "需要提供时间戳，精确到毫秒，如果不提供则使用保存时间"),
            @Param(name = "alertMessage", type = ApiParamType.STRING, desc = "告警详情", xss = true, maxLength = 500),
            @Param(name = "alertLink", type = ApiParamType.STRING, desc = "告警外部链接", xss = true, maxLength = 255),
    })
    @Description(desc = "保存配置项告警接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiEntityAlertVo ciEntityAlertVo = JSONObject.toJavaObject(jsonObj, CiEntityAlertVo.class);
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
