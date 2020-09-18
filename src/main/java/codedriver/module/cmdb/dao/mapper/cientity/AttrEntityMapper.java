package codedriver.module.cmdb.dao.mapper.cientity;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import codedriver.module.cmdb.dto.cientity.AttrEntityVo;

/**
 * 
 * @Author:chenqiwei
 * @Time:Aug 15, 2020
 */
public interface AttrEntityMapper {

    public AttrEntityVo getAttrEntityByCiEntityIdAndAttrId(@Param("ciEntityId") Long ciEntityId,
        @Param("attrId") Long attrId);

    public List<AttrEntityVo> getAttrEntityByAttrIdAndValue(AttrEntityVo attrEntityVo);

    public int checkAttrEntityValueIsExists(AttrEntityVo attrEntityVo);

    public List<AttrEntityVo> getAttrEntityByCiEntityId(Long ciEntityId);

    public List<AttrEntityVo> searchAttrEntityByCiEntityIdList(@Param("idList") List<Long> idList,
        @Param("attrIdList") List<Long> attrIdList);

    public int updateAttrEntity(AttrEntityVo attrEntityVo);

    public int insertAttrEntity(AttrEntityVo attrEntityVo);

    public int deleteAttrEntity(AttrEntityVo attrEntityVo);

}
