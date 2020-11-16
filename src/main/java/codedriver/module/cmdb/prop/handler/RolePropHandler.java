package codedriver.module.cmdb.prop.handler;

import codedriver.framework.cmdb.constvalue.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.dao.mapper.RoleMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RolePropHandler implements IPropertyHandler {

    @Autowired
    private RoleMapper roleMapper;

    @Override
    public String getName() {
        return "role";
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
        //todo 暂无角色属性控件，可能有变
        JSONArray array = new JSONArray();
        if(CollectionUtils.isNotEmpty(values)){
            Integer isMultiple = null;
            if(MapUtils.isNotEmpty(config)){
                isMultiple = config.getInteger("isMultiple");
            }
            if(values.size() > 1 && isMultiple != null && isMultiple.intValue() != 1){
                throw new RuntimeException("不支持多选");
            }

            List<String> valuesCopy = new ArrayList<>(values);
            List<String> existValues = new ArrayList<>();
            List<ValueTextVo> list = roleMapper.getRoleUuidAndNameMapList(values);
            if(CollectionUtils.isNotEmpty(list)){
                for(ValueTextVo vo : list){
                    JSONObject obj = new JSONObject();
                    obj.put("text",vo.getText());
                    obj.put("value", vo.getValue());
                    array.add(obj);
                    existValues.add(vo.getText());
                }
            }

            if(CollectionUtils.isNotEmpty(existValues)){
                valuesCopy.removeAll(existValues);
                if(valuesCopy.size() > 0){
                    throw new RuntimeException("角色：" + valuesCopy.toString() + "不存在");
                }
            }
        }
        return array;
    }
}
