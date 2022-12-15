/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.formattribute.handler;

import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.cmdb.enums.FormHandler;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.form.attribute.core.FormHandlerBase;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.form.dto.AttributeDataVo;
import codedriver.framework.form.exception.AttributeValidException;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
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
        return ParamType.ARRAY;
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
    public Object textConversionValue(List<String> values, JSONObject config) {
        return null;
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
