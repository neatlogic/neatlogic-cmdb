package codedriver.module.cmdb.dao.mapper.ci;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import codedriver.module.cmdb.dto.ci.AttrVo;

/**
 * @Author:chenqiwei
 * @Time:Aug 15, 2020
 * @ClassName: AttrMapper
 */
public interface AttrMapper {

    public List<String> getAttrGroupByCiId(Long ciId);

    public AttrVo getAttrById(Long attrId);

    public List<AttrVo> getAttrByCiId(Long ciId);

    public List<AttrVo> getAttrByCiIdList(@Param("ciIdList") List<Long> ciIdList);

    public int updateAttr(AttrVo attrVo);

    public int insertAttr(AttrVo attrVo);

    public int deleteAttrById(Long attrId);

    public int deleteAttrByCiId(Long ciId);
}
