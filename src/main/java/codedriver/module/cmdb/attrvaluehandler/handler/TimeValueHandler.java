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
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;

import java.text.ParseException;


@Service
public class TimeValueHandler implements IAttrValueHandler {

    @Override
    public String getType() {
        return "time";
    }

    @Override
    public String getName() {
        return "时间";
    }

    @Override
    public String getIcon() {
        return "tsfont-sla";
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
            for (int i = 0; i < valueList.size(); i++) {
                try {
                    DateUtils.parseDate(valueList.getString(i), "HH:mm:ss");
                } catch (ParseException e) {
                    throw new DatetimeAttrFormatIrregularException(attrVo, valueList.getString(i), "HH:mm:ss");
                }
            }
        }
    }

    /*
    @Override
    public JSONArray getActualValueList(AttrVo attrVo, JSONArray valueList) {
        JSONArray returnList = new JSONArray();
        for (int i = 0; i < valueList.size(); i++) {
            try {
                String v = valueList.getString(i);
                if (v.contains(",")) {
                    v = v.replace(",", "~");
                }
                returnList.add(v);
            } catch (Exception ignored) {

            }
        }
        return returnList;
    }


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
*/

    @Override
    public int getSort() {
        return 6;
    }

}