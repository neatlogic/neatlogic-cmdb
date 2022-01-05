/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.formattribute.handler;

import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.form.attribute.core.FormHandlerBase;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.form.dto.AttributeDataVo;
import codedriver.framework.form.exception.AttributeValidException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/12/27 11:14
 **/
@Component
public class ResourcesHandler extends FormHandlerBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getHandler() {
        return "formresoureces";
    }

    @Override
    public String getHandlerName() {
        return "执行目标";
    }

    @Override
    public String getHandlerType(FormConditionModel model) {
        return null;
    }

    @Override
    public String getIcon() {
        return "tsfont-blocks";
    }

    @Override
    public ParamType getParamType() {
        return ParamType.ARRAY;
    }

    @Override
    public String getDataType() {
        return "list";
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
        return false;
    }

    @Override
    public boolean isForTemplate() {
        return true;
    }

    @Override
    public String getModule() {
        return "cmdb";
    }

    @Override
    public JSONObject valid(AttributeDataVo attributeDataVo, JSONObject jsonObj) throws AttributeValidException {
        JSONObject resultObj = new JSONObject();
        resultObj.put("result", true);
        JSONObject dataObj = (JSONObject) attributeDataVo.getDataObj();
        String type = dataObj.getString("type");
        if ("input".equals(type)) {
            JSONArray inputNodeArray = dataObj.getJSONArray("inputNodeList");
            if (CollectionUtils.isNotEmpty(inputNodeArray)) {
                List<ResourceSearchVo> resourceIsNotFoundList = new ArrayList<>();
                List<ResourceSearchVo> inputNodeList = inputNodeArray.toJavaList(ResourceSearchVo.class);
                for (ResourceSearchVo node : inputNodeList) {
                    Long resourceId = resourceCenterMapper.getResourceIdByIpAndPortAndName(node);
                    if (resourceId == null) {
                        resourceIsNotFoundList.add(node);
                    }
                }
                if (CollectionUtils.isNotEmpty(resourceIsNotFoundList)) {
                    resultObj.put("result", false);
                    resultObj.put("list", resourceIsNotFoundList);
                }
            }
        }
        return resultObj;
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

    @Override
    public int getSort() {
        return 21;
    }

    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return null;
    }
}
