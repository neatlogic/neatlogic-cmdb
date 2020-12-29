package codedriver.module.cmdb.prop.handler;

import codedriver.framework.cmdb.constvalue.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TextareaPropHandler implements IPropertyHandler {

    @Override
    public String getName() {
        return "textarea";
    }

    @Override
    public Boolean canSearch() {
        return true;
    }

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[] {SearchExpression.LI, SearchExpression.NL, SearchExpression.NOTNULL,
            SearchExpression.NULL};
    }

    @Override
    public Object getActualValue(List<String> values, JSONObject config) {
        JSONArray array = new JSONArray();
        if (CollectionUtils.isNotEmpty(values)) {
            array.add(values.get(0));
        }
        return array;
    }
}
