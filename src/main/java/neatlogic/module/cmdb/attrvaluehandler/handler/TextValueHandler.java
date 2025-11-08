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

import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.enums.SearchExpression;
import org.springframework.stereotype.Service;


@Service
public class TextValueHandler implements IAttrValueHandler {

    @Override
    public String getType() {
        return "text";
    }

    @Override
    public String getName() {
        return "文本框";
    }

    @Override
    public String getIcon() {
        return "tsfont-forminput";
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
        return false;
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
        return false;
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
        return new SearchExpression[]{SearchExpression.EQ, SearchExpression.NE, SearchExpression.LI,
                SearchExpression.NL, SearchExpression.NOTNULL, SearchExpression.NULL};

    }

    @Override
    public int getSort() {
        return 1;
    }


}
