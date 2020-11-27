package codedriver.module.cmdb.dao.mapper.ci;

import java.util.List;

import codedriver.module.cmdb.dto.ci.CiTypeVo;

/**
 * 
 * @Author:chenqiwei
 * @Time:Aug 15, 2020
 * @ClassName: CiMapper
 * @Description: TODO
 */
public interface CiTypeMapper {

    public Integer getMaxSort();

    public int checkCiTypeNameIsExists(CiTypeVo ciTypeVo);

    public CiTypeVo getCiTypeById(Long ciTypeId);

    public List<CiTypeVo> searchCiType(CiTypeVo ciTypeVo);

    public int insertCiType(CiTypeVo ciTypeVo);

    public int updateCiType(CiTypeVo ciTypeVo);

    public int deleteCiTypeById(Long ciTypeId);
}
