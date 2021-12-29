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

    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return null;
    }
}
