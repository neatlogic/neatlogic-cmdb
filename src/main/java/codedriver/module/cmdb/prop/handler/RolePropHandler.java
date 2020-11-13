package codedriver.module.cmdb.prop.handler;

import codedriver.framework.cmdb.constvalue.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.exception.role.FoundRepeatNameRoleException;
import codedriver.framework.exception.role.RoleNotFoundException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
            for(String name : values){
                List<String> uuid = roleMapper.getRoleUuidByName(name);
                if(CollectionUtils.isNotEmpty(uuid) && uuid.size() == 1){
                    JSONObject obj = new JSONObject();
                    obj.put("text",name);
                    obj.put("value", GroupSearch.ROLE.getValuePlugin() + uuid);
                    array.add(obj);
                }else if(CollectionUtils.isEmpty(uuid)){
                    throw new RoleNotFoundException(name);
                }else if(uuid.size() != 1){
                    throw new FoundRepeatNameRoleException(name);
                }
            }
        }
        return array;
    }
}
