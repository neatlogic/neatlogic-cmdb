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

package neatlogic.module.cmdb.utils;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.enums.InputType;
import neatlogic.framework.cmdb.exception.ci.*;
import neatlogic.framework.exception.core.ApiRuntimeException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.*;

public class VirtualCiSqlBuilder {

    private final String sql;
    private Long ciId;
    private final Map<String, String> attrMap = new HashMap<>();
    private Map<String, Long> attrIdMap;
    private final String dataSchema = TenantContext.get().getDataDbName();
    private final String schema = TenantContext.get().getDbName();

    public VirtualCiSqlBuilder(String xml) {
        try {
            Document document = DocumentHelper.parseText(xml);
            Element root = document.getRootElement();
            Element attrsElement = root.element("attrs");
            if (attrsElement == null) {
                throw new CiViewSettingFileIrregularException("attrs");
            }
            Element sqlElement = root.element("sql");
            if (sqlElement == null) {
                throw new CiViewSettingFileIrregularException("sql");
            }
            List<Element> attrElementList = attrsElement.elements("attr");
            if (CollectionUtils.isNotEmpty(attrElementList)) {
                for (Element attrE : attrElementList) {
                    if (StringUtils.isBlank(attrE.attributeValue("name"))) {
                        throw new CiViewSettingFileIrregularException("attrs->attr", "name");
                    }
                    if (StringUtils.isBlank(attrE.attributeValue("label"))) {
                        throw new CiViewSettingFileIrregularException("attrs->attr", "label");
                    }
                    attrMap.put(attrE.attributeValue("name"), attrE.attributeValue("label"));
                }
            } else {
                throw new CiViewSettingFileIrregularException("attrs->attr");
            }
            sql = sqlElement.getTextTrim();
        } catch (Exception ex) {
            throw new CiViewSettingFileIrregularException(ex);
        }
    }

    public void setAttrIdMap(Map<String, Long> _attrIdMap) {
        this.attrIdMap = _attrIdMap;
    }

    public void setCiId(Long ciId) {
        this.ciId = ciId;
    }


