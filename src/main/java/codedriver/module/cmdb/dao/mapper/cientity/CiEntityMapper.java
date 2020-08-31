package codedriver.module.cmdb.dao.mapper.cientity;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import codedriver.module.cmdb.dto.cientity.CiEntityVo;

/**
 * 
 * @Author:chenqiwei
 * @Time:Aug 15, 2020
 */
public interface CiEntityMapper {
    public List<Long> searchCiEntityId(CiEntityVo ciEntityVo);

    public int searchCiEntityIdCount(CiEntityVo ciEntityVo);

    public List<CiEntityVo> searchCiEntityByIdList(@Param("idList") List<Long> idList);
    
    public int insertCiEntity(CiEntityVo ciEntityVo);

}
