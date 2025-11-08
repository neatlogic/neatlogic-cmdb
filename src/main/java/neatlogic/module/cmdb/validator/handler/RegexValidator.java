/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

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
