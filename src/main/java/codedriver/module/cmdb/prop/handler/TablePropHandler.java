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
    public List<String> getDisplayValue(List<String> valueList) {
        // TODO Auto-generated method stub
        return null;
    }

}
