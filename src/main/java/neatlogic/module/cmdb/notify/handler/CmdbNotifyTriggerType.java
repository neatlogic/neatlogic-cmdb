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

public enum CmdbNotifyTriggerType implements INotifyTriggerType {

    CIMODITY("cimodity", "模型修改", "模型基础配置、属性、关系和授权相关发生变化时触发通知"),
    CIDELETE("cidelete", "模型删除", "模型被删除时触发通知"),
    CIENTITYMODITY("cientitymodify", "配置项修改", "配置项发生变化并且生效时触发通知"),
    CIENTITYDELETE("cientitydelete", "配置项删除", "配置项删除并生效时触发通知"),
    CIENTITYINSERT("cientityinsert", "配置项添加", "配置项添加并且生效时触发通知"),
    CIENTITYINVALID("cientityinvalid", "配置项不合规", "发现配置项不合规时发送通知");

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
        return text;
    }

    @Override
    public String getDescription() {
        return description;
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
