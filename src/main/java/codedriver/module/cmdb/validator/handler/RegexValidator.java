/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.validator.handler;

import codedriver.framework.cmdb.validator.core.ValidatorBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class RegexValidator extends ValidatorBase {

    @Override
    public String getName() {
        return "正则表达式校验组件";
    }

    @Override
    public JSONArray getForm() {
        JSONArray itemList = new JSONArray();
        JSONObject itemObj = new JSONObject();
        itemObj.put("name", "config_regex");
        itemObj.put("type", "text");
        itemObj.put("label", "正则表达式");
        itemObj.put("validateList", new String[]{"required"});
        itemList.add(itemObj);
        return itemList;
    }

    @Override
    protected boolean myValid(JSONArray valueList, JSONObject config) {
        if (CollectionUtils.isNotEmpty(valueList) && config != null && config.containsKey("regex")) {
            String regex = config.getString("regex");
            if (StringUtils.isNotBlank(regex)) {
                Pattern pattern = Pattern.compile(regex);
                return pattern.matcher(valueList.stream().map(Object::toString).collect(Collectors.joining(","))).matches();
            }
        }
        return false;
    }

}
