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

package neatlogic.module.cmdb.notify.handler;

import neatlogic.framework.notify.core.INotifyTriggerType;
import neatlogic.framework.util.I18nUtils;

public enum CmdbNotifyTriggerType implements INotifyTriggerType {

    CIMODITY("cimodity", "enum.cmdb.cmdbnotifytriggertype.cimodity", "enum.cmdb.cmdbnotifytriggertype.cimodity.1"),
    CIDELETE("cidelete", "enum.cmdb.cmdbnotifytriggertype.cidelete", "enum.cmdb.cmdbnotifytriggertype.cidelete.1"),
    CIENTITYMODITY("cientitymodify", "enum.cmdb.cmdbnotifytriggertype.cientitymodity", "enum.cmdb.cmdbnotifytriggertype.cientitymodity.1"),
    CIENTITYDELETE("cientitydelete", "enum.cmdb.cmdbnotifytriggertype.cientitydelete", "enum.cmdb.cmdbnotifytriggertype.cientitydelete.1"),
    CIENTITYINSERT("cientityinsert", "enum.cmdb.cmdbnotifytriggertype.cientityinsert", "enum.cmdb.cmdbnotifytriggertype.cientityinsert.1");

    private final String trigger;
    private final String text;
    private final String description;

    CmdbNotifyTriggerType(String _trigger, String _text, String _description) {
        this.trigger = _trigger;
        this.text = _text;
        this.description = _description;
    }

    @Override
    public String getTrigger() {
        return trigger;
    }

    @Override
    public String getText() {
        return I18nUtils.getMessage(text);
    }

    @Override
    public String getDescription() {
        return I18nUtils.getMessage(description);
    }

    public static String getText(String trigger) {
        for (CmdbNotifyTriggerType n : values()) {
            if (n.getTrigger().equals(trigger)) {
                return n.getText();
            }
        }
        return "";
    }
}
