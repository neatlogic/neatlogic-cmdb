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
