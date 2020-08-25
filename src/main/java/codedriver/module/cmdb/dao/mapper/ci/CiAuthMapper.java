package codedriver.module.cmdb.dao.mapper.ci;

import java.util.List;

import codedriver.module.cmdb.dto.ci.CiAuthVo;

/**
 * 
 * @Author:chenqiwei
 * @Time:Aug 15, 2020
 * @ClassName: CiMapper
 * @Description: TODO
 */
public interface CiAuthMapper {
    public List<CiAuthVo> getCiAuthByCiId(Long ciId);

    public int insertCiAuth(CiAuthVo ciAuthVo);

    public int deleteCiAuthByCiId(Long ciId);

}
