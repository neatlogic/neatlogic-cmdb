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
