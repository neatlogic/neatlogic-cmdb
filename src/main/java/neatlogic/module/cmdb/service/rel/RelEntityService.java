/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.service.rel;

import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionGroupVo;

import java.util.List;

public interface RelEntityService {
    List<RelEntityVo> getRelEntityByCiEntityId(Long ciEntityId);

    void deleteRelEntity(TransactionGroupVo transactionGroupVo, List<RelEntityVo> relEntityList);
}
