package codedriver.module.cmdb.dao.mapper.cientity;

import codedriver.framework.elasticsearch.annotation.ESParam;
import codedriver.framework.elasticsearch.annotation.ESSearch;
import codedriver.module.cmdb.dto.cientity.AttrEntityVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: chenqiwei
 * @Time: Aug 15, 2020
 */
public interface AttrEntityMapper {

    public AttrEntityVo getAttrEntityByCiEntityIdAndAttrId(@Param("ciEntityId") Long ciEntityId,
                                                           @Param("attrId") Long attrId);

    public List<AttrEntityVo> getAttrEntityByAttrIdAndValue(AttrEntityVo attrEntityVo);


    public List<AttrEntityVo> getAttrEntityByCiEntityId(Long ciEntityId);

    public List<AttrEntityVo> searchAttrEntityByCiEntityIdList(@Param("idList") List<Long> idList,
                                                               @Param("attrIdList") List<Long> attrIdList);

    @ESSearch
    public int insertAttrEntity(@ESParam("cientity") AttrEntityVo attrEntityVo);

    @ESSearch
    public int deleteAttrEntity(@ESParam("cientity") AttrEntityVo attrEntityVo);

}
