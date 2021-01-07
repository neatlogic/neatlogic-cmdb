package codedriver.module.cmdb.dao.mapper.cientity;

import codedriver.framework.elasticsearch.annotation.ESParam;
import codedriver.framework.elasticsearch.annotation.ESSearch;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CiEntityMapper {


    List<Long> getCiEntityIdByCiId(Long ciId);

    List<CiEntityVo> getCiEntityByAttrId(Long attrId);

    List<Long> getCiEntityIdByGroupIdList(@Param("groupIdList") List<Long> groupIdList,
                                          @Param("ciEntityIdList") List<Long> ciEntityIdList, @Param("typeList") List<String> typeList);

    CiEntityVo getCiEntityById(Long id);

    List<Long> searchCiEntityId(CiEntityVo ciEntityVo);

    int searchCiEntityIdCount(CiEntityVo ciEntityVo);

    List<CiEntityVo> searchCiEntityByIdList(@Param("idList") List<Long> idList);

    Long getIdByCiIdAndName(@Param("ciId") Long ciId, @Param("name") String name);

    @ESSearch
    int insertCiEntity(@ESParam("cientity") CiEntityVo ciEntityVo);

    int updateCiEntityLockById(CiEntityVo ciEntityVo);

    int deleteCiEntityByCiId(Long ciId);

    int deleteCiEntityById(Long ciEntityId);

}
