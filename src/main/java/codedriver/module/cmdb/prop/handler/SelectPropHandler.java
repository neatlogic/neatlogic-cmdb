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
public class SelectPropHandler implements IPropertyHandler {

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
        // todo
        JSONArray array = new JSONArray();
        if(CollectionUtils.isNotEmpty(values) && MapUtils.isNotEmpty(config)){
            String datasource = config.getString("datasource");
            if("enum".equals(datasource)){
                JSONArray dataList = config.getJSONArray("dataList");
                if(CollectionUtils.isNotEmpty(dataList)){
                    for(String value : values){
                        if(dataList.contains(value)){
                            array.add(value);
                        }else{
                            throw new RuntimeException(value + "不在可选值范围内");
                        }
                    }
                }
            }
            //todo 矩阵数据待支持
        }

        return array;
    }

}
