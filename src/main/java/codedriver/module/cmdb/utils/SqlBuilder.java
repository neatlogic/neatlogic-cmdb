/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.utils;

import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.dto.cientity.AttrFilterVo;
import codedriver.module.cmdb.dto.cientity.RelFilterVo;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class SqlBuilder {
    private List<CiVo> ciList;
    private List<AttrVo> attrList;
    private List<AttrFilterVo> attrFilterList;
    private List<RelFilterVo> relFilterList;

    private SqlBuilder(Builder builder) {
        this.ciList = builder.ciList;
        this.attrList = builder.attrList;
        this.attrFilterList = builder.attrFilterList;
        this.relFilterList = builder.relFilterList;
    }

    /**
     * 获取分页SQL语句
     *
     * @return SQL语句
     */
    public String getPageSql() {
        if (CollectionUtils.isNotEmpty(ciList) && CollectionUtils.isNotEmpty(attrList)) {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT\n");
            sb.append("DISTINCT `ci_base`.`id` AS `id`\n");
            sb.append("FROM\n");
            sb.append("cmdb_cientity `ci_base`\n");
            for (CiVo ciVo : ciList) {
                sb.append("JOIN ").append(ciVo.getCiTableName()).append(" ON `ci_base`.id = ").append(ciVo.getCiTableName()).append(".cientity_id\n");
            }
            return sb.toString();
        }
        return null;
    }

    public String getSql() {
        if (CollectionUtils.isNotEmpty(ciList) && CollectionUtils.isNotEmpty(attrList)) {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT\n");
            for (AttrVo attrVo : attrList) {
                if (!attrVo.isNeedTargetCi()) {
                    sb.append(attrVo.getCiTableName()).append(".`").append(attrVo.getName()).append("` AS `").append(attrVo.getId()).append("`,\n");
                }
            }
            sb.append("`ci_base`.`id` AS `id`,\n");
            sb.append("`ci_base`.`name` AS `name`,\n");
            sb.append("`ci_base`.`status` AS `status`,\n");
            sb.append("`ci_base`.`fcd` AS `fcd`,\n");
            sb.append("`ci_base`.`fcu` AS `fcu`,\n");
            sb.append("`ci_base`.`lcd` AS `lcd`,\n");
            sb.append("`ci_base`.lcu AS `lcu`\n");
            sb.append("FROM\n");
            sb.append("cmdb_cientity `ci_base`\n");
            for (CiVo ciVo : ciList) {
                sb.append("JOIN ").append(ciVo.getCiTableName()).append(" ON `ci_base`.id = ").append(ciVo.getCiTableName()).append(".cientity_id\n");
            }
            return sb.toString();
        }
        return null;
    }

    public static class Builder {
        private List<CiVo> ciList;
        private List<AttrVo> attrList;
        private List<AttrFilterVo> attrFilterList;
        private List<RelFilterVo> relFilterList;

        public Builder(List<CiVo> _ciList, List<AttrVo> _attrList) {
            ciList = _ciList;
            attrList = _attrList;
        }


        public Builder withAttrFilter(List<AttrFilterVo> _attrFilterList) {
            attrFilterList = _attrFilterList;
            return this;
        }

        public Builder withRelFilter(List<RelFilterVo> _relFilterList) {
            relFilterList = _relFilterList;
            return this;
        }

        public SqlBuilder build() {
            return new SqlBuilder(this);
        }
    }
}
