/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.validator.handler;

import neatlogic.framework.cmdb.validator.core.ValidatorBase;
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
