/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.startup.handler;

import neatlogic.framework.cmdb.dto.attrexpression.RebuildAuditVo;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.startup.IStartup;
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
