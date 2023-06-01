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
