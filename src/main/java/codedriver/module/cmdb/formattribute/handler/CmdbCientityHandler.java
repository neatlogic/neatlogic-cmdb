/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.formattribute.handler;

import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.form.attribute.core.FormHandlerBase;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.form.dto.AttributeDataVo;
import codedriver.framework.form.exception.AttributeValidException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/8/18 14:24
 **/
@Component
public class CmdbCientityHandler extends FormHandlerBase {
    @Override
    public String getHandler() {
        return "formcmdbcientity";
    }

    @Override
    public String getHandlerName() {
        return "配置项组件";
    }

    @Override
    public String getHandlerType(FormConditionModel model) {
        return null;
    }

    @Override
    public String getIcon() {
        return "tsfont-tree";
    }

    @Override
    public ParamType getParamType() {
        return ParamType.ARRAY;
    }

    @Override
    public String getDataType() {
        return null;
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
    public boolean isShowable() {
        return true;
    }

    @Override
    public boolean isValueable() {
        return false;
    }

    @Override
    public boolean isFilterable() {
        return false;
    }

    @Override
    public boolean isExtendable() {
        return true;
    }

    @Override
    public boolean isForTemplate() {
        return false;
    }

    @Override
    public String getModule() {
        return "cmdb";
    }

    @Override
    public int getSort() {
        return 18;
    }

    @Override
    public JSONObject valid(AttributeDataVo attributeDataVo, JSONObject configObj) throws AttributeValidException {
        return null;
    }

    @Override
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        List<String> resultList = new ArrayList<>();
        JSONObject dataObj = (JSONObject) attributeDataVo.getDataObj();
        if (MapUtils.isNotEmpty(dataObj)) {
            JSONArray selectedCiEntityList = dataObj.getJSONArray("selectedCiEntityList");
            for (int i = 0; i < selectedCiEntityList.size(); i++) {
                JSONObject selectedCiEntityObj = selectedCiEntityList.getJSONObject(i);
                if (MapUtils.isNotEmpty(selectedCiEntityObj)) {
                    String name = selectedCiEntityObj.getString("name");
                    if (StringUtils.isNotBlank(name)) {
                        resultList.add(name);
                    }
                }
            }
        }
        return resultList;
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return valueConversionText(attributeDataVo, configObj);
    }

    @Override
    public Object textConversionValue(List<String> values, JSONObject config) {
        return null;
    }
//表单组件配置信息
//    {
//        "handler": "formcmdbcientity",
//        "label": "配置项组件_1",
//        "type": "form",
//        "uuid": "468c20afaaed452c89599a44b90ed077",
//        "config": {
//            "isRequired": false,
//            "ruleList": [],
//            "width": "100%",
//            "validList": [],
//            "quoteUuid": "",
//            "defaultValueType": "self",
//            "placeholder": "请选择配置项组件",
//            "authorityConfig": [
//                "common#alluser"
//            ]
//        }
//    }
//保存数据结构
//    {
//        "selectedCiEntityList": [
//            {
//                "typeName": "应用系统",
//                "type": 441079325270016,
//                "inspectStatus": "",
//                "uuid": "770ec8aea3dd4a53925a514b93b2d309",
//                "ciName": "APP",
//                "ciId": 479609502048256,
//                "renewTime": "2022-01-25 14:32",
//                "_selected": true,
//                "maxRelEntityCount": 3,
//                "relEntityData": {},
//                "name": "名称s4",
//                "attrEntityData": {
//                    "attr_478701787553792": {
//                        "ciEntityId": 547082414645248,
//                        "attrId": 478701787553792,
//                        "actualValueList": [
//                            "名称s4"
//					    ],
//                        "valueList": [
//                            "名称s4"
//					    ],
//                        "name": "name",
//                        "label": "名称",
//                        "type": "text",
//                        "ciId": 441087512551424
//                    },
//                    "attr_478702072766464": {
//                        "ciEntityId": 547082414645248,
//                        "attrId": 478702072766464,
//                        "actualValueList": [
//                            "描述s"
//                        ],
//                        "valueList": [
//                            "描述s"
//                        ],
//                        "name": "description",
//                        "label": "备注",
//                        "type": "text",
//                        "ciId": 441087512551424
//                    },
//                    "attr_480816840630272": {
//                        "ciEntityId": 547082414645248,
//                        "attrId": 480816840630272,
//                        "actualValueList": [
//                            "2021-01-02"
//                        ],
//                        "valueList": [
//                            "2021-01-02"
//                        ],
//                        "name": "maintenance_window",
//                        "label": "维护窗口",
//                        "type": "date",
//                        "config": {
//                            "format": "HH:mm",
//                            "type": "timerange"
//                        },
//                        "ciId": 441087512551424
//                    },
//                    "attr_478703406555136": {
//                        "ciEntityId": 547082414645248,
//                        "attrId": 478703406555136,
//                        "actualValueList": [
//                            "闫雅(0163347)"
//                        ],
//                        "valueList": [
//                            431662986961481
//                        ],
//                        "name": "owner",
//                        "label": "负责人",
//                        "targetCiId": 479643459133440,
//                        "type": "select",
//                        "config": {
//                            "mode": "r",
//                            "isMultiple": 1
//                        },
//                        "ciId": 441087512551424
//                    }
//                },
//                "id": 547082414645248,
//                "maxAttrEntityCount": 3,
//                "ciLabel": "应用系统",
//                "monitorStatus": ""
//            }
//        ],
//        "ciId": 441087512551424
//    }
//返回数据结构
//{
//    "value": 原始数据
//}
    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = new JSONObject();
        resultObj.put("value", attributeDataVo.getDataObj());
        return resultObj;
    }
}
