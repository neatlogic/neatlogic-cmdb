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

import java.util.List;
@Deprecated
public class CheckboxPropHandler implements IPropertyHandler {

    @Override
    public String getName() {
        return "checkbox";
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
    public Object getActualValue(List<String> values, JSONObject config) {
        JSONArray array = new JSONArray();
        if (CollectionUtils.isNotEmpty(values)) {
            if (MapUtils.isNotEmpty(config)) {
                JSONArray dataList = config.getJSONArray("dataList");
                for (int i = 0; i < values.size(); i++) {
                    if (dataList.contains(values.get(i))) {
                        array.add(values.get(i));
                    } else {
                        throw new RuntimeException(values.get(i) + "不在复选框可选值范围内");
                    }
                }
            }
        }
        return array;
    }
}
