package codedriver.module.cmdb.prop.handler;

import codedriver.framework.cmdb.constvalue.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Component
public class DatePropHandler implements IPropertyHandler {

    @Override
    public String getName() {
        return "date";
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
    public List<String> getDisplayValue(List<String> valueList) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getActualValue(List<String> values, JSONObject config) {
        JSONArray array = new JSONArray();
        //日期不存在多选
        if(CollectionUtils.isNotEmpty(values) && values.size() > 1){
            throw new RuntimeException("只能选择一个日期");
        }
        if(MapUtils.isNotEmpty(config)){
            String format = config.getString("format");
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            try {
                sdf.parse(values.get(0));
                array.add(values.get(0));
            } catch (ParseException e) {
                throw new RuntimeException("时间不符合格式：" + format);
            }
        }
        return array;
    }
}
