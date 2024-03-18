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

package neatlogic.module.cmdb.dsl.core;

import neatlogic.framework.util.SnowflakeUtil;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectFragment {
    private String alias;
    private Select select;
    private final Set<Long> attrCheckSet = new HashSet<>();//检查属性是否已经配置在select里了，因为模型有继承，没有用到属性模型可以不Join，提升检索效率
    private final Set<Long> ciCheckSet = new HashSet<>();//检查模型是否存在，不存在才增加join
    private List<SearchItem> prevItemList;//记录前面需要连接的关系或属性

    public SelectFragment(Select select) {
        this.select = select;
    }

    public String getAlias() {
        if (StringUtils.isBlank(alias)) {
            alias = "item_" + SnowflakeUtil.uniqueLong();
        }
        return alias;
    }


    public Select getSelect() {
        return select;
    }

    public void setSelect(Select select) {
        this.select = select;
    }


    public boolean isAttrExists(Long attrId) {
        return this.attrCheckSet.contains(attrId);
    }

    public boolean isCiExists(Long ciId) {
        return this.ciCheckSet.contains(ciId);
    }

    public void addAttrToCheckSet(Long attrId) {
        this.attrCheckSet.add(attrId);
    }

    public void addCiToCheckSet(Long ciId) {
        this.ciCheckSet.add(ciId);
    }

    public List<SearchItem> getPrevItemList() {
        return prevItemList;
    }

    public void setPrevItemList(List<SearchItem> prevItemList) {
        this.prevItemList = prevItemList;
    }
}
