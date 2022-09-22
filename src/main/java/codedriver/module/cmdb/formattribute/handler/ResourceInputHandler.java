/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.formattribute.handler;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.form.attribute.core.FormHandlerBase;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.form.dto.AttributeDataVo;
import codedriver.framework.form.exception.AttributeValidException;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/9/13 9:56
 **/
@Deprecated
//@Component 用“执行目标”组件替代“资产输入”组件
public class ResourceInputHandler extends FormHandlerBase {

    @Resource
    private ResourceMapper resourceMapper;

    @Override
    public String getHandler() {
        return "resourceinput";
    }

    @Override
    public String getHandlerName() {
        return "资产输入组件";
    }

    @Override
    public String getHandlerType(FormConditionModel model) {
        return "input";
    }

    @Override
    public String getIcon() {
        return "tsfont-zichanshuruzujian";
    }

    @Override
    public ParamType getParamType() {
        return ParamType.STRING;
    }

    @Override
    public String getDataType() {
        return "string";
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
        return true;
    }

    @Override
    public boolean isFilterable() {
        return false;
    }

    @Override
    public boolean isExtendable() {
        return false;
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
    public JSONObject valid(AttributeDataVo attributeDataVo, JSONObject jsonObj) throws AttributeValidException {
        JSONObject resultObj = new JSONObject();
        resultObj.put("result", true);
//        JSONObject configObj = jsonObj.getJSONObject("attributeConfig");
        JSONArray dataObj = (JSONArray) attributeDataVo.getDataObj();
        if (CollectionUtils.isNotEmpty(dataObj)) {
            List<ResourceSearchVo> resourceIsNotFoundList = new ArrayList<>();
            List<ResourceSearchVo> inputNodeList = dataObj.toJavaList(ResourceSearchVo.class);
            for (ResourceSearchVo searchVo : inputNodeList) {
                Long resourceId = resourceMapper.getResourceIdByIpAndPortAndName(searchVo);
                if (resourceId == null) {
                    resourceIsNotFoundList.add(searchVo);
                }
            }
            if (CollectionUtils.isNotEmpty(resourceIsNotFoundList)) {
                resultObj.put("result", false);
                JSONObject resourceIsNotFoundObj = new JSONObject();
                resourceIsNotFoundObj.put("type", "resourceIsNotFound");
                resourceIsNotFoundObj.put("list", resourceIsNotFoundList);
                JSONArray resultArray = new JSONArray();
                resultArray.add(resourceIsNotFoundObj);
                resultObj.put("list", resultArray);
            }
        }
        return resultObj;
    }

    @Override
    public int getSort() {
        return -1;
    }

    @Override
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return attributeDataVo.getDataObj();
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return attributeDataVo.getDataObj();
    }

    @Override
    public Object textConversionValue(List<String> values, JSONObject config) {
        return null;
    }

    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return null;
    }

    @Override
    public Object dataTransformationForExcel(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return null;
    }
}
