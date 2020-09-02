package codedriver.module.cmdb.prop.handler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;

import codedriver.framework.cmdb.constvalue.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;

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
        return new SearchExpression[] {SearchExpression.EQ, SearchExpression.GE, SearchExpression.GT,
            SearchExpression.LE, SearchExpression.LT, SearchExpression.LI, SearchExpression.NE, SearchExpression.NL,
            SearchExpression.NOTNULL, SearchExpression.NULL,};
    }

    @Override
    public List<String> transferValue(Object value) {
        List<String> valueList = new ArrayList<>();
        if (value != null) {
            if (value instanceof List) {
                List<Object> tl = (List)value;
                for (Object v : tl) {
                    if (v != null) {
                        valueList.add(v.toString());
                    }
                }
            } else {
                String v = value.toString();
                v = v.trim();
                if (v.startsWith("[") && v.endsWith("]")) {
                    try {
                        JSONArray tl = JSONArray.parseArray(v);
                        for (int i = 0; i < tl.size(); i++) {
                            valueList.add(tl.getString(i));
                        }
                    } catch (Exception ex) {
                        valueList.add(v);
                    }
                }
            }
        }
        return valueList;
    }

}
