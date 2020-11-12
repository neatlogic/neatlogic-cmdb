package codedriver.module.cmdb.dao.mapper.cientity;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import codedriver.framework.elasticsearch.annotation.ESParam;
import codedriver.framework.elasticsearch.annotation.ESSearch;
import codedriver.module.cmdb.dto.cientity.RelEntityVo;

/**
 * 
 * @Author:chenqiwei
 * @Time:Aug 15, 2020
 */
public interface RelEntityMapper {
    public int checkRelEntityIsExists(RelEntityVo relEntityVo);

    public List<RelEntityVo> getRelEntityByCiEntityId(Long ciEntityId);

    public List<RelEntityVo> searchRelEntityByCiEntityIdList(@Param("idList") List<Long> idList,
        @Param("relIdList") List<Long> relIdList);

    public List<RelEntityVo> getRelEntityByFromCiEntityIdAndRelId(@Param("fromCiEntityId") Long fromCiEntityId,
        @Param("relId") Long relId);

    public List<RelEntityVo> getRelEntityByToCiEntityIdAndRelId(@Param("toCiEntityId") Long toCiEntityId,
        @Param("relId") Long relId);

    public List<Long> getFromToCiEntityIdByCiEntityIdList(@Param("idList") List<Long> idList);

    public List<Long> getFromToCiEntityIdByCiEntityId(Long ciEntityId);

    @ESSearch
    public int insertRelEntity(@ESParam("cientity") RelEntityVo relEntityVo);

    @ESSearch
    public int deleteRelEntityByFromCiEntityIdAndRelId(
        @Param("fromCiEntityId") @ESParam("cientity") Long fromCiEntityId, @Param("relId") Long relId);

    @ESSearch
    public int deleteRelEntityByToCiEntityIdAndRelId(@Param("toCiEntityId") @ESParam("cientity") Long toCiEntityId,
        @Param("relId") Long relId);

    @ESSearch
    public int deleteRelEntityByRelIdFromCiEntityIdToCiEntityId(@Param("relId") Long relId,
        @Param("fromCiEntityId") @ESParam("cientity") Long fromCiEntityId,
        @Param("toCiEntityId") @ESParam("cientity") Long toCiEntityId);
}
