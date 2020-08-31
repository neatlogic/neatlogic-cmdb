package codedriver.module.cmdb.dao.mapper.cientity;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import codedriver.module.cmdb.dto.cientity.RelEntityVo;

/**
 * 
 * @Author:chenqiwei
 * @Time:Aug 15, 2020
 */
public interface RelEntityMapper {
    public List<RelEntityVo> searchRelEntityByCiEntityIdList(@Param("idList") List<Long> idList);
}
