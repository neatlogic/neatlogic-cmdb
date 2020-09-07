package codedriver.module.cmdb.dao.mapper.ci;

import java.util.List;

import codedriver.module.cmdb.dto.ci.CiViewVo;

/**
 * 
 * @Author:chenqiwei
 * @Time:Aug 15, 2020
 * @ClassName: CiMapper
 */
public interface CiViewMapper {
    public List<CiViewVo> getCiViewByCiId(CiViewVo ciViewVo);

    public int insertCiView(CiViewVo ciViewVo);

    public int deleteCiViewByCiId(Long ciId);
}
