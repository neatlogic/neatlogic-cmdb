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

import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.enums.SearchExpression;
import org.springframework.stereotype.Service;


@Service
public class HyperLinkValueHandler implements IAttrValueHandler {

    @Override
    public String getType() {
        return "hyperlink";
    }

    @Override
    public String getName() {
        return "超链接";
    }

    @Override
    public String getIcon() {
        return "tsfont-bind";
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
        return false;
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
        return false;
    }

    @Override
    public boolean isUniqueAttr() {
        return false;
    }

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[]{SearchExpression.NOTNULL, SearchExpression.NULL};

    }

    @Override
    public int getSort() {
        return 15;
    }


}
