/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.cmdb.formattribute.handler;

import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.enums.FormHandler;
import neatlogic.framework.common.constvalue.ParamType;
import neatlogic.framework.form.attribute.core.FormHandlerBase;
import neatlogic.framework.form.constvalue.FormConditionModel;
import neatlogic.framework.form.dto.AttributeDataVo;
import neatlogic.framework.form.exception.AttributeValidException;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

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
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return "已更新";
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
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

    //表单组件配置信息
//    {
//        "handler": "protocol",
//        "label": "连接协议_1",
//        "type": "form",
//        "uuid": "d9a91e90f2b94171883b8092bdddd384",
//        "config": {
//            "isRequired": false,
//            "ruleList": [],
//            "width": "100%",
//            "validList": [],
//            "quoteUuid": "",
//            "defaultValueType": "self",
//            "placeholder": "选择连接协议",
//            "authorityConfig": [
//                "common#alluser"
//            ]
//        }
//    }
//保存数据
//    478184378212353
//返回数据结构
//    {
//        "value": 478184378212353,
//        "name": "tagent",
//        "port": 3939
//    }
    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = new JSONObject();
        String protocolIdStr = (String) attributeDataVo.getDataObj();
        Long protocolId = Long.valueOf(protocolIdStr);
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
