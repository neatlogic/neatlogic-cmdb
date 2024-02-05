/*
 * Copyright(c) 2024 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.dsl.core;

import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.util.SnowflakeUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 目标链，例如dsl中有属性：linux.netcard.brand.name 则需要分拆成4个SearchItem
 */
public class SearchItem {
    //如果属性或关系是继承过来的，此ciId可能和attrVo或relVo的ciId不一致
    private Long ciId;
    private RelVo relVo;
    private AttrVo attrVo;
    private SearchItem next;
    private SearchItem prev;
    //在sql语句中属性的别名
    private String alias;

    //子查询的别名
    private String tableAlias;

    //如果需要join cmdb_attrentity或cmdb_relentity，则需要这个别名
    private String attrEntityAlias;

    private String relEntityAlias;
    //目标cientity表的alias
    private String targetCiEntityAlias;


    public String getAlias() {
        if (StringUtils.isBlank(alias)) {
            alias = "attr_" + SnowflakeUtil.uniqueLong();
        }
        return alias;
    }

    public String getTableAlias() {
        if (StringUtils.isBlank(tableAlias)) {
            tableAlias = "item_" + SnowflakeUtil.uniqueLong();
        }
        return tableAlias;
    }

    public String getAttrEntityAlias() {
        if (StringUtils.isBlank(attrEntityAlias)) {
            attrEntityAlias = "attrentity_" + SnowflakeUtil.uniqueLong();
        }
        return attrEntityAlias;
    }

    public String getRelEntityAlias() {
        if (StringUtils.isBlank(relEntityAlias)) {
            relEntityAlias = "relentity_" + SnowflakeUtil.uniqueLong();
        }
        return relEntityAlias;
    }

    public String getTargetCiEntityAlias() {
        if (StringUtils.isBlank(targetCiEntityAlias)) {
            targetCiEntityAlias = "targetcientity_" + SnowflakeUtil.uniqueLong();
        }
        return targetCiEntityAlias;
    }

    public Long getCiId() {
        return ciId;
    }

    public void setCiId(Long ciId) {
        this.ciId = ciId;
    }

    public SearchItem(Long ciId, RelVo relVo) {
        this.ciId = ciId;
        this.relVo = relVo;
    }

    public SearchItem(Long ciId, AttrVo attrVo) {
        this.ciId = ciId;
        this.attrVo = attrVo;
    }

    public RelVo getRelVo() {
        return relVo;
    }

    public void setRelVo(RelVo relVo) {
        this.relVo = relVo;
    }

    public AttrVo getAttrVo() {
        return attrVo;
    }

    public void setAttrVo(AttrVo attrVo) {
        this.attrVo = attrVo;
    }

    public SearchItem getNext() {
        return next;
    }

    public void setNext(SearchItem next) {
        this.next = next;
        next.prev = this;
    }

    public SearchItem getPrev() {
        return prev;
    }

    public void setPrev(SearchItem prev) {
        this.prev = prev;
        prev.next = this;
    }

    public List<SearchItem> getPrevItemList() {
        List<SearchItem> prevItemList = new ArrayList<>();
        SearchItem prev = this.getPrev();
        while (prev != null) {
            prevItemList.add(0, prev);
            prev = prev.getPrev();
        }
        return prevItemList;
    }

    //模型路径，用于辨别是否需要重新创建新的子查询
    public String getCiPath() {
        String path = "";
        SearchItem prev = this.prev;
        while (prev != null) {
            if (StringUtils.isNotBlank(path)) {
                path = "." + path;
            }
            if (prev.getAttrVo() != null) {
                path = "attr_" + prev.getAttrVo().getId() + path;
            } else {
                path = "rel_" + prev.getRelVo().getDirection() + "_" + prev.getRelVo().getId() + path;
            }
            prev = prev.getPrev();
        }
        return path;
    }

}
