package codedriver.module.cmdb.validator.handler;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.cmdb.exception.validator.AttrInValidatedException;
import codedriver.framework.cmdb.validator.core.ValidatorBase;

@Component
public class RegexValidator extends ValidatorBase {

    @Override
    public boolean valid(String value, Long validatorId) throws AttrInValidatedException {
        return false;
    }

    @Override
    public String getName() {
        return "正则表达式校验组件";
    }

    @Override
    protected boolean myValid(String value, JSONObject config) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public JSONArray getForm() {
        JSONArray itemList = new JSONArray();
        JSONObject itemObj = new JSONObject();
        itemObj.put("name", "config_regex");
        itemObj.put("type", "text");
        itemObj.put("label", "正则表达式");
        itemObj.put("validateList", new String[] {"required"});
        itemList.add(itemObj);
        return itemList;
    }

}
