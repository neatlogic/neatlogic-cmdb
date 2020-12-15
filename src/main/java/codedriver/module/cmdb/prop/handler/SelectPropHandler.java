package codedriver.module.cmdb.prop.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.cmdb.constvalue.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.matrix.dao.mapper.MatrixDataMapper;

@Component
public class SelectPropHandler implements IPropertyHandler {

    @Autowired
    private MatrixDataMapper matrixDataMapper;

    @Override
    public String getName() {
        return "select";
    }

    @Override
    public Boolean canSearch() {
        return true;
    }

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[] {SearchExpression.EQ, SearchExpression.NE, SearchExpression.LI,
            SearchExpression.NL, SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public List<String> getDisplayValueList(List<String> valueList) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getValueHash(String value) {
        if (StringUtils.isNotBlank(value)) {
            try {
                JSONObject json = JSONObject.parseObject(value);
                if (StringUtils.isNotBlank(json.getString("value"))) {
                    return DigestUtils.md5DigestAsHex(json.getString("value").toLowerCase().getBytes());
                }
            } catch (Exception ex) {
            }
        }
        return null;
    }

    @Override
    public Object getActualValue(List<String> values, JSONObject config) {
        JSONArray array = new JSONArray();
        if (CollectionUtils.isNotEmpty(values) && MapUtils.isNotEmpty(config)) {
            String datasource = config.getString("datasource");
            Integer isMultiple = config.getInteger("isMultiple");
            if (Objects.equals(isMultiple, 0) && values.size() > 1) {
                throw new RuntimeException("不支持多选");
            }
            if ("enum".equals(datasource)) {
                JSONArray dataList = config.getJSONArray("dataList");
                if (CollectionUtils.isNotEmpty(dataList)) {
                    for (String value : values) {
                        if (dataList.contains(value)) {
                            JSONObject obj = new JSONObject();
                            obj.put("text", value);
                            obj.put("value", value);
                            array.add(obj);
                        } else {
                            throw new RuntimeException(value + "不在可选值范围内");
                        }
                    }
                }
            } else if ("cube".equals(datasource)) { // 从矩阵中反查值字段
                List<String> valuesCopy = new ArrayList<>(values);
                List<String> existValues = new ArrayList<>();
                String matrixUuid = config.getString("cube");
                String textKey = config.getString("textKey");
                String valueKey = config.getString("valueKey");
                List<ValueTextVo> data =
                    matrixDataMapper.getDynamicTableCellDataMap(matrixUuid, textKey, valueKey, values);
                if (CollectionUtils.isNotEmpty(data)) {
                    for (ValueTextVo vo : data) {
                        JSONObject obj = new JSONObject();
                        obj.put("text", vo.getText());
                        obj.put("value", vo.getValue());
                        array.add(obj);
                        existValues.add(vo.getText());
                    }
                }
                if (CollectionUtils.isNotEmpty(existValues)) {
                    valuesCopy.removeAll(existValues);
                    if (valuesCopy.size() > 0) {
                        throw new RuntimeException(valuesCopy.toString() + "不在可选范围内");
                    }
                }
            }
        }
        return array;
    }

}
