package codedriver.module.cmdb.dao.mapper.ci;

import java.util.List;

import codedriver.module.cmdb.dto.ci.RelTypeVo;

public interface RelTypeMapper {
    public int checkRelTypeIsInUsed(Long relTypId);

    public int checkRelTypeNameIsExists(RelTypeVo relTypeVo);

    public RelTypeVo getRelTypeById(Long id);

    public List<RelTypeVo> getAllRelType();

    public int insertRelType(RelTypeVo relTypeVo);

    public int updateRelType(RelTypeVo relTypeVo);

    public int deleteRelTypeById(Long relTypId);
}
