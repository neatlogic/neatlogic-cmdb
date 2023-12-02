/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.FormHandler;
import neatlogic.framework.common.constvalue.ParamType;
import neatlogic.framework.form.attribute.core.FormHandlerBase;
import neatlogic.framework.form.constvalue.FormConditionModel;
import neatlogic.framework.form.dto.AttributeDataVo;
import neatlogic.framework.form.exception.AttributeValidException;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author linbq
 * @since 2021/8/18 14:24
 **/
//@Component
public class CiEntitySelectorHandler extends FormHandlerBase {

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Override
    public String getHandler() {
        return FormHandler.FORMCIENTITYSELECTOR.getHandler();
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
    public boolean isProcessTaskBatchSubmissionTemplateParam() {
        return false;
    }

    @Override
    public JSONObject valid(AttributeDataVo attributeDataVo, JSONObject configObj) throws AttributeValidException {
        return null;
    }

    @Override
    public Object conversionDataType(Object source, String attributeLabel) {
        return convertToJSONArray(source, attributeLabel);
    }

    @Override
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = getMyDetailedData(attributeDataVo, configObj);
        JSONArray tbodyArray = resultObj.getJSONArray("tbodyList");
        if (CollectionUtils.isEmpty(tbodyArray)) {
            return new ArrayList<>();
        }
        List<CiEntityVo> tbodyList = tbodyArray.toJavaList(CiEntityVo.class);
        return tbodyList.stream().map(CiEntityVo::getName).collect(Collectors.toList());
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = getMyDetailedData(attributeDataVo, configObj);
        JSONArray tbodyArray = resultObj.getJSONArray("tbodyList");
        if (CollectionUtils.isEmpty(tbodyArray)) {
            return null;
        }
        List<CiEntityVo> tbodyList = tbodyArray.toJavaList(CiEntityVo.class);
        List<String> nameList = tbodyList.stream().map(CiEntityVo::getName).collect(Collectors.toList());
        return String.join("、", nameList);
    }

    @Override
    public Object textConversionValue(Object text, JSONObject config) {
        return null;
    }

    /*
    {
        "handler": "formcientityselector",
        "reaction": {
            "hide": {},
            "readonly": {},
            "disable": {},
            "display": {},
            "mask": {}
        },
        "override_config": {},
        "icon": "tsfont-tree",
        "hasValue": true,
        "label": "配置项选择_1",
        "type": "form",
        "category": "cmdb",
        "config": {
            "disableDefaultValue": true,
            "isMask": false,
            "width": "100%",
            "description": "",
            "placeholder": "选择配置项",
            "isHide": false,
            "ciList": []
        },
        "uuid": "d05ce7dde7004dfe91640d3d824719ea"
    }
     */
    /*
    [
        686818514345989,
        686818514345984
    ]
     */
    /*
    {
        "value": [
            686818514345989,
            686818514345984
        ],
        "tbodyList": [
            {
                "attrEntityData": {},
                "ciIcon": "tsfont-ci",
                "ciId": 686815704162324,
                "fcd": 1659683152668,
                "fcu": "fccf704231734072a1bf80d90b2d1de2",
                "id": 686818514345984,
                "isLocked": 0,
                "isVirtual": 0,
                "lcd": 1662609881807,
                "lcu": "fccf704231734072a1bf80d90b2d1de2",
                "maxAttrEntityCount": 3,
                "maxRelEntityCount": 3,
                "name": "",
                "relEntityData": {},
                "startPage": 1,
                "typeId": 479603328032768
            },
            {
                "attrEntityData": {},
                "ciIcon": "tsfont-ci",
                "ciId": 686815704162324,
                "fcd": 1659683152790,
                "fcu": "fccf704231734072a1bf80d90b2d1de2",
                "id": 686818514345989,
                "isLocked": 0,
                "isVirtual": 0,
                "lcd": 1660546695775,
                "lcu": "fccf704231734072a1bf80d90b2d1de2",
                "maxAttrEntityCount": 3,
                "maxRelEntityCount": 3,
                "name": "test2",
                "relEntityData": {},
                "startPage": 1,
                "typeId": 479603328032768
            }
        ]
    }
     */
    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = new JSONObject();
        JSONArray dataArray = (JSONArray) attributeDataVo.getDataObj();
        resultObj.put("value", dataArray);
        if (CollectionUtils.isEmpty(dataArray)) {
            return resultObj;
        }
        List<Long> idList = dataArray.toJavaList(Long.class);
        List<CiEntityVo> tbodyList = ciEntityMapper.getCiEntityBaseInfoByIdList(idList);
        resultObj.put("tbodyList", tbodyList);
        return resultObj;
    }

    @Override
    public Object dataTransformationForExcel(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = getMyDetailedData(attributeDataVo, configObj);
        JSONArray tbodyArray = resultObj.getJSONArray("tbodyList");
        if (CollectionUtils.isEmpty(tbodyArray)) {
            return null;
        }
        List<CiEntityVo> tbodyList = tbodyArray.toJavaList(CiEntityVo.class);
        List<String> nameList = tbodyList.stream().map(CiEntityVo::getName).collect(Collectors.toList());
        return String.join(",", nameList);
    }
}
