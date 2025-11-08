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

package neatlogic.module.cmdb.attrvaluehandler.handler;

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.framework.common.util.RC4Util;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;


@Service
public class PasswordValueHandler implements IAttrValueHandler {

    @Override
    public String getType() {
        return "password";
    }

    @Override
    public String getName() {
        return "密码";
    }

    @Override
    public String getIcon() {
        return "tsfont-option-horizontal";
    }

    @Override
    public boolean isCanSearch() {
        return false;
    }

    @Override
    public boolean isCanSort() {
        return false;
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
        return false;
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
        return new SearchExpression[]{SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public int getSort() {
        return 11;
    }

    @Override
    public void transferValueListToSave(AttrVo attrVo, JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            for (int i = 0; i < valueList.size(); i++) {
                String value = valueList.getString(i);
                valueList.set(i, RC4Util.encrypt(value));
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
        if (CollectionUtils.isNotEmpty(valueList)) {
            for (int i = 0; i < valueList.size(); i++) {
                String value = valueList.getString(i);
                valueList.set(i, RC4Util.decrypt(value));
            }
        }
    }

    @Override
    public JSONArray transferValueListToExport(AttrVo attrVo, JSONArray valueList) {
        JSONArray returnValueList = new JSONArray();
        if (CollectionUtils.isNotEmpty(valueList)) {
            for (int i = 0; i < valueList.size(); i++) {
                returnValueList.add("*******");
            }
        }
        return returnValueList;
    }
}
