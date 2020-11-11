package codedriver.module.cmdb.dao.mapper.cientity;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import codedriver.framework.elasticsearch.annotation.ESParam;
import codedriver.framework.elasticsearch.annotation.ESSearch;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;

/**
 * 
 * @Author:chenqiwei
 * @Time:Aug 15, 2020
 */
public interface CiEntityMapper {
    public List<Long> getCiEntityIdByCiId(Long ciId);

    public List<Long> getCiEntityIdByGroupIdList(@Param("groupIdList") List<Long> groupIdList,
        @Param("ciEntityIdList") List<Long> ciEntityIdList, @Param("typeList") List<String> typeList);

    public CiEntityVo getCiEntityById(Long id);

    public List<Long> searchCiEntityId(CiEntityVo ciEntityVo);

    public int searchCiEntityIdCount(CiEntityVo ciEntityVo);

    public List<CiEntityVo> searchCiEntityByIdList(@Param("idList") List<Long> idList);

    @ESSearch
    public int insertCiEntity(@ESParam("cientity") CiEntityVo ciEntityVo);

    public int updateCiEntityLockById(CiEntityVo ciEntityVo);

    public int deleteCiEntityByCiId(Long ciId);

    public int deleteCiEntityById(Long ciEntityId);

}
