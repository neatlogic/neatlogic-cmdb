/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.cientity;

import codedriver.framework.cmdb.dto.attrexpression.RebuildAuditVo;

import java.util.List;

public interface AttrExpressionRebuildAuditMapper {
    List<RebuildAuditVo> getAttrExpressionRebuildAuditByServerId(Integer serverId);

    void updateAttrExpressionRebuildAuditCiEntityIdStartById(RebuildAuditVo rebuildAuditVo);

    void insertAttrExpressionRebuildAudit(RebuildAuditVo rebuildAuditVo);

    void deleteAttrExpressionRebuildAuditById(Long id);
}
