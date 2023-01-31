/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.matrix.constvalue;

import neatlogic.framework.matrix.core.IMatrixType;

/**
 * @author linbq
 * @since 2021/11/16 15:21
 **/
public enum MatrixType implements IMatrixType {
    CMDBCI("cmdbci", "配置项", "ciId", 4);

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
