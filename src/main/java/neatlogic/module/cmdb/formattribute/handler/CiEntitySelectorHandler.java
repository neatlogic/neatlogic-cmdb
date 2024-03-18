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
