/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.validator.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.validator.core.ValidatorBase;
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
        itemObj.put("name", "regex");
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
