/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.dao.mapper.ci;

import neatlogic.framework.cmdb.dto.ci.RelTypeVo;

import java.util.List;

public interface RelTypeMapper {
    int checkRelTypeIsInUsed(Long relTypId);

    int checkRelTypeNameIsExists(RelTypeVo relTypeVo);

    RelTypeVo getRelTypeById(Long id);

    List<RelTypeVo> getAllRelType();

    int insertRelType(RelTypeVo relTypeVo);

    int updateRelType(RelTypeVo relTypeVo);

    int updateRelTypeIsShow(RelTypeVo relTypeVo);

    int deleteRelTypeById(Long relTypId);
}
