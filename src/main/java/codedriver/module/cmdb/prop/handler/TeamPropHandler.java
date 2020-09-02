package codedriver.module.cmdb.prop.handler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import codedriver.framework.cmdb.constvalue.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;

@Component
public class TeamPropHandler implements IPropertyHandler {

    @Override
    public String getName() {
        return "team";
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
            valueList.add(value.toString());
        }
        return valueList;
    }

}
