/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.startup;

import codedriver.framework.cmdb.dto.attrexpression.RebuildAuditVo;
import codedriver.framework.common.config.Config;
import codedriver.framework.startup.IStartup;
import codedriver.module.cmdb.attrexpression.AttrExpressionRebuildManager;
import codedriver.module.cmdb.dao.mapper.cientity.AttrExpressionRebuildAuditMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UpdateExpressionAttrStartup implements IStartup {
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
    public void execute() {
        List<RebuildAuditVo> auditList = attrExpressionRebuildAuditMapper.getAttrExpressionRebuildAuditByServerId(Config.SCHEDULE_SERVER_ID);
        if (CollectionUtils.isNotEmpty(auditList)) {
            for (RebuildAuditVo audit : auditList) {
                AttrExpressionRebuildManager.rebuild(audit);
            }
        }
    }
}
