package codedriver.module.cmdb.dao.mapper.ci;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import codedriver.module.cmdb.dto.ci.RelVo;

public interface RelMapper {
    public int checkRelByFromToName(@Param("fromRelName") String fromRelName, @Param("toRelName") String toRelName);

    public RelVo getRelById(Long id);

    public List<RelVo> getRelByCiId(Long ciId);

    public int updateRel(RelVo relVo);

    public int insertRel(RelVo relVo);

    public int deleteRelById(Long relId);

}
