package codedriver.module.cmdb.formattribute.handler;

import codedriver.framework.cmdb.crossover.ISearchCiEntityApiCrossoverService;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.form.dto.AttributeDataVo;
import codedriver.framework.form.exception.AttributeValidException;
import codedriver.framework.form.attribute.core.FormHandlerBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CiEntitySyncHandler extends FormHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(CiEntitySyncHandler.class);
    @Override
    public String getHandler() {
        return "cientityselect";
    }

    @Override
    public JSONObject valid(AttributeDataVo attributeDataVo, JSONObject configObj) throws AttributeValidException {
        return null;
    }

    @Override
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        if (!attributeDataVo.dataIsEmpty()) {
            return "已更新";
        } else {
            return "";
        }
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONArray tableArray = new JSONArray();
        JSONArray dataArray = (JSONArray) attributeDataVo.getDataObj();
        if (CollectionUtils.isNotEmpty(dataArray)) {
            JSONObject paramObj = new JSONObject();
            paramObj.put("currentPage", 1);
            paramObj.put("needPage", false);
            paramObj.put("mode", "dialog");
            paramObj.put("needActionType", true);
            List<Long> ciIdList = new ArrayList<>();
            Map<Long, String> ciLabelMap = new HashMap<>();
            Map<Long, List<CiEntityVo>> ciEntityListMap = new HashMap<>();
            List<CiEntityVo> ciEntityVoList = dataArray.toJavaList(CiEntityVo.class);
            for (CiEntityVo ciEntityVo : ciEntityVoList) {
                Long ciId = ciEntityVo.getCiId();
                if (!ciIdList.contains(ciId)) {
                    ciIdList.add(ciId);
                    ciLabelMap.put(ciId, ciEntityVo.getCiLabel());
                }
                ciEntityListMap.computeIfAbsent(ciId, key -> new ArrayList<>()).add(ciEntityVo);
            }
            for (Long ciId : ciIdList) {
                paramObj.put("ciId", ciId);
                paramObj.put("ciEntityList", ciEntityListMap.get(ciId));
                try {
                    ISearchCiEntityApiCrossoverService searchCiEntityApiCrossoverService = CrossoverServiceFactory.getApi(ISearchCiEntityApiCrossoverService.class);
                    JSONObject tableObj = (JSONObject) searchCiEntityApiCrossoverService.myDoService(paramObj);
                    JSONArray tbodyList = new JSONArray();
                    JSONArray tbodyArray = tableObj.getJSONArray("tbodyList");
                    for (int i = 0; i < tbodyArray.size(); i++) {
                        JSONObject tbodyObj = tbodyArray.getJSONObject(i);
                        String actionType = tbodyObj.getString("actionType");
                        List<String> valueList = new ArrayList<>();
                        valueList.add(actionType);
                        List<String> actualValueList = new ArrayList<>();
                        String actualValue = "";
                        if ("insert".equals(actionType)) {
                            actualValue = "新增";
                        } else if ("update".equals(actionType)) {
                            actualValue = "编辑";
                        } else if ("delete".equals(actionType)) {
                            actualValue = "删除";
                        }
                        actualValueList.add(actualValue);
                        JSONObject actionTypeObj = new JSONObject();
                        actionTypeObj.put("type", "text");
                        actionTypeObj.put("valueList", valueList);
                        actionTypeObj.put("actualValueList", actualValueList);
                        JSONObject attrEntityData = tbodyObj.getJSONObject("attrEntityData");
                        attrEntityData.put("actionType", actionTypeObj);
                        tbodyList.add(attrEntityData);
                    }
                    tableObj.put("tbodyList", tbodyList);
                    tableObj.put("ciId", ciId);
                    tableObj.put("ciLabel", ciLabelMap.get(ciId));
                    tableArray.add(tableObj);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return tableArray;
    }

    @Override
    public Object textConversionValue(List<String> values, JSONObject config) {
        return null;
    }

    @Override
    public String getHandlerName() {
        return "配置项修改组件";
    }

    @Override
    public String getIcon() {
        return "tsfont-cientityselect";
    }

    @Override
    public ParamType getParamType() {
        return null;
    }

    @Override
    public String getDataType() {
        return null;
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
    public String getModule() {
        return "cmdb";
    }

    @Override
    public boolean isForTemplate() {
        return false;
    }

    @Override
    public int getSort() {
        return 10;
    }

    @Override
    public boolean isAudit() {
        return true;
    }

    @Override
    public String getHandlerType(FormConditionModel model) {
        return null;
    }

    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return null;
    }
}
