package codedriver.module.cmdb.prop.handler;

import codedriver.framework.cmdb.constvalue.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;
import codedriver.framework.matrix.dao.mapper.MatrixDataMapper;
import codedriver.framework.matrix.dto.MatrixColumnVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

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
        if(CollectionUtils.isNotEmpty(values) && MapUtils.isNotEmpty(config)){
            String datasource = config.getString("datasource");
            Integer isMultiple = config.getInteger("isMultiple");
            if(Objects.equals(isMultiple,0) && values.size() > 1){
                throw new RuntimeException("不支持多选");
            }
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
            }else if("cube".equals(datasource)){ //从矩阵中反查值字段
                //todo 反查外部数据源
                String matrixUuid = config.getString("cube");
                String textKey = config.getString("textKey");
                String valueKey = config.getString("valueKey");
                for(String value : values){
                    JSONObject obj = new JSONObject();
                    MatrixColumnVo vo = new MatrixColumnVo();
                    vo.setColumn(textKey);
                    vo.setValue(value);
                    List<String> data = matrixDataMapper.getDynamicTableCellData(matrixUuid, vo, valueKey);
                    if(CollectionUtils.isNotEmpty(data)){
                        obj.put("text",value);
                        obj.put("value",data.get(0));
                        array.add(obj);
                    }
                }
            }
        }

        return array;
    }

}
