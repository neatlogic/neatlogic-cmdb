/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.attrvaluehandler.handler;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.framework.cmdb.exception.validator.DatetimeAttrFormatIrregularException;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


@Service
public class DateTimeValueHandler implements IAttrValueHandler {

    @Override
    public String getType() {
        return "datetime";
    }

    @Override
    public String getName() {
        return "日期时间";
    }

    @Override
    public String getIcon() {
        return "tsfont-calendar";
    }

    @Override
    public boolean isCanSearch() {
        return true;
    }

    @Override
    public boolean isCanSort() {
        return true;
    }

    @Override
    public boolean isCanInput() {
        return true;
    }

    @Override
    public boolean isCanImport() {
        return true;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public boolean isNeedTargetCi() {
        return false;
    }

    @Override
    public boolean isNeedConfig() {
        return true;
    }

    @Override
    public boolean isNeedWholeRow() {
        return false;
    }

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[]{SearchExpression.BT, SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public void transferValueListToSave(AttrVo attrVo, JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            int len = valueList.size();
            for (int i = len - 1; i >= 0; i--) {
                String v = valueList.getString(i);
                if (StringUtils.isBlank(v)) {
                    valueList.remove(i);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(valueList)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < valueList.size(); i++) {
                try {
                    //如果前端什么都不修改传回来的值就是Long，所以要转换一下
                    if (valueList.get(i) instanceof Long) {
                        valueList.set(i, sdf.format(new Date(valueList.getLong(i))));
                    } else {
                        DateUtils.parseDate(valueList.getString(i), "yyyy-MM-dd HH:mm:ss");
                    }
                } catch (ParseException e) {
                    throw new DatetimeAttrFormatIrregularException(attrVo, valueList.getString(i), "yyyy-MM-dd HH:mm:ss");
                }
            }
        }
    }

    @Override
    public JSONArray getActualValueList(AttrVo attrVo, JSONArray valueList) {
        JSONArray returnList = new JSONArray();
        for (int i = 0; i < valueList.size(); i++) {
            String v = valueList.getString(i);
            try {
                if (MapUtils.isNotEmpty(attrVo.getConfig()) && StringUtils.isNotBlank(attrVo.getConfig().getString("format"))) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    SimpleDateFormat sdf2 = new SimpleDateFormat(attrVo.getConfig().getString("format"));
                    Date d = sdf.parse(v);
                    returnList.add(sdf2.format(d));
                } else {
                    returnList.add(v);
                }
            } catch (Exception ignored) {
                returnList.add(v);
            }
        }
        return returnList;
    }


    @Override
    public int getSort() {
        return 8;
    }

}
