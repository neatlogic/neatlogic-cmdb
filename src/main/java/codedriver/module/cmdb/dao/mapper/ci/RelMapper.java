/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.ci;

import codedriver.framework.cmdb.crossover.RelCrossoverMapper;
import codedriver.framework.cmdb.dto.ci.RelGroupVo;
import codedriver.framework.cmdb.dto.ci.RelTypeVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.ci.RelativeRelVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RelMapper extends RelCrossoverMapper {
    RelTypeVo getRelTypeByRelId(Long relId);

    List<RelativeRelVo> getRelativeRelByRelId(Long relId);

    List<RelVo> getAllRelList();

    List<RelGroupVo> getRelGroupByCiId(Long ciId);

    int checkRelGroupNameIsExists(RelGroupVo relGroupVo);

    int checkRelByFromToName(RelVo relVo);

    int checkRelByFromToLabel(RelVo relVo);

    RelVo getRelById(Long id);

    List<RelVo> getRelByIdList(@Param("relIdList") List<Long> relIdList);

    RelGroupVo getRelGroupById(Long relGroupId);

    List<RelVo> getRelByCiId(Long ciId);

    int updateRel(RelVo relVo);

    int updateRelGroup(RelGroupVo relGroupVo);

    int insertRel(RelVo relVo);

    int insertRelGroup(RelGroupVo relGroupVo);

    void insertRelativeRel(RelativeRelVo relativeRelVo);

    int deleteRelById(Long relId);

    void deleteRelativeRelByRelId(Long relId);

}
