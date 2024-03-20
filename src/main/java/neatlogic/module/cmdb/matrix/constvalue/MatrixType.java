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

package neatlogic.module.cmdb.matrix.constvalue;

import neatlogic.framework.matrix.core.IMatrixType;
import neatlogic.framework.util.$;

/**
 * @author linbq
 * @since 2021/11/16 15:21
 **/
public enum MatrixType implements IMatrixType {
    CMDBCI("cmdbci", "配置项", "ciId", 4),
    CMDBCUSTOMVIEW("cmdbcustomview", "自定义视图", "customViewId", 5)
    ;

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
        return $.t(name);
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
