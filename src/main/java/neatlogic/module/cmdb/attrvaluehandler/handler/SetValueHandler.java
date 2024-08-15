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
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.framework.cmdb.exception.attr.AttrValueIrregularException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;


@Service
public class SetValueHandler implements IAttrValueHandler {

    @Override
    public String getType() {
        return "set";
    }

    @Override
    public String getName() {
        return "多选集合";
    }

    @Override
    public String getIcon() {
        return "tsfont-check-square-o";
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
    public boolean isCanSort() {
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
        return new SearchExpression[]{SearchExpression.LI, SearchExpression.NL, SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public int getSort() {
        return 5;
    }

    @Override
    public JSONArray getActualValueList(AttrVo attrVo, JSONArray valueList) {
        JSONArray returnList = new JSONArray();
        if (CollectionUtils.isNotEmpty(valueList)) {
            for (int i = 0; i < valueList.size(); i++) {
                returnList.addAll(Arrays.asList(valueList.getString(i).split(",")));
            }
        }
        return returnList;
    }

    @Override
    public String getValue(JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            String v = "";
            for (int i = 0; i < valueList.size(); i++) {
                if (StringUtils.isNotBlank(valueList.getString(i))) {
                    if (StringUtils.isNotBlank(v)) {
                        v += ",";
                    }
                    v += valueList.getString(i);
                }
            }
            return v;
        }
        return null;
    }

    @Override
    public Object transferValueListToInput(AttrVo attrVo, Object value) {
        if (value != null && StringUtils.isNotBlank(value.toString())) {
            JSONArray newValue = new JSONArray();
            newValue.addAll(Arrays.asList(value.toString().split(",")));
            return newValue;
        }
        return value;
    }

    @Override
    public boolean valid(AttrVo attrVo, JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            JSONObject config = attrVo.getConfig();
            JSONArray members = config.getJSONArray("members");
            for (int i = 0; i < valueList.size(); i++) {
                String v = valueList.getString(i);
                if (StringUtils.isNotBlank(v) && !members.contains(v)) {
                    throw new AttrValueIrregularException(attrVo, v);
                }
            }
        }
        return true;
    }

}
