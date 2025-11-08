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

package neatlogic.module.cmdb.process.stephandler;

import neatlogic.framework.process.stephandler.core.IProcessStepHandlerType;
import neatlogic.framework.util.$;

public enum CmdbProcessStepHandlerType implements IProcessStepHandlerType {
    CIENTITYSYNC("cientitysync", "process", "nmcps.cmdbprocessstephandlertype.cientitysync"),
    CMDBSYNC("cmdbsync", "process", "nmcps.cmdbprocessstephandlertype.cmdbsync");

    private final String handler;
    private final String name;
    private final String type;

    CmdbProcessStepHandlerType(String _handler, String _type, String _name) {
        this.handler = _handler;
        this.type = _type;
        this.name = _name;
    }

    public String getHandler() {
        return handler;
    }

    public String getName() {
        return $.t(name);
    }

    public String getType() {
        return type;
    }

}
