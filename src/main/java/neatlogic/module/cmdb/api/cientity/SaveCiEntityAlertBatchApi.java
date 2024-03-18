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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CIENTITY_ALERT_MODIFY;
import neatlogic.framework.cmdb.dto.cientity.CiEntityAlertVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityAlertMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

@Service
@AuthAction(action = CIENTITY_ALERT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveCiEntityAlertBatchApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private CiEntityAlertMapper ciEntityAlertMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/alert/batchsave";
    }

    @Override
    public String getName() {
        return "nmcac.savecientityalertbatchapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public JSONObject example() {
        JSONObject exampleObj = new JSONObject();
        exampleObj.put("alertList", new JSONArray() {{
            this.add(new JSONObject() {{
                this.put("ciEntityUuid", "必填，配置项唯一标识，如果是32字符的散列，系统会直接保存，否则，系统会先做散列处理");
                this.put("ciEntityId", "选填，配置项id");
                this.put("ip", "必填，ip地址");
                this.put("level", "必填，告警级别，必须是大于等于0的整数，数字越大级别越高，如果等于0代表消除告警");
                this.put("metricName", "必填，告警属性，每个配置项对应一个告警属性只能有一条告警信息");
                this.put("metricValue", "选填，告警值");
                this.put("alertMessage", "选填，告警详情");
                this.put("alertLink", "选填，告警外部链接");
            }});
        }});
        return exampleObj;
    }

    @Input({@Param(name = "alertList", isRequired = true, type = ApiParamType.JSONARRAY, desc = "common.alertlist", help = "nmcac.savecientityalertbatchapi.input.param.help"),
    })
    @Description(desc = "nmcac.savecientityalertbatchapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray alertList = jsonObj.getJSONArray("alertList");
        for (int i = 0; i < alertList.size(); i++) {
            JSONObject alertObj = alertList.getJSONObject(i);
            CiEntityAlertVo ciEntityAlertVo = JSONObject.toJavaObject(alertObj, CiEntityAlertVo.class);
            String ciEntityUuid = alertObj.getString("ciEntityUuid");
            if (StringUtils.isBlank(ciEntityUuid)) {
                throw new ParamNotExistsException("ciEntityUuid");
            }
            if (StringUtils.isBlank(ciEntityAlertVo.getIp())) {
                throw new ParamNotExistsException("ip");
            }
            if (StringUtils.isBlank(ciEntityAlertVo.getMetricName())) {
                throw new ParamNotExistsException("metricName");
            }
            if (!Md5Util.isMd5(ciEntityUuid)) {
                ciEntityUuid = Md5Util.encryptMD5(ciEntityUuid);
            }
            Long ciEntityId = alertObj.getLong("ciEntityId");
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
        }

        return null;
    }

}