    public boolean valid() {
        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            Select selectStatement = (Select) stmt;
            PlainSelect select = (PlainSelect) selectStatement.getSelectBody();
            final boolean[] hasId = new boolean[1];
            final boolean[] hasName = new boolean[1];
            Set<String> columnList = new HashSet<>();
            for (SelectItem selectItem : select.getSelectItems()) {
                selectItem.accept(new SelectItemVisitor() {
                    @Override
                    public void visit(AllColumns allColumns) {
                    }

                    @Override
                    public void visit(AllTableColumns allTableColumns) {
                    }

                    @Override
                    public void visit(SelectExpressionItem selectExpressionItem) {
                        String columnName;
                        if (selectExpressionItem.getAlias() != null) {
                            columnName = selectExpressionItem.getAlias().getName();
                        } else {
                            columnName = selectExpressionItem.toString();
                        }
                        if (columnName.equalsIgnoreCase("id")) {
                            hasId[0] = true;
                        } else if (columnName.equalsIgnoreCase("name")) {
                            hasName[0] = true;
                        } else {
                            columnList.add(columnName);
                        }
                    }
                });
            }
            if (!hasId[0]) {
                throw new CiViewSqlFieldNotExistsException("id");
            }
            if (!hasName[0]) {
                throw new CiViewSqlFieldNotExistsException("name");
            }
            if (CollectionUtils.isNotEmpty(columnList)) {
                for (String attrName : attrMap.keySet()) {
                    if (!columnList.contains(attrName)) {
                        throw new CiViewSqlFieldNotExistsException(attrName);
                    }
                }
            } else {
                throw new CiViewHasNoAttrException();
            }
        } catch (ApiRuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CiViewSqlIrregularException(ex);
        }
        return true;
    }

    /**
     * 获取SQL测试语句
     *
     * @return 获取测试SQL语句，只会返回1行数据，用于检查表和字段名是否有异常
     */
    public String getTestSql() {
        try {
            this.valid();
            Statement stmt = CCJSqlParserUtil.parse(sql);
            Select selectStatement = (Select) stmt;
            PlainSelect select = (PlainSelect) selectStatement.getSelectBody();
            Limit limit = new Limit();
            limit.setRowCount(new LongValue(1L));
            select.setLimit(limit);
            return stmt.toString();
        } catch (ApiRuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CiViewSqlIrregularException(ex);
        }
    }

    /**
     * 获取所有列
     *
     * @return 列名列表
     */
    public List<AttrVo> getAttrList() {
        List<AttrVo> attrList = new ArrayList<>();
        for (String attrName : attrMap.keySet()) {
            AttrVo attrVo = new AttrVo();
            attrVo.setType("text");
            attrVo.setInputType(InputType.MT.getValue());
            attrVo.setName(attrName);
            attrVo.setLabel(attrMap.get(attrName));
            attrList.add(attrVo);
        }
        return attrList;
    }

    /**
     * 获取创建视图语句
     *
     * @return 创建视图语句
     */
    public String getSql() {
        try {
            if (MapUtils.isEmpty(attrIdMap)) {
                throw new CiViewAttrIdMapEmptyException();
            }
            if (ciId == null) {
                throw new CiViewCiIdEmptyException();
            }
            Statement stmt = CCJSqlParserUtil.parse(sql);
            Select selectStatement = (Select) stmt;
            fillUpSchema(selectStatement.getSelectBody());
            fillUpAlias(selectStatement.getSelectBody());
            return stmt.toString();

        } catch (ApiRuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CiViewSqlIrregularException(ex);
        }
    }

    /**
     * 补充列别名
     *
     * @param selectBody select主体
     */
    private void fillUpAlias(SelectBody selectBody) {
        PlainSelect select = (PlainSelect) selectBody;
        List<SelectItem> newItemList = new ArrayList<>();
        for (SelectItem selectItem : select.getSelectItems()) {
            selectItem.accept(new SelectItemVisitor() {
                @Override
                public void visit(AllColumns allColumns) {
                }

                @Override
                public void visit(AllTableColumns allTableColumns) {
                }

                @Override
                public void visit(SelectExpressionItem selectExpressionItem) {
                    //替换列名的别名为attrId
                    if (MapUtils.isNotEmpty(attrIdMap)) {
                        //原来已经有别名
                        String alias;
                        SelectExpressionItem selectItem = new SelectExpressionItem();
                        if (selectExpressionItem.getAlias() != null) {
                            selectItem.setExpression(new Column("md5(LOWER(" + selectExpressionItem.getExpression() + "))"));
                            alias = selectExpressionItem.getAlias().getName();
                        } else {
                            selectItem.setExpression(new Column("md5(LOWER(" + selectExpressionItem + "))"));
                            alias = selectExpressionItem.toString();
                        }
                        if (attrIdMap.containsKey(alias)) {
                            selectItem.setAlias(new Alias("`" + attrIdMap.get(alias) + "_hash`"));
                            selectExpressionItem.setAlias(new Alias("`" + attrIdMap.get(alias) + "`"));
                            newItemList.add(selectItem);
                        }
                    }
                }
            });
        }
        //补充ci列
        SelectExpressionItem selectItem = new SelectExpressionItem();
        selectItem.setExpression(new StringValue(ciId.toString()));
        selectItem.setAlias(new Alias("ci_id"));
        newItemList.add(selectItem);
        if (CollectionUtils.isNotEmpty(newItemList)) {
            select.addSelectItems(newItemList);
        }

    }

    /**
     * 给SQL语句补充视图schema
     *
     * @param selectBody select主体
     */
    private void fillUpSchema(SelectBody selectBody) {
        PlainSelect select = (PlainSelect) selectBody;
        //处理from
        FromItem fromItem = select.getFromItem();
        if (fromItem instanceof Table) {
            Table table = (Table) select.getFromItem();
            table.setSchemaName(schema);
        } else if (fromItem instanceof SubSelect) {
            SubSelect subselect = (SubSelect) select.getFromItem();
            fillUpSchema(subselect.getSelectBody());
        }

        //处理join
        if (CollectionUtils.isNotEmpty(select.getJoins())) {
            for (Join j : select.getJoins()) {
                if (j.getRightItem() instanceof Table) {
                    Table t = (Table) j.getRightItem();
                    t.setSchemaName(schema);
                } else if (j.getRightItem() instanceof SubSelect) {
                    SubSelect subselect = (SubSelect) j.getRightItem();
                    fillUpSchema(subselect.getSelectBody());
                }
            }
        }
    }

    public static void main(String[] a) throws DocumentException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<ci id=\"user\">\n" +
                "        <attrs>\n" +
                "            <attr name=\"user_id\" label=\"用户id\"></attr>\n" +
                "            <attr name=\"user_name\" label=\"用户名\"></attr>\n" +
                "            <attr name=\"email\" label=\"邮件\"></attr>\n" +
                "            <attr name=\"phone\" label=\"电话\"></attr>\n" +
                "            <attr name=\"pinyin\" label=\"拼音\"></attr>\n" +
                "        </attrs>\n" +
                "        <sql>\n" +
                "           INSERT INTO test (a) VALUES ('a')" +
                "        </sql>\n" +
                "</ci>\n" +
                "\n";
        VirtualCiSqlBuilder viewBuilder = new VirtualCiSqlBuilder(xml);
        viewBuilder.valid();
    }

}
