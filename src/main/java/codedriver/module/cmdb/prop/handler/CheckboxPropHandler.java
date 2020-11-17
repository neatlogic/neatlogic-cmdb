package codedriver.module.cmdb.prop.handler;

import codedriver.framework.cmdb.constvalue.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CheckboxPropHandler implements IPropertyHandler {

    @Override
    public String getName() {
        return "checkbox";
    }

    @Override
    public Boolean canSearch() {
        return true;
    }

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[] {SearchExpression.EQ, SearchExpression.LI, SearchExpression.NE,
            SearchExpression.NL, SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public List<String> getDisplayValue(List<String> valueList) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getActualValue(List<String> values, JSONObject config) {
        JSONArray array = new JSONArray();
        if (CollectionUtils.isNotEmpty(values)) {
            if (MapUtils.isNotEmpty(config)) {
                JSONArray dataList = config.getJSONArray("dataList");
                for (int i = 0; i < values.size(); i++) {
                    if (dataList.contains(values.get(i))) {
                        array.add(values.get(i));
                    } else {
                        throw new RuntimeException(values.get(i) + "不在复选框可选值范围内");
                    }
                }
            }
        }
        return array;
    }
}