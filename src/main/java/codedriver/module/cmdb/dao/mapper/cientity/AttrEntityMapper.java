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
    public List<AttrEntityVo> searchAttrEntityByCiEntityIdList(@Param("idList") List<Long> idList);
}
