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

package neatlogic.module.cmdb.constvalue;

import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.util.$;

public enum JobSource implements IAutoexecJobSource {
    DISCOVERY("自动发现", "discovery");
    private final String text;
    private final String value;

    JobSource(String _text, String _value) {
        this.text = _text;
        this.value = _value;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return $.t(text);
    }

    public static String getText(String _status) {
        for (JobSource s : JobSource.values()) {
            if (s.getValue().equals(_status)) {
                return s.getText();
            }
        }
        return "";
    }

}
