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
import neatlogic.framework.cmdb.exception.attr.AttrValueIrregularException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Objects;


@Service
public class NumberValueHandler implements IAttrValueHandler {

    @Override
    public String getType() {
        return "number";
    }

    @Override
    public String getName() {
        return "数字";
    }

    @Override
    public String getIcon() {
        return "tsfont-chart-number";
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
    public boolean isNeedWholeRow() {
        return false;
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
        return true;
    }

    @Override
    public boolean isUniqueAttr() {
        return true;
    }

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[]{SearchExpression.BT, SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public int getSort() {
        return 2;
    }

    public static void main(String[] a) throws ParseException {
        DecimalFormat df = new DecimalFormat("0.##");
        System.out.println(df.format(df.parse("12")));
    }

    @Override
    public JSONArray getActualValueList(AttrVo attrVo, JSONArray valueList) {


        JSONArray actualValueList = new JSONArray();
        if (CollectionUtils.isNotEmpty(valueList)) {
            DecimalFormat df = new DecimalFormat("0.####");
            if (attrVo.getConfig() != null && StringUtils.isNotBlank(attrVo.getConfig().getString("format"))) {
                String format = attrVo.getConfig().getString("format");
                switch (format) {
                    case "1":
                        df = new DecimalFormat("0.0");
                        break;
                    case "2":
                        df = new DecimalFormat("0.00");
                        break;
                    case "3":
                        df = new DecimalFormat("0.000");
                        break;
                    case "4":
                        df = new DecimalFormat("0.0000");
                        break;
                }
            }
            for (int i = 0; i < valueList.size(); i++) {
                try {
                    actualValueList.add(df.format(df.parse(valueList.getString(i))));
                } catch (Exception ignored) {

                }
            }
        }
        return actualValueList;
    }

    /**
     * 将值转换成显示的形式
     *
     * @param valueList 数据库的数据
     * @return 用于显示数据
     */
    @Override
    public void transferValueListToDisplay(AttrVo attrVo, JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            DecimalFormat df = new DecimalFormat("0.####");
            if (attrVo.getConfig() != null && StringUtils.isNotBlank(attrVo.getConfig().getString("format"))) {
                String format = attrVo.getConfig().getString("format");
                switch (format) {
                    case "1":
                        df = new DecimalFormat("0.0");
                        break;
                    case "2":
                        df = new DecimalFormat("0.00");
                        break;
                    case "3":
                        df = new DecimalFormat("0.000");
                        break;
                    case "4":
                        df = new DecimalFormat("0.0000");
                        break;
                }
            }
            for (int i = 0; i < valueList.size(); i++) {
                try {
                    valueList.set(i, df.format(df.parse(valueList.getString(i))));
                } catch (Exception ignored) {

                }
            }
        }
    }

    private static void checkDecimalPlaces(String numberStr, int decimalPlaces) {
        BigDecimal bigDecimal = new BigDecimal(numberStr);
        bigDecimal = bigDecimal.stripTrailingZeros();

        int actualDecimalPlaces = bigDecimal.scale();

        if (actualDecimalPlaces > decimalPlaces) {
            throw new IllegalArgumentException("数字的小数位数不符合要求，期望: " + decimalPlaces + "，实际: " + actualDecimalPlaces);
        }
    }

    @Override
    public boolean valid(AttrVo attrVo, JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            String format = "auto";
            if (attrVo.getConfig() != null && StringUtils.isNotBlank(attrVo.getConfig().getString("format"))) {
                format = attrVo.getConfig().getString("format");
            }
            for (int i = 0; i < valueList.size(); i++) {
                String v = valueList.getString(i);
                if (StringUtils.isNotBlank(v)) {
                    if (!Objects.equals("auto", format)) {
                        try {
                            switch (format) {
                                case "1":
                                    checkDecimalPlaces(v, 1);
                                    break;
                                case "2":
                                    checkDecimalPlaces(v, 2);
                                    break;
                                case "3":
                                    checkDecimalPlaces(v, 3);
                                    break;
                                case "4":
                                    checkDecimalPlaces(v, 4);
                                    break;
                            }
                        } catch (IllegalArgumentException e) {
                            throw new AttrValueIrregularException(attrVo, e);
                        }
                    } else {
                        try {
                            Double.parseDouble(v);
                        } catch (Exception ex) {
                            throw new AttrValueIrregularException(attrVo, v);
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public JSONArray transferValueListToExport(AttrVo attrVo, JSONArray valueList) {
        JSONArray actualValueList = new JSONArray();
        if (CollectionUtils.isNotEmpty(valueList)) {
            DecimalFormat df = new DecimalFormat("0.####");
            if (attrVo.getConfig() != null && StringUtils.isNotBlank(attrVo.getConfig().getString("format"))) {
                String format = attrVo.getConfig().getString("format");
                switch (format) {
                    case "1":
                        df = new DecimalFormat("0.0");
                        break;
                    case "2":
                        df = new DecimalFormat("0.00");
                        break;
                    case "3":
                        df = new DecimalFormat("0.000");
                        break;
                    case "4":
                        df = new DecimalFormat("0.0000");
                        break;
                }
            }
            for (int i = 0; i < valueList.size(); i++) {
                try {
                    actualValueList.add(df.format(df.parse(valueList.getString(i))));
                } catch (Exception ignored) {

                }
            }
        }
        return actualValueList;
    }


}
