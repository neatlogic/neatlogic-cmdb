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
    public void executeForCurrentTenant() {
        List<RebuildAuditVo> auditList = attrExpressionRebuildAuditMapper.getAttrExpressionRebuildAuditByServerId(Config.SCHEDULE_SERVER_ID);
        if (CollectionUtils.isNotEmpty(auditList)) {
            for (RebuildAuditVo audit : auditList) {
                AttrExpressionRebuildManager.rebuild(audit);
            }
        }
    }

    @Override
    public void executeForAllTenant() {

    }
}
