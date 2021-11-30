/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.matrix.constvalue;

import codedriver.framework.matrix.core.IMatrixType;

/**
 * @author linbq
 * @since 2021/11/16 15:21
 **/
public enum MatrixType implements IMatrixType {
    CMDBCI("cmdbci", "cmdb模型", "ciId", 4),
    CMDBCICUSTOMVIEW("cmdbcustomview", "cmdb自定义视图", "customViewId", 5);

    private String value;
    private String name;
    private String key;
    private int sort;
    MatrixType(String _value, String _name, String _key, int _sort) {
        this.value = _value;
        this.name = _name;
        this.key = _key;
        this.sort = _sort;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public int getSort() {
        return sort;
    }
}
