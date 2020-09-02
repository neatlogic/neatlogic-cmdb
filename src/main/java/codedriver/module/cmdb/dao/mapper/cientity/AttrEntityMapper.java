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
    public int checkAttrEntityValueIsExists(AttrEntityVo attrEntityVo);

    public List<AttrEntityVo> getAttrEntityByCiEntityId(Long ciEntityId);

    public List<AttrEntityVo> searchAttrEntityByCiEntityIdList(@Param("idList") List<Long> idList);

    public int updateAttrEntity(AttrEntityVo attrEntityVo);

    public int insertAttrEntity(AttrEntityVo attrEntityVo);

    public int deleteAttrEntity(AttrEntityVo attrEntityVo);

}
