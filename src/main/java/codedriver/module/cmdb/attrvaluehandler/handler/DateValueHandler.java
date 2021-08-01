/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.attrvaluehandler.handler;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.framework.cmdb.exception.validator.DatetimeAttrFormatIrregularException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;

import java.text.ParseException;


@Service
public class DateValueHandler implements IAttrValueHandler {

    @Override
    public String getType() {
        return "date";
    }

    @Override
    public String getName() {
        return "日期时间";
    }

    @Override
    public String getIcon() {
        return "ts-calendar";
    }

    @Override
    public boolean isCanSearch() {
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
        return new SearchExpression[]{SearchExpression.EQ, SearchExpression.NE, SearchExpression.LI,
                SearchExpression.NL, SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public void transferValueListToSave(AttrVo attrVo, JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            JSONObject config = attrVo.getConfig();
            String type = config.getString("type");
            String format = config.getString("format");
            if (StringUtils.isNotBlank(type) && StringUtils.isNotBlank(format)) {
                if (type.equals("time") || type.equals("datetime")) {
                    for (int i = 0; i < valueList.size(); i++) {
                        try {
                            DateUtils.parseDate(valueList.getString(i), format);
                        } catch (ParseException e) {
                            throw new DatetimeAttrFormatIrregularException(attrVo, valueList.getString(i), format);
                        }
                    }
                } else if (type.equals("datetimerange") || type.equals("timerange")) {
                    for (int i = 0; i < valueList.size(); i++) {
                        try {
                            //数据库存的是yyyy-mm-dd,yyyy-mm-dd，显示是yyyy-mm-dd~yyyy-mm-dd，所以需要转换，因为excel导入时用的是显示格式
                            String v = valueList.getString(i);
                            if (v.contains("~")) {
                                v = v.replace("~", ",");
                            }
                            String[] vs = v.split(",");
                            if (vs.length != 2) {
                                throw new DatetimeAttrFormatIrregularException(attrVo, valueList.getString(i), format + "~" + format);
                            }
                            DateUtils.parseDate(vs[0], format);
                            DateUtils.parseDate(vs[1], format);
                            valueList.set(i, v);
                        } catch (ParseException e) {
                            throw new DatetimeAttrFormatIrregularException(attrVo, valueList.getString(i), format + "~" + format);
                        }
                    }
                }
            }
        }
    }

    /**
     * 将值转换成显示的形式
     *
     * @param valueList 数据库的数据
     * @return 用于显示数据
     */
    @Override
    public void transferValueListToDisplay(AttrVo attrVo, JSONArray valueList) {
        for (int i = 0; i < valueList.size(); i++) {
            try {
                String v = valueList.getString(i);
                if (v.contains(",")) {
                    v = v.replace(",", "~");
                }
                valueList.set(i, v);
            } catch (Exception ignored) {

            }
        }
    }

    @Override
    public int getSort() {
        return 4;
    }

}
