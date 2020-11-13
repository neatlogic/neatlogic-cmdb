package codedriver.module.cmdb.prop.handler;

import codedriver.framework.cmdb.constvalue.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.exception.team.FoundRepeatNameTeamException;
import codedriver.framework.exception.team.TeamNotFoundException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TeamPropHandler implements IPropertyHandler {

    @Autowired
    private TeamMapper teamMapper;

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
    public List<String> getDisplayValue(List<String> valueList) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getActualValue(List<String> values, JSONObject config) {
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
                List<String> uuid = teamMapper.getTeamUuidByName(name);
                if(CollectionUtils.isNotEmpty(uuid) && uuid.size() == 1){
                    JSONObject obj = new JSONObject();
                    obj.put("text",name);
                    obj.put("value", GroupSearch.TEAM.getValuePlugin() + uuid);
                    array.add(obj);
                }else if(CollectionUtils.isEmpty(uuid)){
                    throw new TeamNotFoundException(name);
                }else if(uuid.size() != 1){
                    throw new FoundRepeatNameTeamException(name);
                }
            }
        }
        return array;
    }
}
