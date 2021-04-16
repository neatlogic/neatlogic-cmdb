/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.prop.handler;

import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RadioPropHandler implements IPropertyHandler {

    @Override
    public String getName() {
        return "radio";
    }

    @Override
    public Boolean canSearch() {
        return true;
    }

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[] {SearchExpression.EQ, SearchExpression.NE, SearchExpression.LI,
            SearchExpression.NL, SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public Object getActualValue(List<String> values, JSONObject config) throws Exception {
        JSONArray array = new JSONArray();
        if (CollectionUtils.isNotEmpty(values) && values.size() > 1) {
            throw new RuntimeException("单选框不可多选");
        }
        if (MapUtils.isNotEmpty(config)) {
            JSONArray dataList = config.getJSONArray("dataList");
            if (CollectionUtils.isNotEmpty(dataList) && dataList.contains(values.get(0))) {
                array.add(values.get(0));
            } else {
                throw new RuntimeException("所选值不在单选框可选值范围内");
            }
        }

        return array;
    }
}
