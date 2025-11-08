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

package neatlogic.module.cmdb.startup.handler;

import neatlogic.framework.cmdb.dto.attrexpression.RebuildAuditVo;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.startup.StartupBase;
import neatlogic.module.cmdb.attrexpression.AttrExpressionRebuildManager;
import neatlogic.module.cmdb.dao.mapper.cientity.AttrExpressionRebuildAuditMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UpdateExpressionAttrStartupHandler extends StartupBase {
    @Resource
    private AttrExpressionRebuildAuditMapper attrExpressionRebuildAuditMapper;

    @Override
    public String getName() {
        return "更新配置项表达式属性";
    }

    @Override
    public int sort() {
        return 0;
    }

    @Override
    public int executeForCurrentTenant() {
        List<RebuildAuditVo> auditList = attrExpressionRebuildAuditMapper.getAttrExpressionRebuildAuditByServerId(Config.SCHEDULE_SERVER_ID);
        if (CollectionUtils.isNotEmpty(auditList)) {
            for (RebuildAuditVo audit : auditList) {
                AttrExpressionRebuildManager.rebuild(audit);
            }
        }
        return 0;
    }

}
