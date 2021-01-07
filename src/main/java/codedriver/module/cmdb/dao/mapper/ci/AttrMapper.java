package codedriver.module.cmdb.dao.mapper.ci;

import codedriver.module.cmdb.dto.ci.AttrVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AttrMapper {

    List<String> getAttrGroupByCiId(Long ciId);

    AttrVo getAttrById(Long attrId);

    List<AttrVo> getAttrByCiId(Long ciId);

    List<AttrVo> getAttrByCiIdList(@Param("ciIdList") List<Long> ciIdList);


    int checkAttrNameIsRepeat(AttrVo attrVo);

    int updateAttr(AttrVo attrVo);

    int insertAttr(AttrVo attrVo);

    int deleteAttrById(Long attrId);

    int deleteAttrByCiId(Long ciId);
}
