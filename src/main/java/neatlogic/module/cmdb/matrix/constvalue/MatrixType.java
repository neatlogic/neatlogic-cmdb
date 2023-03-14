/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
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

package neatlogic.module.cmdb.matrix.constvalue;

import neatlogic.framework.matrix.core.IMatrixType;
import neatlogic.framework.util.I18nUtils;

/**
 * @author linbq
 * @since 2021/11/16 15:21
 **/
public enum MatrixType implements IMatrixType {
    CMDBCI("cmdbci", "enum.cmdb.matrixtype.cmdbci", "ciId", 4);

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
        return I18nUtils.getMessage(name);
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
