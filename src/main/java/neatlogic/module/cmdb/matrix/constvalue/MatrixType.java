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

package neatlogic.module.cmdb.matrix.constvalue;

import neatlogic.framework.matrix.core.IMatrixType;
import neatlogic.framework.util.$;

/**
 * @author linbq
 * @since 2021/11/16 15:21
 **/
public enum MatrixType implements IMatrixType {
//    CMDBCI("cmdbci", "nmcfe.cmdbfulltextindextype.cientity", "ciId", 4),
//    CMDBCUSTOMVIEW("cmdbcustomview", "nmcmc.matrixtype.cmdbcustomview", "customViewId", 5)
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
