package codedriver.module.cmdb.dao.mapper.ci;

import java.util.List;

import codedriver.module.cmdb.dto.ci.RelGroupVo;
import codedriver.module.cmdb.dto.ci.RelVo;

public interface RelMapper {
    public List<RelGroupVo> getRelGroupByCiId(Long ciId);

    public int checkRelGroupNameIsExists(RelGroupVo relGroupVo);

    public int checkRelByFromToName(RelVo relVo);

    public int checkRelByFromToLabel(RelVo relVo);

    public RelVo getRelById(Long id);

    public RelGroupVo getRelGroupById(Long relGroupId);

    public List<RelVo> getRelByCiId(Long ciId);

    public int updateRel(RelVo relVo);

    public int updateRelGroup(RelGroupVo relGroupVo);

    public int insertRel(RelVo relVo);

    public int insertRelGroup(RelGroupVo relGroupVo);

    public int deleteRelById(Long relId);

}
