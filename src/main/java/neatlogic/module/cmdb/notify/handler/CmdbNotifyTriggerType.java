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

package neatlogic.module.cmdb.notify.handler;

import neatlogic.framework.notify.core.INotifyTriggerType;
import neatlogic.framework.util.I18n;

public enum CmdbNotifyTriggerType implements INotifyTriggerType {

    CIMODITY("cimodity", new I18n("模型修改"), new I18n("模型基础配置、属性、关系和授权相关发生变化时触发通知")),
    CIDELETE("cidelete", new I18n("模型删除"), new I18n("模型被删除时触发通知")),
    CIENTITYMODITY("cientitymodify", new I18n("配置项修改"), new I18n("配置项发生变化并且生效时触发通知")),
    CIENTITYDELETE("cientitydelete", new I18n("配置项删除"), new I18n("配置项删除并生效时触发通知")),
    CIENTITYINSERT("cientityinsert", new I18n("配置项添加"), new I18n("配置项添加并且生效时触发通知"));

    private final String trigger;
    private final I18n text;
    private final I18n description;

    CmdbNotifyTriggerType(String _trigger, I18n _text, I18n _description) {
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
        return text.toString();
    }

    @Override
    public String getDescription() {
        return description.toString();
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
