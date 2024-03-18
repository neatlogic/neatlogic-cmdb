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

package neatlogic.module.cmdb.formattribute.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.enums.FormHandler;
import neatlogic.framework.common.constvalue.ParamType;
import neatlogic.framework.form.attribute.core.FormHandlerBase;
import neatlogic.framework.form.constvalue.FormConditionModel;
import neatlogic.framework.form.dto.AttributeDataVo;
import neatlogic.framework.form.exception.AttributeValidException;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author linbq
 * @since 2021/12/27 11:18
 **/
@Component
public class ProtocolNewHandler extends FormHandlerBase {

    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getHandler() {
        return FormHandler.FORMPROTOCOL.getHandler();
    }

    @Override
    public String getHandlerType(FormConditionModel model) {
        return null;
    }

    @Override
    public ParamType getParamType() {
        return ParamType.NUMBER;
    }

    @Override
    public boolean isAudit() {
        return true;
    }

    @Override
    public boolean isConditionable() {
        return false;
    }

    @Override
    public boolean isProcessTaskBatchSubmissionTemplateParam() {
        return true;
    }

    @Override
    public JSONObject valid(AttributeDataVo attributeDataVo, JSONObject configObj) throws AttributeValidException {
        return null;
    }

    @Override
    public Object conversionDataType(Object source, String attributeLabel) {
        if (source == null) {
            return null;
        }
        if (source instanceof String) {
            try {
                return Long.valueOf((String) source);
            } catch (NumberFormatException e) {
            }
        } else if (source instanceof Number) {
            return source;
        }
        throw new AttributeValidException(attributeLabel);
    }

    @Override
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject dataObj = getMyDetailedData(attributeDataVo, configObj);
        if (MapUtils.isNotEmpty(dataObj)) {
            return dataObj.getString("name");
        }
        return null;
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject dataObj = getMyDetailedData(attributeDataVo, configObj);
        if (MapUtils.isNotEmpty(dataObj)) {
            return dataObj.getString("name");
        }
        return null;
    }

    @Override
    public Object textConversionValue(Object text, JSONObject config) {
        if (text == null) {
            return null;
        }
        AccountProtocolVo accountProtocolVo = resourceAccountMapper.getAccountProtocolVoByProtocolName((String) text);
        if (accountProtocolVo == null) {
            return null;
        }
        return accountProtocolVo.getId();
    }

    /*
    表单组件配置信息
    {
        "handler": "formprotocol",
        "reaction": {
            "hide": {},
            "readonly": {},
            "display": {}
        },
        "override_config": {},
        "icon": "tsfont-zirenwu",
        "hasValue": true,
        "label": "连接协议_15",
        "type": "form",
        "category": "autoexec",
        "config": {
            "isRequired": false,
            "disableDefaultValue": true,
            "isMask": false,
            "width": "100%",
            "description": "",
            "isHide": false
        },
        "uuid": "7f960f1045e24c128e7673913d3f1572"
    }
     */
    /*
    保存数据结构
    478219912355840
     */
    /*
    返回数据结构
    {
        "value": 478219912355840,
        "name": "tagent",
        "port": 3939
    }
     */
    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = new JSONObject();
        Long protocolId = (Long) attributeDataVo.getDataObj();
        if (protocolId == null) {
            return resultObj;
        }
        resultObj.put("value", protocolId);
        AccountProtocolVo protocolVo = resourceAccountMapper.getAccountProtocolVoByProtocolId(protocolId);
        if (protocolVo != null) {
            resultObj.put("name", protocolVo.getName());
            resultObj.put("port", protocolVo.getPort());
        }
        return resultObj;
    }

    @Override
    public Object dataTransformationForExcel(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject detailedData = getMyDetailedData(attributeDataVo, configObj);
        if (detailedData != null) {
            Long value = detailedData.getLong("value");
            String name = detailedData.getString("name");
            String port = detailedData.getString("port");
            return name + port + "(" + value + ")";
        }
        return null;
    }
}
