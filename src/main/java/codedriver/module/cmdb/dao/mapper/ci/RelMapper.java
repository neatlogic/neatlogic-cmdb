/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.ci;

import codedriver.framework.cmdb.dto.ci.RelGroupVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RelMapper {
    RelVo getRelBaseInfoById(Long relId);

    List<RelVo> getAllRelList();

    List<RelGroupVo> getRelGroupByCiId(Long ciId);

    List<RelVo> getRelByCiIdList(@Param("ciIdList") List<Long> ciIdList);

    int checkRelGroupNameIsExists(RelGroupVo relGroupVo);

    int checkRelByFromToName(RelVo relVo);

    int checkRelByFromToLabel(RelVo relVo);

    RelVo getRelById(Long id);

    RelGroupVo getRelGroupById(Long relGroupId);

    List<RelVo> getRelByCiId(Long ciId);

    int updateRel(RelVo relVo);

    int updateRelGroup(RelGroupVo relGroupVo);

    int insertRel(RelVo relVo);

    int insertRelGroup(RelGroupVo relGroupVo);

    int deleteRelById(Long relId);

}
