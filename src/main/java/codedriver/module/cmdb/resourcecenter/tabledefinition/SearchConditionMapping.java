/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.resourcecenter.tabledefinition;

import codedriver.framework.cmdb.enums.resourcecenter.JoinType;

public class SearchConditionMapping {
    private String tableName;
//    private TableType tableType = TableType.VIEW;
    private String columnName;
    private boolean left = true;
    private String fromTableAlias;
    private Long fromTableCiId;
    private String joinTableAlias;
    private Long joinTableId;
    private Integer joinTableCiIsVirtual;
    private Long fromTableAttrId;
    private String fromTableAttrName;
    private Long fromTableAttrCiId;
    private String fromTableAttrCiName;
    private Integer fromTableAttrCiIsVirtual;
    private JoinType joinType;
    private String direction;

    public SearchConditionMapping(String tableName, String columnName) {
        this.tableName = tableName;
        this.columnName = columnName;
    }
    public SearchConditionMapping(String tableName, String columnName, boolean left) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.left = left;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

//    public TableType getTableType() {
//        return tableType;
//    }
//
//    public void setTableType(TableType tableType) {
//        this.tableType = tableType;
//    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Long getFromTableAttrId() {
        return fromTableAttrId;
    }

    public void setFromTableAttrId(Long fromTableAttrId) {
        this.fromTableAttrId = fromTableAttrId;
    }

    public String getFromTableAttrName() {
        return fromTableAttrName;
    }

    public void setFromTableAttrName(String fromTableAttrName) {
        this.fromTableAttrName = fromTableAttrName;
    }

    public String getJoinTableAlias() {
        return joinTableAlias;
    }

    public void setJoinTableAlias(String joinTableAlias) {
        this.joinTableAlias = joinTableAlias;
    }

    public Long getJoinTableId() {
        return joinTableId;
    }

    public void setJoinTableId(Long joinTableId) {
        this.joinTableId = joinTableId;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    public void setJoinType(JoinType joinType) {
        this.joinType = joinType;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getFromTableAlias() {
        return fromTableAlias;
    }

    public void setFromTableAlias(String fromTableAlias) {
        this.fromTableAlias = fromTableAlias;
    }

    public Long getFromTableCiId() {
        return fromTableCiId;
    }

    public void setFromTableCiId(Long fromTableCiId) {
        this.fromTableCiId = fromTableCiId;
    }

    public Long getFromTableAttrCiId() {
        return fromTableAttrCiId;
    }

    public void setFromTableAttrCiId(Long fromTableAttrCiId) {
        this.fromTableAttrCiId = fromTableAttrCiId;
    }

    public String getFromTableAttrCiName() {
        return fromTableAttrCiName;
    }

    public void setFromTableAttrCiName(String fromTableAttrCiName) {
        this.fromTableAttrCiName = fromTableAttrCiName;
    }

    public Integer getFromTableAttrCiIsVirtual() {
        return fromTableAttrCiIsVirtual;
    }

    public void setFromTableAttrCiIsVirtual(Integer fromTableAttrCiIsVirtual) {
        this.fromTableAttrCiIsVirtual = fromTableAttrCiIsVirtual;
    }

    public Integer getJoinTableCiIsVirtual() {
        return joinTableCiIsVirtual;
    }

    public void setJoinTableCiIsVirtual(Integer joinTableCiIsVirtual) {
        this.joinTableCiIsVirtual = joinTableCiIsVirtual;
    }

    public boolean getLeft() {
        return left;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }
}
