package codedriver.module.cmdb.prop.handler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import codedriver.framework.cmdb.constvalue.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;

@Component
public class TablePropHandler implements IPropertyHandler {

    @Override
    public String getName() {
        return "table";
    }

    @Override
    public Boolean canSearch() {
        return false;
    }

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[] {};
    }

    @Override
    public List<String> transferValue(Object value) {
        List<String> valueList = new ArrayList<>();
        if (value != null) {
            valueList.add(value.toString());
        }
        return valueList;
    }

}
