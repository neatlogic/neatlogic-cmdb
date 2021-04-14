/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.ci;

import java.util.List;

import codedriver.framework.cmdb.dto.ci.RelTypeVo;

public interface RelTypeMapper {
    public int checkRelTypeIsInUsed(Long relTypId);

    public int checkRelTypeNameIsExists(RelTypeVo relTypeVo);

    public RelTypeVo getRelTypeById(Long id);

    public List<RelTypeVo> getAllRelType();

    public int insertRelType(RelTypeVo relTypeVo);

    public int updateRelType(RelTypeVo relTypeVo);

    public int deleteRelTypeById(Long relTypId);
}
