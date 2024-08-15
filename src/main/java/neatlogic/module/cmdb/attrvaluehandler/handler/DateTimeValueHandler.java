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

package neatlogic.module.cmdb.attrvaluehandler.handler;

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.framework.cmdb.exception.validator.DatetimeAttrFormatIrregularException;
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
    public boolean isNameAttr() {
        return false;
    }

    @Override
    public boolean isUniqueAttr() {
        return false;
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
            String format = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            for (int i = 0; i < valueList.size(); i++) {
                try {
                    //如果前端什么都不修改传回来的值就是Long，所以要转换一下
                    if (valueList.get(i) instanceof Long) {
                        valueList.set(i, sdf.format(new Date(valueList.getLong(i))));
                    } else {
                        DateUtils.parseDate(valueList.getString(i), format);
                    }
                } catch (ParseException e) {
                    throw new DatetimeAttrFormatIrregularException(attrVo, valueList.getString(i), format);
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
    public void transferValueListToDisplay(AttrVo attrVo, JSONArray valueList) {
        for (int i = 0; i < valueList.size(); i++) {
            String v = valueList.getString(i);
            try {
                if (MapUtils.isNotEmpty(attrVo.getConfig()) && StringUtils.isNotBlank(attrVo.getConfig().getString("format"))) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    SimpleDateFormat sdf2 = new SimpleDateFormat(attrVo.getConfig().getString("format"));
                    Date d = sdf.parse(v);
                    valueList.set(i, sdf2.format(d));
                }
            } catch (Exception ignored) {
            }
        }
    }


    @Override
    public int getSort() {
        return 9;
    }

}
