package codedriver.module.cmdb.prop.handler;

import codedriver.framework.cmdb.constvalue.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.exception.user.UserNotFoundException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserPropHandler implements IPropertyHandler {

    @Autowired
    private UserMapper userMapper;

    @Override
    public String getName() {
        return "user";
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
        //根据values查找uuid，目前只支持用userId查找
        JSONArray array = new JSONArray();
        if(CollectionUtils.isNotEmpty(values)){
            Integer isMultiple = null;
            if(MapUtils.isNotEmpty(config)){
                isMultiple = config.getInteger("isMultiple");
            }
            if(values.size() > 1 && isMultiple != null && isMultiple.intValue() != 1){
                throw new RuntimeException("不支持多选");
            }
            for(String userId : values){
                UserVo user = userMapper.getUserByUserId(userId);
                if(user != null){
                    JSONObject obj = new JSONObject();
                    obj.put("text",user.getUserName());
                    obj.put("value", GroupSearch.USER.getValuePlugin() + user.getUuid());
                    array.add(obj);
                }else{
                   throw new UserNotFoundException(userId);
                }
            }
        }
        return array;
    }
}
