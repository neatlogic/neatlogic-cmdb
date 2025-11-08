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

package neatlogic.module.cmdb.fulltextindex.enums;

import neatlogic.framework.fulltextindex.core.IFullTextIndexType;
import neatlogic.framework.util.$;

public enum CmdbFullTextIndexType implements IFullTextIndexType {
    CIENTITY("cientity", "nmcfe.cmdbfulltextindextype.cientity");

    private final String type;
    private final String typeName;

    CmdbFullTextIndexType(String _type, String _typeName) {
        type = _type;
        typeName = _typeName;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getTypeName() {
        return $.t(typeName);
    }

    @Override
    public String getTypeName(String type) {
        for (CmdbFullTextIndexType t : values()) {
            if (t.getType().equals(type)) {
                return t.getTypeName();
            }
        }
        return "";
    }

    @Override
    public boolean isActiveGlobalSearch() {
        return true;
    }
}
