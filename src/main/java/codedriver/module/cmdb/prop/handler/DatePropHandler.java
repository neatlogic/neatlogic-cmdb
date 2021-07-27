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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
@Deprecated
public class DatePropHandler implements IPropertyHandler {

    @Override
    public String getName() {
        return "date";
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
        // 日期不存在多选
        if (CollectionUtils.isNotEmpty(values) && values.size() > 1) {
            throw new RuntimeException("只能选择一个日期");
        }
        if (CollectionUtils.isNotEmpty(values) && MapUtils.isNotEmpty(config)) {
            String format = config.getString("format");
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            if (Objects.equals(values.get(0).length(), format.length())) {
                try {
                    sdf.setLenient(false);
                    sdf.parse(values.get(0));
                    array.add(values.get(0));
                } catch (ParseException e) {
                    throw new RuntimeException("时间不符合格式：" + format);
                }
            } else {
                throw new RuntimeException("时间不符合格式：" + format);
            }
        }
        return array;
    }
}
