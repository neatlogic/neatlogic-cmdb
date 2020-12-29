package codedriver.module.cmdb.prop.handler;

import codedriver.framework.cmdb.constvalue.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FilePropHandler implements IPropertyHandler {

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public Boolean canSearch() {
        return true;
    }

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[] {SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public Object getActualValue(List<String> values, JSONObject config) {
        return null;
    }
}
